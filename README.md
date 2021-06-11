# bq-security-classifier

# deployment
```
export PROJECT_ID=facilities-910444929556
export REGION=europe-west2
export SA_STARTER=cf-security-classifier-starter

gcloud config set project "${PROJECT_ID}"

# Needed to deploy Cloud Functions
gcloud services enable cloudbuild.googleapis.com

# TODO: Section about service accounts need update
# Service account to invoke starter function
gcloud iam service-accounts create "${SA_STARTER}" \
    --description="Invoke Starter Cloud Function for BQ security classifier solution" \
    --display-name="${SA_STARTER}"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="serviceAccount:${SA_STARTER}@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/cloudfunctions.invoker"

# Deploy Cloud Functions (from each folder)

gcloud functions deploy bq_security_classifier_01_dispatcher --region="${REGION}" --runtime python38 --trigger-http  --timeout=540s

gcloud functions deploy bq_security_classifier_02_inspector --region="${REGION}" --runtime python38 --trigger-http  --timeout=540s
 
```

GCP test
```
https://europe-west2-facilities-910444929556.cloudfunctions.net/bq_security_classifier_01_starter?project_id=facilities-910444929556

```

Local dev
```
python3 -m venv /tmp/python-venv/bq-securioty-classifier

source /tmp/python-venv/bq-securioty-classifier/bin/activate 

pip install functions-framework
pip install requests
pip install google-cloud-bigquery


# read https://github.com/GoogleCloudPlatform/functions-framework-python

# test functin locally
functions-framework --target=bq_security_classifier_01_starter

# debug locally 
functions-framework --target hello --debug


# On exit
deactivate

```

# needed APIs and permissions

Enable Cloud Build API - needed to deploy cloud functions


# TODO
* SA for starter function
* Invoke starter function per project from a config parser function 

* Service account(s) used to inspect BigQuery must have "Fine-Grained Reader" role
on all taxonomies (can we provide that on project level?). This is to enable reading
columns that have column level access control




# Testing DLP API manualy

ref: https://cloud.google.com/dlp/docs/auth#bearer
ref: https://cloud.google.com/dlp/docs/reference/rest/v2/projects.dlpJobs/create?apix_params=%7B%22parent%22%3A%22projects%2Ffacilities-910444929556%22%2C%22resource%22%3A%7B%22jobId%22%3A%22test%22%7D%7D#authorization-scopes

create a service account (created: sa-dlp)

Download key

gcloud auth activate-service-account --key-file PATHTOKEYFILE

gcloud auth print-access-token  

Edit the CURL request

curl --request POST \
  'https://dlp.googleapis.com/v2/projects/facilities-910444929556/locations/europe-west2/dlpJobs' \
  --header 'Authorization: Bearer ya29.c.KqYBAQjxg-nh5xUiaEMQ_sM_aX6jX12drQrB0fNjMwG_wTaJWUrUBbXtqbu_4MKl8mrwP4H7D6AbVD_muo6RWCFt2Rl9Wj7LWL_M-f1XC8s1S6BtOEAyyyPG9AQSIvcLxrTxfcI2oLHHFgjXLOM2gR3KMkaCkZscz9pfedqgY-gsDTF5MOo6JF4Emz1N9gTERswdYGvYY_1tJE3XhaJjzbdtT9ONZdrOfw'\
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --data @dlp-job.json
  
  
  /Users/wadie/Downloads/facilities-910444929556-1b2fd5482d23.json
  
  
  # Java function

JAVA client lib https://googleapis.dev/java/google-cloud-dlp/latest/index.html
Guide: https://cloud.google.com/functions/docs/writing/http

mvn compile
mvn function:run
  
gcloud functions deploy bq_security_classifier_02_inspector --region="${REGION}" --entry-point functions.Tagger --runtime java11 --trigger-http --timeout=540s

test payload
{
   "inputProjectId":"facilities-910444929556",
   "inputDatasetId":"dlp",
   "inputTableId":"customers",
   "findingsProjectId":"facilities-910444929556",
   "findingsDatasetId":"dlp",
   "findingsTableId":"output",
   "dlpProject":"facilities-910444929556",
   "dlpRegion":"europe-west2"
}


# Local account auth
switch between developer account and sa
gcloud auth list 

to confirm:
gcloud config list

# dlp service account setting

service-458743432870@dlp-api.iam.gserviceaccount.com
must have 
Fine-Grained Reader (to inspect BQ columns with column-access enables)


gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="service-458743432870@dlp-api.iam.gserviceaccount.com" \
    --role="roles/datacatalog.categoryFineGrainedReader"