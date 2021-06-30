# tagger function

# Info

BQ Limit
Maximum rate of dataset metadata update operations (including patch) — 5 operations every 10 seconds per dataset


# Local setup


mvn compile
mvn function:run
  

Local testing

in terminal
export MAP_EMAIL_ADDRESS=projects/facilities-910444929556/locations/europe-west2/taxonomies/5039015961740517867/policyTags/1931626256428401592 
mvn function:run

in browser
http://localhost:8080/?dlpJobName=projects/facilities-910444929556/locations/europe-west2/dlpJobs/i-679460788514711677 

test payload
{
"dlpJobName":"projects/facilities-910444929556/locations/europe-west2/dlpJobs/i-679460788514711677"
}


# Set up SA access
Must have access to 
dlp.jobs.get --> retrive job info
BigQuery setCategory --> apply policy tags
bigquery.tables.update --> update table
bigquery.jobs.create --> submit a big query job
//TODO: grant read access to a dataset that holds DLP table results


```
export SA_TAGGER=sa-sec-classifier-tagger
export ROLE_TAGGER=secClassifierTagger

gcloud iam service-accounts create "${SA_TAGGER}" \
    --description="CF runtime SA for Tagger function" \
    --display-name="${SA_TAGGER}"

gcloud iam roles create "${ROLE_TAGGER}" --project=${PROJECT_ID}\
  --permissions=bigquery.tables.setCategory,dlp.jobs.get,datacatalog.taxonomies.get\ 
  --stage=GA

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="serviceAccount:${SA_TAGGER}@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="projects/${PROJECT_ID}/roles/${ROLE_TAGGER}"

// TODO: grant granular permissions to the SA role instead

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="serviceAccount:${SA_TAGGER}@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/bigquery.dataEditor"





```

# Deploy 
with service account 
```
gcloud functions deploy bq_security_classifier_03b_tagger \ 
--region="${REGION}" \
 --entry-point functions.tagger.Tagger \
 --runtime java11 \
 --trigger-http \
 --timeout=540s \
 --env-vars-file config.yaml \
 --service-account=${SA_TAGGER}@${PROJECT_ID}.iam.gserviceaccount.com

```

