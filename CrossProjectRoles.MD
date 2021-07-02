
sa_dispatcher: BQ list datasets role
DLP service account service-458743432870@dlp-api.iam.gserviceaccount.com: BQ read all data (including column level security)
sa_tagger: bigquery.tables.setCategory


Make sa-wadie-dev@facilities-910444929556.iam.gserviceaccount.com admin on target project to deploy


export TARGET_PROJECT=zbooks-910444929556

# to list datasets and tables in a project
gcloud projects add-iam-policy-binding "${TARGET_PROJECT}" \
    --member="serviceAccount:tf-sa-dispatcher@facilities-910444929556.iam.gserviceaccount.com" \
    --role="roles/bigquery.metadataViewer"
    
# to read and inspect bigquery data
gcloud projects add-iam-policy-binding "${TARGET_PROJECT}" \
    --member="serviceAccount:service-458743432870@dlp-api.iam.gserviceaccount.com" \
    --role="roles/bigquery.dataViewer"
    
# to read table schema and update tables policy tags
gcloud projects add-iam-policy-binding "${TARGET_PROJECT}" \
    --member="serviceAccount:tf-sa-tagger@facilities-910444929556.iam.gserviceaccount.com" \
    --role="roles/bigquery.dataOwner"



