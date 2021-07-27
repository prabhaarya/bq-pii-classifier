# bq-security-classifier


    
# Deployment

## Env setup
```
export PROJECT_ID=prj-vm-n-data-bqprivacy-poc-01
export REGION=europe-west2
export BUCKET=gs://prj-vm-n-data-bqprivacy-poc-01-bq-security-classifier

gcloud auth login

gcloud config configurations create vrigin-media
gcloud config set project $PROJECT_ID
gcloud config set compute/region $REGION
gcloud config set account karim.wadie@virginmedia.co.uk

gcloud auth application-default login
```

## GCP Set up

* Enable App Engine API in the project and create an application (for cloud tasks and scheduler to work)

## Deploy Terraform

* Create a bucket for Terraform state and update [backend.tf](terraform/backend.tf)
```
gsutil mb -p $PROJECT_ID -l $REGION -b on $BUCKET
```

* Create a new .tfvars file and update the variables
```
export VARS=virgin-media-poc.tfvars
```

* Deploy solution

```
cd terraform

alias tf=terraform

tf init

tf plan -var-file=$VARS

tf apply -var-file=$VARS -auto-approve

```

```
gcloud builds submit --gcs-source-staging-dir="$BUCKET/cloud-build/"

```

# dlp service account setting

service-xyz@dlp-api.iam.gserviceaccount.com
must have 
Fine-Grained Reader (to inspect BQ columns with column-access enables)


gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="service-xyz@dlp-api.iam.gserviceaccount.com" \
    --role="roles/datacatalog.categoryFineGrainedReader"



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
 