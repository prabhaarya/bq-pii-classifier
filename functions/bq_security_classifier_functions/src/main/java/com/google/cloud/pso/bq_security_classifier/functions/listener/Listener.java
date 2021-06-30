package com.google.cloud.pso.bq_security_classifier.functions.listener;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.pso.bq_security_classifier.functions.tagger.Tagger;
import com.google.cloud.pso.bq_security_classifier.helpers.LoggingHelper;
import com.google.cloud.pso.bq_security_classifier.helpers.Utils;
import com.google.cloud.tasks.v2.*;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Listener implements BackgroundFunction<PubSubMessage> {

    private final LoggingHelper logger = new LoggingHelper(
            Listener.class.getSimpleName(),
            applicationName,
            defaultLog,
            trackerLog,
            functionNumber);
    private static final String applicationName = "[bq-security-classifier]";
    private static final String defaultLog = "default-log";
    private static final String trackerLog = "tracker-log";
    private static final Integer functionNumber = 3;

    @Override
    public void accept(PubSubMessage pubSubMessage, Context context) throws IOException {


        String dlpJobName = pubSubMessage.attributes.getOrDefault("DlpJobName", "");

        if(dlpJobName.isBlank()){
            throw new IllegalArgumentException("PubSub message attribute 'DlpJobName' is missing.");
        }

        // dlp job is created using the trackingId via the Inspector CF
        String trackingId = Utils.extractTrackingIdFromJobName(dlpJobName);

        logger.logFunctionStart(trackingId);

        logger.logInfoWithTracker(trackingId, String.format("Parsed DlpJobName %s", dlpJobName));

        String projectId = Utils.getConfigFromEnv("PROJECT_ID", true);
        String regionId = Utils.getConfigFromEnv("REGION_ID", true);
        String queueId = Utils.getConfigFromEnv("QUEUE_ID", true);
        String serviceAccountEmail = Utils.getConfigFromEnv("SA_EMAIL", true);
        String httpEndPoint = Utils.getConfigFromEnv("HTTP_ENDPOINT", true);

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

        logger.logInfoWithTracker(trackingId, String.format("Task created: %s", task.getName()));

        logger.logFunctionEnd(trackingId);
    }


}