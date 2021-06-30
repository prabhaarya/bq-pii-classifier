package com.google.cloud.pso.bq_security_classifier.functions.tagger;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.BigqueryScopes;
import com.google.api.services.bigquery.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Tuple;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;
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
import com.google.privacy.dlp.v2.BigQueryTable;
import com.google.privacy.dlp.v2.DlpJob;
import com.google.cloud.pso.bq_security_classifier.helpers.LoggingHelper;
import com.google.cloud.pso.bq_security_classifier.helpers.Utils;

public class Tagger implements HttpFunction {

    private final LoggingHelper logger = new LoggingHelper(
            Tagger.class.getSimpleName(),
            applicationName,
            defaultLog,
            trackerLog,
            functionNumber);
    private static final String applicationName = "[bq-security-classifier]";
    private static final String defaultLog = "default-log";
    private static final String tagHistoryLog = "tag-history-log";
    private static final String trackerLog = "tracker-log";
    private static final Integer functionNumber = 4;

    private static final Gson gson = new Gson();


    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException, InterruptedException {

        var writer = new PrintWriter(response.getWriter());
        FunctionOptions options = parseArgs(request);

        // dlp job is created using the trackingId via the Inspector CF
        String trackingId = Utils.extractTrackingIdFromJobName(options.getDlpJobName());

        logger.logFunctionStart(trackingId);

        logger.logInfoWithTracker(trackingId, String.format("Parsed arguments : %s", options.toString()));

        try {
            DlpServiceClient dlpServiceClient = DlpServiceClient.create();
            DlpJob dlpJob = dlpServiceClient.getDlpJob(options.getDlpJobName());

            if (dlpJob.getState() != DlpJob.JobState.DONE) {
                // TODO: handle job states
                String msg = String.format("DLP Job '%s' state must be 'DONE'. Current state : '%s'. Function call will terminate. ",
                        options.getDlpJobName(),
                        dlpJob.getState());
                logger.logSevereWithTracker(trackingId, msg);
                writer.printf(msg);
                throw new RuntimeException(msg);
            }


            BigQueryTable targetTable = dlpJob.getInspectDetails()
                    .getRequestedOptions()
                    .getJobConfig()
                    .getStorageConfig()
                    .getBigQueryOptions()
                    .getTableReference();


            // Query DLP results in BQ and return a dict of bq_column=policy_tag
            Map<String, String> fieldsToPolicyTagsMap = getFieldsToPolicyTagsMap(options.getDlpJobName());
            logger.logInfoWithTracker(trackingId, String.format("Computed Fields to Policy Tags mapping : %s", fieldsToPolicyTagsMap.toString()));

            // Apply policy tags to columns in BigQuery
            applyPolicyTags(
                    targetTable.getProjectId(),
                    targetTable.getDatasetId(),
                    targetTable.getTableId(),
                    fieldsToPolicyTagsMap,
                    trackingId);

            logger.logFunctionEnd(trackingId);

            writer.printf("Call completed successfully.");
        } catch (Exception ex) {
            logger.logSevereWithTracker(trackingId, String.format("Exception: %s", ex.toString()));
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

            String dlpJobName = Utils.getArgFromJsonOrQueryParams(requestJson, request, "dlpJobName", true);

            return new FunctionOptions(dlpJobName);

        } catch (JsonParseException e) {

            logger.logSevereWithTracker("",  "Error parsing JSON: " + e.getMessage());
            throw e;
        }
    }

    public Map<String, String> getFieldsToPolicyTagsMap(String dlpJobName) throws InterruptedException {

        BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

        String projectId = Utils.getConfigFromEnv("PROJECT_ID", true);
        String datasetId = Utils.getConfigFromEnv("DATASET_ID", true);
        String filedsToInfoTypeFindingsView = Utils.getConfigFromEnv("BQ_VIEW_FIELDS_FINDINGS", true);

        // This view is defined in Terraform based on the dlp results table. If it needs update, do it in Terraform
        String queryTemplate = "SELECT field_name, policy_tag FROM `%s.%s.%s` WHERE job_name = '%s' ";

        String formattedQuery = String.format(queryTemplate,
                projectId,
                datasetId,
                filedsToInfoTypeFindingsView,
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
        Map<String, String> fieldsToPolicyTagMap = new HashMap<>();
        for (FieldValueList row : result.iterateAll()) {
            String fieldName = row.get("field_name").getStringValue();
            String infoTypeName = row.get("policy_tag").getStringValue();
            fieldsToPolicyTagMap.put(fieldName, infoTypeName);
        }

        return fieldsToPolicyTagMap;
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
                .setApplicationName(applicationName)
                .build();

        Table targetTable = bq.tables()
                .get(projectId, datasetId, tableId)
                .execute();

        List<TableFieldSchema> currentFields = targetTable.getSchema().getFields();
        List<TableFieldSchema> updatedFields = new ArrayList<>();

        // store all actions on policy tags and log them after patching the BQ table
        List<Tuple<String, Level>> policyUpdateLogs = new ArrayList<>();

        for (TableFieldSchema field : currentFields) {
            if (fieldsToPolicyTagsMap.containsKey(field.getName())) {

                String policyTagId = fieldsToPolicyTagsMap.get(field.getName());

                PolicyTags fieldPolicyTags = field.getPolicyTags();

                // if no policy exists on the field, attach one
                if (fieldPolicyTags == null) {
                    fieldPolicyTags = new PolicyTags().setNames(Arrays.asList(policyTagId));
                    field.setPolicyTags(fieldPolicyTags);

                    String logMsg = String.format("%s | %s | %s | %s | %s | %s | %s | %s",
                            projectId,
                            datasetId,
                            tableId,
                            field.getName(),
                            "",
                            policyTagId,
                            "CREATE",
                            ""
                    );
                    policyUpdateLogs.add(Tuple.of(logMsg, Level.INFO));
                } else {

                    // overwrite policy tag if it belongs to the same taxonomy only
                    String existingTaxonomy = Utils.extractTaxonomyIdFromPolicyTagId(fieldPolicyTags.getNames().get(0));
                    String newTaxonomy = Utils.extractTaxonomyIdFromPolicyTagId(policyTagId);

                    // update existing tags only if they belong to the same taxonomy
                    if (existingTaxonomy.equals(newTaxonomy)) {

                        fieldPolicyTags.setNames(Arrays.asList(policyTagId));

                        String logMsg = String.format("%s | %s | %s | %s | %s | %s | %s | %s",
                                projectId,
                                datasetId,
                                tableId,
                                field.getName(),
                                fieldPolicyTags.getNames().get(0),
                                policyTagId,
                                "OVERWRITE",
                                ""
                        );
                        policyUpdateLogs.add(Tuple.of(logMsg, Level.INFO));

                    } else {

                        String logMsg = String.format("%s | %s | %s | %s | %s | %s | %s | %s",
                                projectId,
                                datasetId,
                                tableId,
                                field.getName(),
                                fieldPolicyTags.getNames().get(0),
                                policyTagId,
                                "KEEP_EXISTING",
                                "Can't overwrite tags that belong to different taxonomies"
                        );
                        policyUpdateLogs.add(Tuple.of(logMsg, Level.WARNING));
                    }
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

        // log all actions on policy tags after bq.tables.patch operation is successful
        for (Tuple<String, Level> t: policyUpdateLogs){
            logger.logWithTracker(tagHistoryLog, trackingId, t.x(), t.y());
        }
    }
}