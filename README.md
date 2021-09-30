# bq-security-classifier


    
# Deployment

## Env setup
```
export PROJECT_ID=<>  # project to deploy to
export DLP
export REGION=europe-west2
export BUCKET_NAME=${PROJECT_ID}-bq-security-classifier
export BUCKET=gs://${BUCKET_NAME}
export CONFIG=<> # gcloud & terraform config name
export ACCOUNT=<>  # personal account

gcloud config configurations create $CONFIG
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

* Create a bucket for Terraform state
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
     * DLP service account is in the form service-<project number>@dlp-api.iam.gserviceaccount.com
     * Search in IAM for @dlp-api.iam.gserviceaccount.com (tick the "Include Google-Provided role grants" box)
     * If this host project never used DLP before, run a sample inspection job for GCP to create a service account
 * Set the `dlp_service_account` variable in the terraform variables file




### Deploy via Terraform

* Set Terraform Service Account
  * Terraform needs to run with a service account to deploy DLP resources. User accounts are not enough.  

```
export TF_SA=sa-terraform

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

gcloud iam service-accounts add-iam-policy-binding \
    $TF_SA@$PROJECT_ID.iam.gserviceaccount.com \
    --member="user:${ACCOUNT}" \
    --role="roles/iam.serviceAccountTokenCreator"
```

* Deploy solution

```
cd terraform

terraform init \
    -backend-config="bucket=${BUCKET_NAME}" \
    -backend-config="prefix=terraform-state"

terraform workspace new $CONFIG
# or
terraform workspace select $CONFIG

terraform plan -var-file=$VARS

terraform apply -var-file=$VARS -auto-approve

```

# Configure Data Projects

The application is deployed under a host project as set in the `PROJECT_ID` variable.
To enable the application to scan and tag columns in other projects (i.e. data projects) one must grant a number of
permissions on each data project. To do, run the following script for each data project:

PS: update the SA emails if the default names have been changed

```
export SA_DISPATCHER_EMAIL=sa-sc-dispatcher@${PROJECT_ID}.iam.gserviceaccount.com
export SA_TAGGER_EMAIL=sa-sc-tagger@${PROJECT_ID}.iam.gserviceaccount.com
export SA_DLP_EMAIL=service-$PROJECT_NUMBER0@dlp-api.iam.gserviceaccount.com

./scripts/prepare_data_projects.sh "bqsc-marketing" $SA_DISPATCHER_EMAIL $SA_TAGGER_EMAIL $SA_DLP_EMAIL
./scripts/prepare_data_projects.sh "bqsc-finance" $SA_DISPATCHER_EMAIL $SA_TAGGER_EMAIL $SA_DLP_EMAIL
./scripts/prepare_data_projects.sh "bqsc-dwh" $SA_DISPATCHER_EMAIL $SA_TAGGER_EMAIL $SA_DLP_EMAIL
```


# Reporting

Get the latest run_id

```
SELECT MAX(run_id) max_run_id, TIMESTAMP_MILLIS(CAST(SUBSTR(MAX(run_id), 3) AS INT64)) AS start_time, FROM `bq_security_classifier.v_steps`

```

Monitor each invocation of Cloud Functions

```
SELECT * FROM `bq_security_classifier.v_steps` WHERE run_id = 'R-1631537508737'

```

Monitor failed runs (per table)

```
SELECT * FROM `bq_security_classifier.v_broken_steps` WHERE run_id = 'R-1631537508737'

```

Monitor column tagging activities

```
SELECT th.start_time,
th.run_id,
th.tracker,
th.project_id,
th.dataset_id,
th.table_id,
th.field_id,
 m.info_type,
 th.existing_policy_tag,
 th.new_policy_tag,
 th.operation,
 th.details
 FROM `bq_security_classifier.v_log_tag_history` th
INNER JOIN `bq_security_classifier.v_config_infotypes_policytags_map` m
ON th.new_policy_tag = m.policy_tag
WHERE run_id = 'R-1631537508737'
ORDER BY tracker


```

# Updating DLP Info Types
Steps to add/change an InfoType:
* Add InfoType to the [inspection_template](terraform/modules/dlp/main.tf)
* In your .tfvars file Add a mapping entry to variable infoTypeName_policyTagName_map (info type to policy tag name)
e.g. {info_type = "EMAIL_ADDRESS", policy_tag = "email"}
* Apply terraform (will create/update the inspection template)




# GCP rate limiting
Inspector Function:
* DLP: 600 requests per min --> DISPATCH_RATE = 10 (per sec)
* DLP: 1000 running jobs --> handle via retries since creating jobs is async

Tagger Function:
* Maximum rate of dataset metadata update operations (including patch) 
* 5 operations every 10 seconds per dataset --> DISPATCH_RATE = 1 (per sec)
 (pessimistic setting assuming 1 dataset)(rely on retries as fallback)

Cloud Tasks Configurations:
 DISPATCH_RATE is actually the rate at which tokens in the bucket are refreshed. In conditions where there is a relatively steady flow of tasks, this is the equivalent of the rate at which tasks are dispatched.
 MAX_RUNNING is the maximum number of tasks in the queue that can run at once.
 