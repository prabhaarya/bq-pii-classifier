TODO: Confirm that the submitted job is doing sampling

JAVA client lib https://googleapis.dev/java/google-cloud-dlp/latest/index.html
Guide: https://cloud.google.com/functions/docs/writing/http

mvn compile
mvn function:run
  
gcloud functions deploy bq_security_classifier_02_inspector --region="${REGION}" --entry-point functions.Tagger --runtime java11 --trigger-http --timeout=540s





# creating SA for function

```

export SA_INSPECTOR=sa-sec-classifier-inspector

gcloud iam service-accounts create "${SA_INSPECTOR}" \
--description="CF runtime SA for Inspector function" \
--display-name="${SA_INSPECTOR}"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="serviceAccount:${SA_INSPECTOR}@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/dlp.jobsEditor"



```

# deploy function

```
gcloud functions deploy bq_security_classifier_02_inspector \
--region "${REGION}" \
--entry-point functions.Inspector \
--runtime java11 \
--trigger-http \
--timeout 540s \
--service-account ${SA_INSPECTOR}@${PROJECT_ID}.iam.gserviceaccount.com \
--env-vars-file config.yaml

```

# Test
```

export INFO_TYPES="EMAIL_ADDRESS, PHONE_NUMBER"
export MIN_LIKELIHOOD="LIKELY"
export MAX_FINDINGS_PER_ITEM="50"
export SAMPLING_METHOD="2"
export ROWS_LIMIT_PERCENT="10"

mvn function:run

http://localhost:8080?inputProjectId=facilities-910444929556&inputDatasetId=dlp&inputTableId=customers&findingsProjectId=facilities-910444929556&findingsDatasetId=dlp&findingsTableId=output&notificationPubSubTopic=projects/facilities-910444929556/topics/dlp-job-notifications&dlpProject=facilities-910444929556&dlpRegion=europe-west2&trackingId=test-tracking-id

```

# UI test 

test payload
```
{
   "inputProjectId":"facilities-910444929556",
   "inputDatasetId":"dlp",
   "inputTableId":"customers",
   "findingsProjectId":"facilities-910444929556",
   "findingsDatasetId":"dlp",
   "findingsTableId":"output",
   "notificationPubSubTopic":"projects/facilities-910444929556/topics/dlp-job-notifications",
   "dlpProject":"facilities-910444929556",
   "dlpRegion":"europe-west2",
   "trackingId":"test-tracking-id" 
}

```
