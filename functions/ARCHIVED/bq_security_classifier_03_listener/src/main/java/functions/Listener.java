package functions;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.cloud.tasks.v2.*;
import com.google.protobuf.ByteString;

import java.nio.charset.Charset;

public class Listener implements BackgroundFunction<PubSubMessage> {
    private static final Logger logger = Logger.getLogger(Listener.class.getName());

    @Override
    public void accept(PubSubMessage pubSubMessage, Context context) throws IOException {


        String dlpJobName = pubSubMessage.attributes.getOrDefault("DlpJobName", "");

        if(dlpJobName.isBlank()){
            throw new IllegalArgumentException("PubSub message attribute 'DlpJobName' is missing.");
        }

        // dlp job is created using the trackingId via the Inspector CF
        String trackingId = extractTrackingIdFromJobName(dlpJobName);

        logger.info(String.format("%s | %s | %s | %s | %s",
                "[bq-security-classifier]",
                trackingId,
                Listener.class.getName(),
                "3",
                "Start"
        ));

        logInfoWithTracker(String.format("Parsed DlpJobName %s", dlpJobName), trackingId);

        String projectId = getConfigFromEnv("PROJECT_ID");
        String regionId = getConfigFromEnv("REGION_ID");
        String queueId = getConfigFromEnv("QUEUE_ID");
        String serviceAccountEmail = getConfigFromEnv("SA_EMAIL");
        String httpEndPoint = getConfigFromEnv("HTTP_ENDPOINT");

        CloudTasksClient client = CloudTasksClient.create();
        String payload = String.format("{\"dlpJobName\":\"%s\"}", dlpJobName);

        // Construct the fully qualified queue name.
        String queuePath = QueueName.of(projectId, regionId, queueId).toString();

        // Add your service account email to construct the OIDC token.
        // in order to add an authentication header to the request.
        OidcToken.Builder oidcTokenBuilder =
                OidcToken.newBuilder().setServiceAccountEmail(serviceAccountEmail);

        // Construct the task body.
        Task.Builder taskBuilder =
                Task.newBuilder()
                        .setHttpRequest(
                                HttpRequest.newBuilder()
                                        .setBody(ByteString.copyFrom(payload, Charset.defaultCharset()))
                                        .setUrl(httpEndPoint)
                                        .setHttpMethod(HttpMethod.POST)
                                        .setOidcToken(oidcTokenBuilder)
                                        .build());

        // Send create task request.
        Task task = client.createTask(queuePath, taskBuilder.build());

        logInfoWithTracker(String.format("Task created: %s", task.getName()), trackingId);

        logger.info(String.format("%s | %s | %s | %s | %s",
                "[bq-security-classifier]",
                trackingId,
                Listener.class.getName(),
                "3",
                "End"
        ));
    }

    public String getConfigFromEnv(String config){
        String value = System.getenv().getOrDefault(config, "");
        if(value.isBlank()){
            throw new IllegalArgumentException(String.format("Missing environment variable '%s'",config));
        }
        return value;
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