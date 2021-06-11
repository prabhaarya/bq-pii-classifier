package functions;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.BigqueryScopes;
import com.google.api.services.bigquery.model.*;

import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;


import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;


import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


import com.google.api.services.bigquery.model.TableFieldSchema.PolicyTags;


import com.google.api.services.bigquery.model.Table;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;

import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.privacy.dlp.v2.Action;
import com.google.privacy.dlp.v2.BigQueryTable;
import com.google.privacy.dlp.v2.DlpJob;

public class Tagger implements HttpFunction {

    private static final Logger logger = Logger.getLogger(Tagger.class.getName());
    private static final Gson gson = new Gson();
    private static final String mapping_prefix = "MAP_";

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException, InterruptedException {

        var writer = new PrintWriter(response.getWriter());
        FunctionOptions options = parseArgs(request);

        // dlp job is created using the trackingId via the Inspector CF
        String trackingId = extractTrackingIdFromJobName(options.getDlpJobName());

        logInfoWithTracker(String.format("Parsed arguments : %s", options.toString()), trackingId);

        try {
            DlpServiceClient dlpServiceClient = DlpServiceClient.create();
            DlpJob dlpJob = dlpServiceClient.getDlpJob(options.getDlpJobName());

            if (dlpJob.getState() != DlpJob.JobState.DONE) {
                // TODO: handle job states
                String msg = String.format("DLP Job State must be 'DONE'. Current state' : %s", dlpJob.getState());
                logWithTracker(msg, trackingId, Level.SEVERE);
                writer.printf(msg);
                return;
            }

            BigQueryTable targetTable = dlpJob.getInspectDetails()
                    .getRequestedOptions()
                    .getJobConfig()
                    .getStorageConfig()
                    .getBigQueryOptions()
                    .getTableReference();

            BigQueryTable dlpResultsTable = dlpJob.getInspectDetails()
                    .getRequestedOptions()
                    .getJobConfig()
                    .getActionsList()
                    .stream()
                    .filter(Action::hasSaveFindings) //multiple publish actions might exists
                    .map(actions -> actions.getSaveFindings().getOutputConfig().getTable())
                    .collect(Collectors.toUnmodifiableList())
                    .get(0);

            logInfoWithTracker(String.format("Retrieved target table : %s", targetTable), trackingId);
            logInfoWithTracker(String.format("Retrieved results table : %s", dlpResultsTable), trackingId);

            // 1. Query DLP results in BQ and return a dict of bq_column=InfoType
            Map<String, String> fieldsInfoTypeMapping = getFieldsInfoTypeMapping(
                    options.getDlpJobName(),
                    dlpResultsTable.getProjectId(),
                    dlpResultsTable.getDatasetId(),
                    dlpResultsTable.getTableId()
            );

            logInfoWithTracker(String.format("Retrieved fields to infoType mapping : %s", fieldsInfoTypeMapping), trackingId);

            Map<String, String> infoTypesToPolicyTagsMap = getInfoTypePolicyTagMapping(mapping_prefix);

            logInfoWithTracker(String.format("InfoTypes to PolicyTags config :  %s", infoTypesToPolicyTagsMap.toString()), trackingId);

            // 2. For each column Lookup InfoType and map to PolicyTag ID
            Map<String, String> fieldsToPolicyTagsMap = getFieldsPolicyTagsMapping(
                    fieldsInfoTypeMapping,
                    infoTypesToPolicyTagsMap);

            logInfoWithTracker(String.format("Computed Fields to Policy Tags mapping : %s", fieldsToPolicyTagsMap.toString()), trackingId);

            // 3. Apply policy tags to columns in BigQuery
            applyPolicyTags(
                    targetTable.getProjectId(),
                    targetTable.getDatasetId(),
                    targetTable.getTableId(),
                    fieldsToPolicyTagsMap,
                    trackingId);

            writer.printf("Call completed successfully.");
        }catch (Exception ex){
            logWithTracker(String.format("Exception: %s", ex.toString()), trackingId, Level.SEVERE);
            writer.printf("Call Failed. Check logs for more details. %s", ex.getMessage());
            //rethrow to appear in Cloud Error Reporting
            throw ex;
        }
    }


    public FunctionOptions parseArgs(HttpRequest request) throws IOException {

        try {
            JsonElement requestParsed = gson.fromJson(request.getReader(), JsonElement.class);
            JsonObject requestJson = null;

            if (requestParsed != null && requestParsed.isJsonObject()) {
                requestJson = requestParsed.getAsJsonObject();
            }

            String dlpJobName = getArgFromJsonOrQueryParams(requestJson, request, "dlpJobName");

            return new FunctionOptions(dlpJobName);

        } catch (JsonParseException e) {
            logger.severe("Error parsing JSON: " + e.getMessage());
            throw e;
        }
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

    public Map<String, String> getFieldsInfoTypeMapping(String dlpJobName,
                                                             String dlpResultsProject,
                                                             String dlpResultsDataset,
                                                             String dlpResultsTable) throws InterruptedException {

        BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

        String queryTemplate = "SELECT DISTINCT \n" +
                "l.record_location.field_id.name AS field_name,\n" +
                "info_type.name AS infotype_name\n" +
                "FROM `%s.%s.%s` o, UNNEST(location.content_locations) l\n" +
                "WHERE job_name = '%s'";

        String formattedQuery = String.format(queryTemplate,
                dlpResultsProject,
                dlpResultsDataset,
                dlpResultsTable,
                dlpJobName
        );

        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(formattedQuery)
                        .setUseLegacySql(false)
                        .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            // You can also look at queryJob.getStatus().getExecutionErrors() for all
            // errors, not just the latest one.
            throw new RuntimeException(queryJob.getStatus().getError().toString());
        }

        TableResult result = queryJob.getQueryResults();

        // Construct a mapping between field names and DLP infotypes
        Map<String, String> fieldsToInfoTypeMap = new HashMap<>();
        for (FieldValueList row : result.iterateAll()) {
            String fieldName = row.get("field_name").getStringValue();
            String infoTypeName = row.get("infotype_name").getStringValue();
            fieldsToInfoTypeMap.put(fieldName, infoTypeName);
        }

        return fieldsToInfoTypeMap;
    }

    public Map<String, String> getInfoTypePolicyTagMapping(String prefix) {

        Map<String, String> envMap = System.getenv();
        Map<String, String> infoTypeToPolicyTagsMap = new HashMap<>();
        for (String env : envMap.keySet()) {
            if (env.startsWith(prefix)) {
                infoTypeToPolicyTagsMap.putIfAbsent(env.substring(prefix.length()), envMap.get(env));
            }
        }

        return infoTypeToPolicyTagsMap;
    }

    public Map<String, String> getFieldsPolicyTagsMapping(Map<String, String> fieldsToInfoTypeMap,
                                                          Map<String, String> infoTypesToPolicyTagsMap) {

        // Lookup PolicyTags for each field based on it's InfoType
        Map<String, String> fieldsToPolicyTagsMap = new HashMap<>();
        for (String field : fieldsToInfoTypeMap.keySet()) {
            String fieldInfoType = fieldsToInfoTypeMap.get(field);
            String infoTypePolicyTag = infoTypesToPolicyTagsMap.get(fieldInfoType);
            fieldsToPolicyTagsMap.put(field, infoTypePolicyTag);
        }

        return fieldsToPolicyTagsMap;
    }

    public void applyPolicyTags(String projectId,
                                String datasetId,
                                String tableId,
                                Map<String, String> fieldsToPolicyTagsMap,
                                String trackingId) throws IOException {

        // TODO: Look for wrappers on top of these API calls
        Bigquery bq = new Bigquery.Builder(
                new NetHttpTransport(),
                new JacksonFactory(),
                new HttpCredentialsAdapter(GoogleCredentials
                        .getApplicationDefault()
                        .createScoped(BigqueryScopes.all())))
                .setApplicationName("bq-security-classifier")
                .build();

        Table targetTable = bq.tables()
                .get(projectId, datasetId, tableId)
                .execute();

        List<TableFieldSchema> currentFields = targetTable.getSchema().getFields();
        List<TableFieldSchema> updatedFields = new ArrayList<>();

        for (TableFieldSchema field : currentFields) {
            if (fieldsToPolicyTagsMap.containsKey(field.getName())) {

                String policyTagId = fieldsToPolicyTagsMap.get(field.getName());

                PolicyTags fieldPolicyTags = field.getPolicyTags();

                // if no policy exists on the field, attach one
                if (fieldPolicyTags == null) {
                    logInfoWithTracker(String.format("field '%s' has no existing policy tag. Will attach a new one '%s'",
                            field.getName(),
                            policyTagId),trackingId);

                    fieldPolicyTags = new PolicyTags().setNames(Arrays.asList(policyTagId));
                    field.setPolicyTags(fieldPolicyTags);
                } else {

                    logInfoWithTracker(String.format("field '%s' has an existing policy tag '%s'. Will overwrite existing one with '%s'",
                            field.getName(),
                            fieldPolicyTags.getNames(),
                            policyTagId), trackingId);

                    fieldPolicyTags.setNames(Arrays.asList(policyTagId));
                }
            }
            updatedFields.add(field);
        }

        // patch the table with the new schema including new policy tags
        bq.tables()
                .patch(projectId,
                        datasetId,
                        tableId,
                        new Table().setSchema(new TableSchema().setFields(updatedFields)))
                .execute();
    }

    public void logInfoWithTracker(String log, String tracker){
        logWithTracker(log,tracker, Level.INFO);
    }

    public void logWithTracker(String log, String tracker, Level level){
        logger.log(level, String.format("tracker:%s %s",tracker,log));
    }

    /**
     *
     * @param jobName Dlp Job name in format projects/locations/dlpJobs/i-<tracking-number>
     * @return tracking-number part
     */
    public String extractTrackingIdFromJobName(String jobName){
        String [] splits = jobName.split("/");
        return  splits[splits.length-1].substring(2);
    }
}