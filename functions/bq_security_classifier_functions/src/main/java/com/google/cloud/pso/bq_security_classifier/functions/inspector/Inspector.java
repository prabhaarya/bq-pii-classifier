package com.google.cloud.pso.bq_security_classifier.functions.inspector;

import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.pso.bq_security_classifier.functions.listener.Listener;
import com.google.cloud.pso.bq_security_classifier.helpers.LoggingHelper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.privacy.dlp.v2.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.cloud.pso.bq_security_classifier.helpers.Utils;
import com.google.cloud.pso.bq_security_classifier.functions.inspector.FunctionOptions;

public class Inspector implements HttpFunction {

    private final LoggingHelper logger = new LoggingHelper(
            Inspector.class.getSimpleName(),
            applicationName,
            defaultLog,
            trackerLog,
            functionNumber);
    private static final String applicationName = "[bq-security-classifier]";
    private static final String defaultLog = "default-log";
    private static final String trackerLog = "tracker-log";
    private static final Integer functionNumber = 2;
    private static final Gson gson = new Gson();

    @Override
    public void service(HttpRequest request, HttpResponse response)
            throws IOException {

        var writer = new PrintWriter(response.getWriter());
        String resultMessage = "";

        FunctionOptions options = parseArgs(request);

        logger.logFunctionStart(options.getTrackingId());

        try {
            logger.logInfoWithTracker(options.getTrackingId(), String.format("Parsed arguments %s", options.toString()));

            String jobName = createJobs(options);

            resultMessage = String.format("DLP job created successfully id='%s'", jobName);
            logger.logInfoWithTracker(options.getTrackingId(), resultMessage);
        }catch (Exception ex){

            resultMessage = String.format("Function encountered an exception ='%s'", ex);
            logger.logSevereWithTracker(options.getTrackingId(), resultMessage);
            //to fail the function and report to Cloud Error Reporting.
            throw ex;
        }

        logger.logFunctionEnd(options.getTrackingId());

        writer.printf(resultMessage);
    }

    public static String createJobs(FunctionOptions options) throws IOException {

        // get configs from environment
        String minLiklihoodStr = Utils.getConfigFromEnv("MIN_LIKELIHOOD", true);
        String maxFindingsStr =  Utils.getConfigFromEnv("MAX_FINDINGS_PER_ITEM", true);
        String samplingMethodStr =  Utils.getConfigFromEnv("SAMPLING_METHOD", true);
        String rowsLimitPercentStr =  Utils.getConfigFromEnv("SAMPLING_METHOD", true);
        String dlpInspectionTemplateId =  Utils.getConfigFromEnv("DLP_INSPECTION_TEMPLATE_ID", true);

        DlpServiceClient dlpServiceClient = DlpServiceClient.create();

        // 1. Specify which table to inspect

        BigQueryTable bqTable = BigQueryTable.newBuilder()
                .setProjectId(options.getInputProjectId())
                .setDatasetId(options.getInputDatasetId())
                .setTableId(options.getInputTableId())
                .build();

        BigQueryOptions bqOptions = BigQueryOptions.newBuilder()
                .setTableReference(bqTable)
                .setSampleMethod(BigQueryOptions.SampleMethod.forNumber(Integer.parseInt(samplingMethodStr)))
                .setRowsLimitPercent(Integer.parseInt(rowsLimitPercentStr))
                .build();

        StorageConfig storageConfig =
                StorageConfig.newBuilder()
                        .setBigQueryOptions(bqOptions)
                        .build();


        // The minimum likelihood required before returning a match:
        // See: https://cloud.google.com/dlp/docs/likelihood
        Likelihood minLikelihood = Likelihood.valueOf(minLiklihoodStr);

        // The maximum number of findings to report (0 = server maximum)
        InspectConfig.FindingLimits findingLimits =
                InspectConfig.FindingLimits.newBuilder()
                        .setMaxFindingsPerItem(Integer.parseInt(maxFindingsStr))
                        .build();

        InspectConfig inspectConfig =
                InspectConfig.newBuilder()
                        .setIncludeQuote(false) // don't store identified PII in the table
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
                        .setInspectTemplateName(dlpInspectionTemplateId)
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

        String inputProjectId = Utils.getArgFromJsonOrQueryParams(requestJson, request, "inputProjectId", true);
        String inputDatasetId = Utils.getArgFromJsonOrQueryParams(requestJson, request, "inputDatasetId",true);
        String inputTableId = Utils.getArgFromJsonOrQueryParams(requestJson, request, "inputTableId",true);

        String findingsProjectId = Utils.getArgFromJsonOrQueryParams(requestJson, request, "findingsProjectId",true);
        String findingsDatasetId = Utils.getArgFromJsonOrQueryParams(requestJson, request, "findingsDatasetId",true);
        String findingsTableId = Utils.getArgFromJsonOrQueryParams(requestJson, request, "findingsTableId",true);

        String notificationPubSubTopic = Utils.getArgFromJsonOrQueryParams(requestJson, request, "notificationPubSubTopic",true);

        String dlpProject = Utils.getArgFromJsonOrQueryParams(requestJson, request, "dlpProject",true);
        String dlpRegion = Utils.getArgFromJsonOrQueryParams(requestJson, request, "dlpRegion",true);

        String trackingId = Utils.getArgFromJsonOrQueryParams(requestJson, request, "trackingId",true);

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
}