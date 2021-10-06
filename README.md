# BigQuery PII Classifier

# Overview

This a solution to automate the process of discovering and tagging
PII data across Bigquery tables and applying column level access controls to restrict access to 
specific PII data types to certain users/groups.

# Architecture

![alt text](diagrams/architecture.jpeg)

# Configuration

The solution is deployed by Terraform and thus all configurations are done
on the Terraform side.

## Create a Terraform .tfvars file

Create a new .tfvars file and override the variables

```
export VARS=my-variables.tfvars
```

## Configure Basic Variables

Most required variables have default names defined in [variables.tf](terraform/variables.tf).
You can use the defaults or overwrite them in the .tfvars file you just created.

Both ways, you must define the below variables:

```
project = "<GCP project ID to deploy solution to>"
region = "<GCP region>"
```

PS: Cloud tasks queues can't be re-created with the same name
after deleting them. If you deleted the queues (manually or via Terraform), you must provide
new names other than the defaults otherwise Terraform will fail to deploy them.

## Configure Scanning Scope

Override the following variables to define the scanning scope of the entry point Cloud Scheduler.

At least one variable should be provided among the _INCLUDE configs.

tables format: "project.dataset.table1, project.dataset.table2, etc"
datasets format: "project.dataset, project.dataset, etc"
projects format: "project1, project2, etc"

```
tables_include_list = ""
datasets_include_list = ""
projects_include_list = ""
datasets_exclude_list = ""
tables_exclude_list = ""
```
## Configure InfoTypes Mapping

For each project in scope, these policy tags will be created in the taxonomy and mapped in BQ configuration with the
generated policy_tag_id

PS: INFO_TYPEs configured in the [DLP inspection job](terraform/modules/dlp/main.tf) 
MUST be mapped here. Otherwise, mapping to policy tag ids will fail

```

infoTypeName_policyTagName_map = [
  {
    info_type = "EMAIL_ADDRESS",
    policy_tag = "email"
  },
  {
    info_type = "PHONE_NUMBER",
    policy_tag = "phone"
  },
  .. etc
]
```

## Configure Domain Mapping

Domains are logical units (e.g. departments, source system, etc) that
you can segregate access control based on it. For example, marketing PII readers
shouldn't have access to finance PII data.

This is done by creating a Policy Tag taxonomy per domain.

You can define one domain per project that will be applied to all
BigQuery tables inside it. Additionally, you can overwrite this default project 
domain on dataset level (e.g. in case of a DWH project having data from different domains).


```
domain_mapping = [
  {
    project = "marketing-project",
    domain = "marketing"
  },
  {
    project = "dwh-project",
    domain = "dwh"
    datasets = [
      {
        name = "demo_marketing",
        domain = "marketing"
      },
      {
        name = "demo_finance",
        domain = "finance"
      }
    ]
  }
]
```
## Configure domain-IAM mapping

For each domain you defined in the "domain_mapping" config, you must 
provide a list of one or more user or groups that will have access to PII
data tagged under this domain.

For users: "user:username@example.com"
For groups: "group:groupname@example.com"

For example:

```
domain_iam_mapping = {
  marketing = ["group:marketing-pii-readers@example.com"],
  finance = ["group:finance-pii-readers@example.com", "user:admin@example.com"],
  dwh = ["user:username1@example.com", "user:username2@example.com"],
}
```

## Configure DLP Service Account

* DLP service account must have Fine-Grained Reader role on the created taxonomies in order to inspect tagged columns for new data.
Steps:
 * Detect the DLP service account in the host project
     * DLP service account is in the form service-<project number>@dlp-api.iam.gserviceaccount.com
     * Search in IAM for @dlp-api.iam.gserviceaccount.com (tick the "Include Google-Provided role grants" box)
     * If this host project never used DLP before, run a sample inspection job for GCP to create a service account
 * Set the `dlp_service_account` variable in the terraform variables file

```
dlp_service_account = "service-<project number>@dlp-api.iam.gserviceaccount.com"

```

## Configure DryRun

By setting `is_dry_run = "True"` the solution will scan BigQuery tables 
for PII data, store the scan result, but it will not apply policy tags to columns.
Instead, the "Tagger" function will only log [actions](functions/bq_security_classifier_functions/src/main/java/com/google/cloud/pso/bq_security_classifier/functions/tagger/ColumnTaggingAction.java).

Check the Monitoring sections on how to access these logs.  

```
is_dry_run = "False"
```

## Configure Cloud Scheduler CRON

Configure the schedule on which the scan should take place.

PS: the current solution has one entry point/scheduler but one can extend the solution
by adding more schedulers that have different scanning scope and/or timing.

```
cron_expression = "0 0 * * *"
```

## Configure Table Scan Limits

This will define the scan limit of the DLP jobs when they inspect BigQuery tables. 
`limitType`: could be `NUMBER_OF_ROWS` or `PERCENTAGE_OF_ROWS`.
`limits`: key/value pairs of {interval_upper_limit, rows_to_sample}. For example,
`"limits": { "1000": "100" , "5000": "500"}` means that tables  with 0-1000
records will use a sample of 100 records, tables between 1001-5000 will sample 500 records
and tables 5001-INF will also use 500 records.

When using `PERCENTAGE_OF_ROWS` the rows_to_sample should be an integer between 1-100. For example,
20 means 20%.

```
table_scan_limits_json_config = "{\"limitType\": \"NUMBER_OF_ROWS\", \"limits\": {\"10000\": \"100\",\"100000\": \"5000\", \"1000000\": \"7000\"}}"
```

# Deployment

## Env setup
```
# project to deploy to
export PROJECT_ID=<> 
export REGION=europe-west2
export BUCKET_NAME=${PROJECT_ID}-bq-security-classifier
export BUCKET=gs://${BUCKET_NAME}
# gcloud & terraform config name
export CONFIG=<> 
# personal account
export ACCOUNT=<>  

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

## Deploy via Terraform

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
export DATA_PROJECT=<>
export SA_DISPATCHER_EMAIL=sa-sc-dispatcher@${PROJECT_ID}.iam.gserviceaccount.com
export SA_TAGGER_EMAIL=sa-sc-tagger@${PROJECT_ID}.iam.gserviceaccount.com
export SA_DLP_EMAIL=service-$PROJECT_NUMBER0@dlp-api.iam.gserviceaccount.com

./scripts/prepare_data_projects.sh "${DATA_PROJECT}" $SA_DISPATCHER_EMAIL $SA_TAGGER_EMAIL $SA_DLP_EMAIL
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
 