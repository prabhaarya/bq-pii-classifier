# bq-security-classifier


    
# Deployment

## Env setup
```
export PROJECT_ID=prj-vm-n-data-bqprivacy-poc-01
export REGION=europe-west2
export BUCKET=gs://${PROJECT_ID}-bq-security-classifier
export ACCOUNT=admin@wadie.joonix.net

gcloud config configurations create vrigin-media
gcloud config set project $PROJECT_ID
gcloud config set compute/region $REGION
gcloud config set account $ACCOUNT

gcloud auth login

gcloud auth application-default login
```

## GCP Set up

* Enable App Engine API in the project and create an application (for cloud tasks and scheduler to work)
* Enable APIs
  * Enable [Cloud Resource Manager API](https://console.cloud.google.com/apis/library/cloudresourcemanager.googleapis.com)
  * Enable [IAM API](https://console.developers.google.com/apis/api/iam.googleapis.com/overview)
  * Enable [Data Catalog API](https://console.developers.google.com/apis/api/datacatalog.googleapis.com/overview)
  * Enable [Cloud Tasks API](https://console.developers.google.com/apis/api/cloudtasks.googleapis.com/overview)
  * Enable [Cloud Functions API](https://console.developers.google.com/apis/api/cloudfunctions.googleapis.com/overview)


## Prepare Terraform 

* Create a bucket for Terraform state and update [backend.tf](terraform/backend.tf)
```
gsutil mb -p $PROJECT_ID -l $REGION -b on $BUCKET
```

* Create a new .tfvars file and update the variables
```
export VARS=my-variables.tfvars
```


* DLP service account must have Fine-Grained Reader role in order to inspect tagged columns for new data.
Steps:
 * Detect the DLP service account in the host project
     * Search in IAM for @dlp-api.iam.gserviceaccount.com (tick the "Include Google-Provided role grants" box)
     * If this host project never used DLP before, run a sample inspection job for GCP to create a service account
 * Set the `dlp_service_account` variable in the terraform variables file




### Option 1: Deploy Terraform from local machine

* Set Terraform Service Account
  * Terraform needs to run with a service account to deploy DLP resources

```
export TF_SA=sa-terraform
export TF_SA_KEY_FILE_PATH=/Users/wadie/terraform_sa_key.json

gcloud iam service-accounts create $TF_SA \
    --description="Used by Terraform to deploy GCP resources" \
    --display-name="Terraform Service Account"

gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:${TF_SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/owner"

gcloud iam service-accounts add-iam-policy-binding \
    $TF_SA@$PROJECT_ID.iam.gserviceaccount.com \
    --member="user:${ACCOUNT}" \
    --role="roles/iam.serviceAccountUser"

gcloud iam service-accounts keys create $TF_SA_KEY_FILE_PATH \
    --iam-account=$TF_SA@$PROJECT_ID.iam.gserviceaccount.com

export GOOGLE_APPLICATION_CREDENTIALS=$TF_SA_KEY_FILE_PATH

```

  * TODO: limit terraform sa role instead of using roles/owner

* Deploy solution

```
cd terraform

alias tf=terraform

tf init

tf plan -var-file=$VARS

tf apply -var-file=$VARS -auto-approve

```

### Option 2: Deploy via Cloud Build 
If you can't create or export service account keys you can run the deployment script 
within Cloud Build via a [cloudbuild.yaml](cloudbuild.yaml) file.

Steps:
* Grant the Cloud Build service account all permissions needed to deploy resources
* Run 
```
gcloud builds submit --gcs-source-staging-dir="$BUCKET/cloud-build/"

```

# Configure Data Projects
The application is deployed under a host project as set in the `PROJECT_ID` variable.
To enable the application to scan and tag columns in other projects (i.e. data projects) one must grant a number of
permissions on each data project. To do, run the following script for each data project:


```
export SA_DISPATCHER_EMAIL=sa-sc-dispatcher@${PROJECT_ID}.iam.gserviceaccount.com
export SA_TAGGER_EMAIL=sa-sc-tagger@${PROJECT_ID}.iam.gserviceaccount.com
export SA_DLP_EMAIL=service-414082511776@dlp-api.iam.gserviceaccount.com

./scripts/prepare_data_projects.sh "bqsc-marketing" $SA_DISPATCHER_EMAIL $SA_TAGGER_EMAIL $SA_DLP_EMAIL
./scripts/prepare_data_projects.sh "bqsc-finance" $SA_DISPATCHER_EMAIL $SA_TAGGER_EMAIL $SA_DLP_EMAIL
./scripts/prepare_data_projects.sh "bqsc-dwh" $SA_DISPATCHER_EMAIL $SA_TAGGER_EMAIL $SA_DLP_EMAIL
```


##### OLD DOCS ####

# rate limiting
Inspector
- DLP: 600 requests per min --> DISPATCH_RATE = 10 (per sec)
- DLP: 1000 running jobs --> handle via retries since creating jobs is async
Tagger
Maximum rate of dataset metadata update operations (including patch) 
 — 5 operations every 10 seconds per dataset --> DISPATCH_RATE = 1 (per sec)
 (pessimistic setting assuming 1 dataset)(rely on retries as fallback)
 
 DISPATCH_RATE is actually the rate at which tokens in the bucket are refreshed. In conditions where there is a relatively steady flow of tasks, this is the equivalent of the rate at which tasks are dispatched.
 MAX_RUNNING is the maximum number of tasks in the queue that can run at once.
 