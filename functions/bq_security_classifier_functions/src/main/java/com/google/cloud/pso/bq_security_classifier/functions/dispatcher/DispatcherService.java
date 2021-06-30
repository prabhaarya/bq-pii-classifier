package com.google.cloud.pso.bq_security_classifier.functions.dispatcher;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.gax.paging.Page;
import com.google.api.services.bigquery.BigqueryScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.*;
import com.google.cloud.pso.bq_security_classifier.helpers.LoggingHelper;
import com.google.cloud.tasks.v2.*;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import com.google.cloud.pso.bq_security_classifier.helpers.Utils;

public class DispatcherService {

    private final LoggingHelper logger;

    // attributes initialized by user.
    private String solutionProjectId;
    private String solutionRegionId;
    private String runId;
    private String dlpResultsDataset;
    private String dlpResultsTable;
    private String dlpPubsubNotificationTopic;
    private String queueId;
    private String httpEndPoint;
    private String serviceAccountEmail;
    private List<String> projectIncludeList;
    private List<String> datasetIncludeList;
    private List<String> datasetExcludeList;
    private List<String> tableIncludeList;
    private List<String> tableExcludeList;

    // attributes initialized by the service.
    private com.google.api.services.bigquery.Bigquery bqAPI;
    private BigQuery bqAPIWrapper;
    private String queuePath;
    private CloudTasksClient client;
    private OidcToken oidcToken;
    private List<String> outputMessages;

    public DispatcherService(LoggingHelper logger, String solutionProjectId, String solutionRegionId, String runId, String dlpResultsDataset, String dlpResultsTable, String dlpPubsubNotificationTopic, String queueId, String httpEndPoint, String serviceAccountEmail, List<String> projectIncludeList, List<String> datasetIncludeList, List<String> datasetExcludeList, List<String> tableIncludeList, List<String> tableExcludeList) throws IOException {
        this.logger = logger;
        this.solutionProjectId = solutionProjectId;
        this.solutionRegionId = solutionRegionId;
        this.runId = runId;
        this.dlpResultsDataset = dlpResultsDataset;
        this.dlpResultsTable = dlpResultsTable;
        this.dlpPubsubNotificationTopic = dlpPubsubNotificationTopic;
        this.queueId = queueId;
        this.httpEndPoint = httpEndPoint;
        this.serviceAccountEmail = serviceAccountEmail;
        this.projectIncludeList = projectIncludeList;
        this.datasetIncludeList = datasetIncludeList;
        this.datasetExcludeList = datasetExcludeList;
        this.tableIncludeList = tableIncludeList;
        this.tableExcludeList = tableExcludeList;

        initialize();
    }

    private void initialize() throws IOException{
        bqAPIWrapper = BigQueryOptions.getDefaultInstance().getService();

        // direct API calls are needed for some operations
        // TODO: follow up on the missing/faulty wrapper calls and stop using direct API calls
        bqAPI = new com.google.api.services.bigquery.Bigquery.Builder(
                new NetHttpTransport(),
                new JacksonFactory(),
                new HttpCredentialsAdapter(GoogleCredentials
                        .getApplicationDefault()
                        .createScoped(BigqueryScopes.all())))
                .setApplicationName("bq-security-classifier")
                .build();

        client = CloudTasksClient.create();
        // Construct the fully qualified Cloud Tasks queue name.

        queuePath = QueueName.of(solutionProjectId, solutionRegionId, queueId).toString();
        // Add your service account email to construct the OIDC token.
        // in order to add an authentication header to the request.
        oidcToken =
                OidcToken.newBuilder().setServiceAccountEmail(serviceAccountEmail).build();

        outputMessages = new ArrayList<>();
    }

    public void execute() throws IOException {

        logger.logInfoWithTracker(runId, String.format("will scan based on the given scope %s", getBqScanScopeAsStr()));

        if(!tableIncludeList.isEmpty()){
            processTables(tableIncludeList, tableExcludeList);
            return;
        }
        if(!datasetIncludeList.isEmpty()){
            processDatasets(datasetIncludeList, datasetExcludeList, tableExcludeList);
            return;
        }
        if(!projectIncludeList.isEmpty()){
            processProjects();
        }
    }

    public void processTables (List<String> tableIncludeList,
                               List<String> tableExcludeList){
        for(String table: tableIncludeList){
            if(!tableExcludeList.contains(table)){

                List<String> tokens = Utils.tokenize(table,".",true);
                String projectId = tokens.get(0);
                String datasetId = tokens.get(1);
                String tableId = tokens.get(2);

                String tableTracker = String.format("%s-%s-%s",
                        projectId,
                        datasetId,
                        tableId);

                String trackingId = String.format("%s-T-%s",runId, tableTracker);

                InspectorTask taskOptions = new InspectorTask();
                taskOptions.setTargetTableProject(projectId);
                taskOptions.setTargetTableDataset(datasetId);
                taskOptions.setTargetTable(tableId);
                taskOptions.setResultsTableProject(this.solutionProjectId);
                taskOptions.setResultsTableDataset(dlpResultsDataset);
                taskOptions.setResultsTable(dlpResultsTable);
                taskOptions.setPubSubNotificationTopic(dlpPubsubNotificationTopic);
                taskOptions.setDlpProject(this.solutionProjectId);
                taskOptions.setDlpRegion(solutionRegionId);
                taskOptions.setQueuePath(queuePath);
                taskOptions.setHttpEndPoint(httpEndPoint);
                taskOptions.setTrackingId(trackingId);

                Task task = createCloudTask(client, oidcToken, taskOptions);

                String logMsg = String.format("Cloud task created with id %s for tracker %s",
                        task.getName(),
                        trackingId);

                outputMessages.add(logMsg);
                logger.logInfoWithTracker(trackingId, logMsg);
            }
        }
    }

    public void processDatasets (List<String> datasetIncludeList,
            List<String> datasetExcludeList,
            List<String> tableExcludeList) throws IOException {

        for(String dataset: datasetIncludeList){
            if(!datasetExcludeList.contains(dataset)){

                List<String> tokens = Utils.tokenize(dataset,".",true);
                String projectId = tokens.get(0);
                String datasetId = tokens.get(1);

                // calling dataset.getLocation always returns null --> seems like a bug in the SDK
                // instead, use the underlying API call to get dataset info
                String datasetLocation = bqAPI.datasets()
                        .get(projectId, datasetId)
                        .execute()
                        .getLocation();

                /*
                 TODO: Support tagging in multiple locations

                 to support all locations:
                 1- Taxonomies/PolicyTags have to be created in each required location
                 2- Update the Tagger Cloud Function to read one mapping per location

                 For now, we don't submit tasks for tables in other locations than the PolicyTag location
                 */
                if(!datasetLocation.equals(solutionRegionId)){
                    logger.logWarnWithTracker(runId,
                            String.format(
                            "Ignoring dataset %s in location %s. Only location %s is configured",
                            dataset,
                            datasetLocation,
                            solutionRegionId)
                    );
                    continue;
                }

                Page<Table> tablesPages = bqAPIWrapper.listTables(DatasetId.of(projectId, datasetId));

                List<String> tablesIncludeList = StreamSupport.stream(
                        tablesPages.iterateAll().spliterator(),
                        false)
                        .map(t -> String.format("%s.%s.%s",projectId, datasetId, t.getTableId().getTable()))
                        .collect(Collectors.toList());

                if(tablesIncludeList.isEmpty()){
                    String msg = String.format(
                            "No tables found under dataset '%s'. Dataset might be empty or no permissions to list tables.",
                            dataset);

                    outputMessages.add(msg);
                    logger.logWarnWithTracker(getRunId(), msg);
                }

                processTables(tablesIncludeList, tableExcludeList);
            }
        }
    }

    public void processProjects () throws IOException {
        logger.logInfoWithTracker(runId, String.format("Will process projects %s", projectIncludeList));

        for(String project: projectIncludeList){

            logger.logInfoWithTracker(runId, String.format("Inspecting project %s",project));

            Page<Dataset> datasets = bqAPIWrapper.listDatasets(project);

            // construct a list of datasets in the format project.dataset
            List<String> datasetIncludeList = StreamSupport.stream(
                    datasets.iterateAll().spliterator(),
                    false)
                    .map(d->String.format("%s.%s",project,d.getDatasetId().getDataset()))
                    .collect(Collectors.toList());

            if(datasetIncludeList.isEmpty()){
                String msg = String.format(
                        "No datasets found under project '%s'. Project might be empty or no permissions to list datasets.",
                        project);

                outputMessages.add(msg);
                logger.logWarnWithTracker(getRunId(), msg);
            }

            processDatasets(datasetIncludeList, datasetExcludeList, tableExcludeList);
        }
    }

    public Task createCloudTask(CloudTasksClient client, OidcToken oidcToken, InspectorTask inspectorTask){

        String payloadTemplate = "{\n" +
                "   \"inputProjectId\":\"%s\",\n" +
                "   \"inputDatasetId\":\"%s\",\n" +
                "   \"inputTableId\":\"%s\",\n" +
                "   \"findingsProjectId\":\"%s\",\n" +
                "   \"findingsDatasetId\":\"%s\",\n" +
                "   \"findingsTableId\":\"%s\",\n" +
                "   \"notificationPubSubTopic\":\"%s\",\n" +
                "   \"dlpProject\":\"%s\",\n" +
                "   \"dlpRegion\":\"%s\",\n" +
                "   \"trackingId\":\"%s\"\n" +
                "}";

        String payload = String.format(payloadTemplate,
                inspectorTask.getTargetTableProject(),
                inspectorTask.getTargetTableDataset(),
                inspectorTask.getTargetTable(),
                inspectorTask.getResultsTableProject(),
                inspectorTask.getResultsTableDataset(),
                inspectorTask.getResultsTable(),
                inspectorTask.getPubSubNotificationTopic(),
                inspectorTask.getDlpProject(),
                inspectorTask.getDlpRegion(),
                inspectorTask.getTrackingId()
        );

        // Construct the task body.
        Task.Builder taskBuilder =
                Task.newBuilder()
                        .setHttpRequest(
                                HttpRequest.newBuilder()
                                        .setBody(ByteString.copyFrom(payload, Charset.defaultCharset()))
                                        .setUrl(inspectorTask.getHttpEndPoint())
                                        .setHttpMethod(HttpMethod.POST)
                                        .setOidcToken(oidcToken)
                                        .build());

        // Send create task request.
        return client.createTask(inspectorTask.getQueuePath(), taskBuilder.build());
    }

    public String getSolutionProjectId() {
        return solutionProjectId;
    }

    public String getSolutionRegionId() {
        return solutionRegionId;
    }

    public String getRunId() {
        return runId;
    }

    public String getDlpResultsDataset() {
        return dlpResultsDataset;
    }

    public String getDlpResultsTable() {
        return dlpResultsTable;
    }

    public String getDlpPubsubNotificationTopic() {
        return dlpPubsubNotificationTopic;
    }

    public String getQueueId() {
        return queueId;
    }

    public String getHttpEndPoint() {
        return httpEndPoint;
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public List<String> getProjectIncludeList() {
        return projectIncludeList;
    }

    public List<String> getDatasetIncludeList() {
        return datasetIncludeList;
    }

    public List<String> getDatasetExcludeList() {
        return datasetExcludeList;
    }

    public List<String> getTableIncludeList() {
        return tableIncludeList;
    }

    public List<String> getTableExcludeList() {
        return tableExcludeList;
    }

    public List<String> getOutputMessages() {
        return outputMessages;
    }

    public String getBqScanScopeAsStr() {
        return "BQ scan scope{" +
                "projectIncludeList=" + projectIncludeList +
                ", datasetIncludeList=" + datasetIncludeList +
                ", datasetExcludeList=" + datasetExcludeList +
                ", tableIncludeList=" + tableIncludeList +
                ", tableExcludeList=" + tableExcludeList +
                '}';
    }
}
