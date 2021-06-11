package functions;

import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.privacy.dlp.v2.*;
import jdk.jshell.execution.Util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Inspector implements HttpFunction {

    private static final Logger logger = Logger.getLogger(Inspector.class.getName());
    private static final Gson gson = new Gson();

    @Override
    public void service(HttpRequest request, HttpResponse response)
            throws IOException {

        var writer = new PrintWriter(response.getWriter());
        String resultMessage = "";

        FunctionOptions options = parseArgs(request);

        try {
            logInfoWithTracker(String.format("Parsed arguments %s", options.toString()), options.getTrackingId());

            String jobName = createJobs(options);

            resultMessage = String.format("DLP job created successfully id='%s'", jobName);
            logInfoWithTracker(resultMessage, options.getTrackingId());
        }catch (Exception ex){

            resultMessage = String.format("Function encountered an exception ='%s'", ex);
            logWithTracker(resultMessage, options.getTrackingId(), Level.SEVERE);
            //to fail the function and report to Cloud Error Reporting.
            throw ex;
        }

        writer.printf(resultMessage);
    }

    public static String createJobs(FunctionOptions options) throws IOException {

        // get configs from environment
        List<String> infoTypesStrList = Utils.tokenize(Utils.getConfigFromEnv("INFO_TYPES",true),",",true);
        String minLiklihoodStr = Utils.getConfigFromEnv("MIN_LIKELIHOOD", true);
        String maxFindingsStr =  Utils.getConfigFromEnv("MAX_FINDINGS_PER_ITEM", true);
        String samplingMethodStr =  Utils.getConfigFromEnv("SAMPLING_METHOD", true);
        String rowsLimitPercentStr =  Utils.getConfigFromEnv("SAMPLING_METHOD", true);

        DlpServiceClient dlpServiceClient = DlpServiceClient.create();

        // 1. Specify which table to inspect

        BigQueryTable bqTable = BigQueryTable.newBuilder()
                .setProjectId(options.getInputProjectId())
                .setDatasetId(options.getInputDatasetId())
                .setTableId(options.getInputTableId())
                .build();

        BigQueryOptions bqOptions = BigQueryOptions.newBuilder()
                .setTableReference(bqTable)
                .setSampleMethod(BigQueryOptions.SampleMethod.forNumber(Integer.valueOf(samplingMethodStr)))
                .setRowsLimitPercent(Integer.valueOf(rowsLimitPercentStr))
                .build();

        StorageConfig storageConfig =
                StorageConfig.newBuilder()
                        .setBigQueryOptions(bqOptions)
                        .build();

        // 2. Specify what to inspect for and how
        List<InfoType> infoTypes = infoTypesStrList.stream()
                .map(it -> InfoType.newBuilder().setName(it).build())
                .collect(Collectors.toList());

        // The minimum likelihood required before returning a match:
        // See: https://cloud.google.com/dlp/docs/likelihood
        Likelihood minLikelihood = Likelihood.valueOf(minLiklihoodStr);

        // The maximum number of findings to report (0 = server maximum)
        InspectConfig.FindingLimits findingLimits =
                InspectConfig.FindingLimits.newBuilder()
                        .setMaxFindingsPerItem(Integer.valueOf(maxFindingsStr))
                        .build();

        InspectConfig inspectConfig =
                InspectConfig.newBuilder()
                        .addAllInfoTypes(infoTypes)
                        .setIncludeQuote(true)
                        .setMinLikelihood(minLikelihood)
                        .setLimits(findingLimits)
                        .build();

        // 3. Specify saving detailed results to BigQuery.

        // Save detailed findings to BigQuery
        BigQueryTable outputBqTable = BigQueryTable.newBuilder()
                .setProjectId(options.getFindingsProjectId())
                .setDatasetId(options.getFindingsDatasetId())
                .setTableId(options.getFindingsTableId())
                .build();
        OutputStorageConfig outputStorageConfig = OutputStorageConfig.newBuilder()
                .setTable(outputBqTable)
                .build();
        Action.SaveFindings saveFindingsActions = Action.SaveFindings.newBuilder()
                .setOutputConfig(outputStorageConfig)
                .build();
        Action bqAction = Action.newBuilder()
                .setSaveFindings(saveFindingsActions)
                .build();

        // 4. Specify sending PubSub notification on completion.
        Action.PublishToPubSub publishToPubSub = Action.PublishToPubSub.newBuilder()
                .setTopic(options.getNotificationPubSubTopic())
                .build();
        Action pubSubAction = Action.newBuilder()
                .setPubSub(publishToPubSub)
                .build();

        // Configure the inspection job we want the service to perform.
        InspectJobConfig inspectJobConfig =
                InspectJobConfig.newBuilder()
                        .setInspectConfig(inspectConfig)
                        .setStorageConfig(storageConfig)
                        .addActions(bqAction)
                        .addActions(pubSubAction)
                        .build();

        // Construct the job creation request to be sent by the client.
        CreateDlpJobRequest createDlpJobRequest =
                CreateDlpJobRequest.newBuilder()
                        .setJobId(options.getTrackingId())
                        .setParent(LocationName.of(options.getDlpProject(), options.getDlpRegion()).toString())
                        .setInspectJob(inspectJobConfig)
                        .build();

        // Send the job creation request and process the response.
        DlpJob createdDlpJob = dlpServiceClient.createDlpJob(createDlpJobRequest);
        return createdDlpJob.getName();
    }

    public FunctionOptions parseArgs(HttpRequest request) throws IOException {

        JsonElement requestParsed = gson.fromJson(request.getReader(), JsonElement.class);
        JsonObject requestJson = null;

        if (requestParsed != null && requestParsed.isJsonObject()) {
            requestJson = requestParsed.getAsJsonObject();
        }

        String inputProjectId = getArgFromJsonOrQueryParams(requestJson, request, "inputProjectId");
        String inputDatasetId = getArgFromJsonOrQueryParams(requestJson, request, "inputDatasetId");
        String inputTableId = getArgFromJsonOrQueryParams(requestJson, request, "inputTableId");

        String findingsProjectId = getArgFromJsonOrQueryParams(requestJson, request, "findingsProjectId");
        String findingsDatasetId = getArgFromJsonOrQueryParams(requestJson, request, "findingsDatasetId");
        String findingsTableId = getArgFromJsonOrQueryParams(requestJson, request, "findingsTableId");

        String notificationPubSubTopic = getArgFromJsonOrQueryParams(requestJson, request, "notificationPubSubTopic");

        String dlpProject = getArgFromJsonOrQueryParams(requestJson, request, "dlpProject");
        String dlpRegion = getArgFromJsonOrQueryParams(requestJson, request, "dlpRegion");

        String trackingId = getArgFromJsonOrQueryParams(requestJson, request, "trackingId");

        return new FunctionOptions(
                inputProjectId,
                inputDatasetId,
                inputTableId,
                findingsProjectId,
                findingsDatasetId,
                findingsTableId,
                notificationPubSubTopic,
                dlpProject,
                dlpRegion,
                trackingId
                );
    }

    public String getArgFromJsonOrQueryParams(JsonObject requestJson, HttpRequest request, String argName) {

        // check in HttpRequest
        String arg = request.getFirstQueryParameter(argName).orElse("");

        // check in Json
        if (requestJson != null && requestJson.has(argName)) {
            arg = requestJson.get(argName).getAsString();
        }

        // validate it exists
        if (arg.isBlank())
            throw new IllegalArgumentException(String.format("%s is required", argName));

        return arg;
    }

    public void logInfoWithTracker(String log, String tracker){
        logWithTracker(log,tracker, Level.INFO);
    }

    public void logWithTracker(String log, String tracker, Level level){
        logger.log(level, String.format("tracker:%s %s",tracker,log));
    }

}