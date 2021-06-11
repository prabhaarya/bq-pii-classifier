
# creating task queue

Reqquires:
enable Cloud tasks API
Creating app engine in the project 
```
export TAGGER_QUEUE=tagger-queue 
gcloud tasks queues create "${TAGGER_QUEUE}" --log-sampling-ratio=1.0
```


# creating SA for function

```

export SA_LISTENER=sa-sec-classifier-listener

gcloud iam service-accounts create "${SA_LISTENER}" \
--description="CF runtime SA for functions.Listener function" \
--display-name="${SA_LISTENER}"

# grant access to the created queue
gcloud tasks queues add-iam-policy-binding "${TAGGER_QUEUE}" \
--member="serviceAccount:${SA_LISTENER}@${PROJECT_ID}.iam.gserviceaccount.com" \
--role="roles/cloudtasks.enqueuer" \
--location="europe-west2"
```

# listener function

background function to trigger on DLP PubSub notification to submit a Tagging task to Cloud Tasks

mvn compile
mvn function:run
  
```

gcloud functions deploy bq_security_classifier_03a_listener \
--region="${REGION}" \
--trigger-topic dlp-job-notifications \
--entry-point functions.Listener \
--runtime java11 \
--timeout 540s \
--env-vars-file config.yaml \
--service-account ${SA_LISTENER}@${PROJECT_ID}.iam.gserviceaccount.com
```  



# invoke with pubsub message

gcloud pubsub topics publish dlp-job-notifications --message="local_test" \
  --attribute="DlpJobName=projects/facilities-910444929556/locations/europe-west2/dlpJobs/i-test-pubsub-3"


# SA DLP Task settings
```
export SA_TAGGER_TASKS=sa-sec-classifier-tagger-tasks

gcloud iam service-accounts create "${SA_TAGGER_TASKS}" \
    --description="To authorize Cloud Tasks HTTP requests to Tagger CF" \
    --display-name="${SA_TAGGER_TASKS}"

gcloud functions add-iam-policy-binding  bq_security_classifier_03b_tagger \
--region europe-west2 --project facilities-910444929556 --member serviceAccount:sa-sec-classifier-tagger-tasks@facilities-910444929556.iam.gserviceaccount.com \
--role roles/cloudfunctions.invoker


#CF runtime SA ${SA_LISTENER}@${PROJECT_ID}.iam.gserviceaccount.com
#must have roles/iam.serviceAccountUser on
#sa-sec-classifier-tagger-tasks@facilities-910444929556.iam.gserviceaccount.com
#to create cloud tasks with authori

gcloud iam service-accounts add-iam-policy-binding \
    ${SA_TAGGER_TASKS}@${PROJECT_ID}.iam.gserviceaccount.com \
    --member=serviceAccount:${SA_LISTENER}@${PROJECT_ID}.iam.gserviceaccount.com \
    --role=roles/iam.serviceAccountUser

```
