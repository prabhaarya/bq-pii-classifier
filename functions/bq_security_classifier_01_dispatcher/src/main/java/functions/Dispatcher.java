package functions;


import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Dispatcher implements HttpFunction {

    private static final Logger logger = Logger.getLogger(Dispatcher.class.getName());

    @Override
    public void service(HttpRequest request, HttpResponse response)
            throws IOException {

        var writer = new PrintWriter(response.getWriter());
        List<String> outputMessages = new ArrayList<>();

        // Generate a unique ID for this invocation
        String runId = String.format("R-%s",String.valueOf(System.currentTimeMillis()));
        String runIdMsg = String.format("Computed Run ID = %s",runId);
        outputMessages.add(runIdMsg);
        logInfoWithTracker(runIdMsg,runId);

        String projectId = Utils.getConfigFromEnv("PROJECT_ID", true);
        String regionId = Utils.getConfigFromEnv("REGION_ID", true);
        String queueId = Utils.getConfigFromEnv("QUEUE_ID", true);
        String serviceAccountEmail =Utils.getConfigFromEnv("SA_EMAIL", true);
        String httpEndPoint = Utils.getConfigFromEnv("HTTP_ENDPOINT", true);
        String resultsDataset = Utils.getConfigFromEnv("BQ_RESULTS_DATASET", true);
        String resultsTable = Utils.getConfigFromEnv("BQ_RESULTS_TABLE", true);
        String pubsubNotificationTopic = Utils.getConfigFromEnv("PUBSUB_NOTIFICATION_TOPIC", true);

        /**
         * Detecting which resources to scan is done bottom up TABLES > DATASETS > PROJECTS where lower levels configs (e.g. Tables)
         * ignore higher level configs (e.g. Datasets)
         * For example:
         * If TABLES_INCLUDE list is provided:
         *  * SCAN only these tables
         *  * SKIP tables in TABLES_EXCLUDE list
         *  * IGNORE all other INCLUDE lists
         * If DATASETS_INCLUDE list is provided:
         *  * SCAN only tables in these datasets
         *  * SKIP datasets in DATASETS_EXCLUDE
         *  * SKIP tables in TABLES_EXCLUDE
         *  * IGNORE all other INCLUDE lists
         * If PROJECTS_INCLUDE list is provided:
         *  * SCAN only datasets and tables in these projects
         *  * SKIP datasets in DATASETS_EXCLUDE
         *  * SKIP tables in TABLES_EXCLUDE
         *  * IGNORE all other INCLUDE lists
         */
        List<String> tableIncludeList = Utils.tokenize(Utils.getConfigFromEnv("TABLES_INCLUDE", false),",",false);
        List<String> tableExcludeList = Utils.tokenize(Utils.getConfigFromEnv("TABLES_EXCLUDE", false),",",false);
        List<String> datasetIncludeList = Utils.tokenize(Utils.getConfigFromEnv("DATASETS_INCLUDE", false),",",false);
        List<String> datasetExcludeList = Utils.tokenize(Utils.getConfigFromEnv("DATASETS_EXCLUDE", false),",",false);
        List<String> projectIncludeList = Utils.tokenize(Utils.getConfigFromEnv("PROJECTS_INCLUDE", false),",",false);

        DispatcherService dispatcherService = new DispatcherService(
                projectId,
                regionId,
                runId,
                resultsDataset,
                resultsTable,
                pubsubNotificationTopic,
                queueId,
                httpEndPoint,
                serviceAccountEmail,
                projectIncludeList,
                datasetIncludeList,
                datasetExcludeList,
                tableIncludeList,
                tableExcludeList
        );

        dispatcherService.execute();

        outputMessages.addAll(dispatcherService.getOutputMessages());
        for(String msg: outputMessages){
            writer.printf("%s \r\n", msg);
        }
    }

    public void logInfoWithTracker(String log, String tracker){
        logWithTracker(log,tracker,Level.INFO);
    }

    public void logWithTracker(String log, String tracker, Level level){
        logger.log(level, String.format("tracker:%s %s",tracker,log));
    }
}
