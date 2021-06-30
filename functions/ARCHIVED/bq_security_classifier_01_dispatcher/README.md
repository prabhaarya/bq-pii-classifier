# creating task queue

Reqquires:
enable Cloud tasks API
Creating app engine in the project 
```
export INSPECTOR_QUEUE=inspector-queue 
gcloud tasks queues create "${INSPECTOR_QUEUE}" --log-sampling-ratio=1.0
```


# creating SA for function

```

export SA_DISPATCHER=sa-sec-classifier-dispatcher

gcloud iam service-accounts create "${SA_DISPATCHER}" \
--description="CF runtime SA for functions.Dispatcher function" \
--display-name="${SA_DISPATCHER}"

# grant access to the created queue
gcloud tasks queues add-iam-policy-binding "${INSPECTOR_QUEUE}" \
--member="serviceAccount:${SA_DISPATCHER}@${PROJECT_ID}.iam.gserviceaccount.com" \
--role="roles/cloudtasks.enqueuer" \
--location="${REGION}"

# grant access to list datasets and tables
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="serviceAccount:${SA_DISPATCHER}@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/bigquery.metadataViewer"
```

# local testing
```
export PROJECT_ID=facilities-910444929556
export REGION_ID=europe-west2
export QUEUE_ID=inspector-queue
export SA_EMAIL=sa-inspector-tasks@facilities-910444929556.iam.gserviceaccount.com
export HTTP_ENDPOINT=https://europe-west2-facilities-910444929556.cloudfunctions.net/bq_security_classifier_02_inspector
export PUBSUB_NOTIFICATION_TOPIC=projects/facilities-910444929556/topics/dlp-job-notifications
export BQ_RESULTS_DATASET=dlp
export BQ_RESULTS_TABLE=output

# Optional fields. At least one should be provided among the _INCLUDE configs 
export TABLES_INCLUDE=
export DATASETS_INCLUDE=
export PROJECTS_INCLUDE="facilities-910444929556, zbooks-910444929556"
export DATASETS_EXCLUDE=
export TABLES_EXCLUDE=

mvn function:run

```

# deploy function
TODO: use service account for function
```
gcloud functions deploy bq_security_classifier_01_dispatcher \
--region "${REGION}" \
--entry-point functions.Dispatcher \
--runtime java11 \
--trigger-http \
--timeout 540s \
--service-account ${SA_DISPATCHER}@${PROJECT_ID}.iam.gserviceaccount.com \
--env-vars-file config.yaml
```

# SA DLP Task settings
```
export SA_INSPECTOR_TASKS=sa-inspector-tasks

gcloud iam service-accounts create "${SA_INSPECTOR_TASKS}" \
    --description="To authorize Cloud Tasks HTTP requests to Inspector CF" \
    --display-name="${SA_INSPECTOR_TASKS}"

# SA_INSPECTOR_TASKS must be able to invoke CF bq_security_classifier_02_inspector
gcloud functions add-iam-policy-binding  bq_security_classifier_02_inspector \
--region europe-west2 \
--project facilities-910444929556 \
--member serviceAccount:${SA_INSPECTOR_TASKS}@${PROJECT_ID}.iam.gserviceaccount.com \
--role roles/cloudfunctions.invoker


#CF runtime SA ${SA_DISPATCHER}@${PROJECT_ID}.iam.gserviceaccount.com
#must have roles/iam.serviceAccountUser on
#SA_INSPECTOR_TASKS
#to create cloud tasks with authori

gcloud iam service-accounts add-iam-policy-binding \
    ${SA_INSPECTOR_TASKS}@${PROJECT_ID}.iam.gserviceaccount.com \
    --member=serviceAccount:${SA_DISPATCHER}@${PROJECT_ID}.iam.gserviceaccount.com \
    --role=roles/iam.serviceAccountUser

```


