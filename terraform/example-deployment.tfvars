#   Copyright 2021 Google LLC
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

project = "<enter host project id>"
region = "<GCP region to deploy to>"
env = "<dev, tst, prod, poc, etc>"

bigquery_dataset_name = "bq_security_classifier"
dlp_results_table_name = "dlp_results"
tagger_queue = "tagger-queue"
inspector_queue = "inspector-queue"
cf_source_bucket = "<project name>-functions-source"

# DLP scanning scope
# Optional fields. At least one should be provided among the _INCLUDE configs
# format: project.dataset.table1, project.dataset.table2, etc
tables_include_list = ""
datasets_include_list = ""
projects_include_list = "<project 1>, <project 2>, <project 3>"
datasets_exclude_list = ""
tables_exclude_list = ""

# for each domain in scope, these policy tags will be created in a domain-specific taxonomy
# and mapped in BQ configuration with the generated policy_tag_id. Each policy tag will be created
# under a parent node based on the 'classification' field
# INFO_TYPEs configured in the DLP inspection job MUST be mapped here. Otherwise, mapping to policy tag ids will fail
classification_taxonomy = [
  {
    info_type = "EMAIL_ADDRESS",
    info_type_category = "standard",
    policy_tag = "email",
    classification = "P1"
  },
  {
    info_type = "PHONE_NUMBER",
    info_type_category = "standard",
    policy_tag = "phone",
    classification = "P1"
  },
  {
    info_type = "STREET_ADDRESS",
    info_type_category = "standard",
    policy_tag = "street_address",
    classification = "P1"
  },
  {
    info_type = "PERSON_NAME",
    info_type_category = "standard",
    policy_tag = "person_name",
    classification = "P1"
  },
  {
    info_type = "IP_ADDRESS",
    info_type_category = "standard",
    policy_tag = "ip_address",
    classification = "P2"
  },
  {
    info_type = "IMEI_HARDWARE_ID",
    info_type_category = "standard",
    policy_tag = "imei_hardware",
    classification = "P2"
  },
  {
    info_type = "MAC_ADDRESS",
    info_type_category = "standard",
    policy_tag = "mac_address",
    classification = "P2"
  },
  {
    info_type = "URL",
    info_type_category = "standard",
    policy_tag = "url",
    classification = "P2"
  },
  {
    info_type = "GENDER",
    info_type_category = "standard",
    policy_tag = "gender",
    classification = "P2"
  },
  {
    info_type = "UK_NATIONAL_INSURANCE_NUMBER",
    info_type_category = "standard",
    policy_tag = "uk_national_insurance_number",
    classification = "P2"
  },
  {
    info_type = "UK_DRIVERS_LICENSE_NUMBER",
    info_type_category = "standard",
    policy_tag = "uk_drivers_license_number",
    classification = "P2"
  },
  {
    info_type = "CT_PAYMENT_METHOD",
    info_type_category = "custom",
    policy_tag = "payment_method",
    classification = "P2"
  }
]

domain_mapping = [
  {
    project = "<marketing project>",
    domain = "<marketing>"
  },
  {
    project = "<finance project>",
    domain = "<finance>"
  },
  {
    project = "<data lake project>",
    domain = "<dwh>",
    datasets = [
      {
        name = "<marketing dataset>",
        domain = "<marketing>"
      },
      {
        name = "<finance dataset>",
        domain = "<finance>"
      }
    ]
  }
]


// Map with key = "<domain>_<classification>" and value = [list of IAM members]
iam_mapping = {
  marketing_P1 = ["group:marketing-p1-readers@example.com"],
  marketing_P2 = ["group:marketing-p2-readers@example.com", "user:marketing-p2-reader@example.com"],
  finance_P1 = ["group:finance-p1-readers@example.com"],
  finance_P2 = ["group:finance-p2-readers@example.com"],
  dwh_P1 = ["group:dwh-p1-readers@example.com"],
  dwh_P2 = ["group:dwh-p2-readers@example.com"],
}

dlp_service_account = "service-<PROJECT_NUMBER>@dlp-api.iam.gserviceaccount.com"

is_dry_run = "False"

cron_expression = "0 0 * * *"

table_scan_limits_json_config = "{\"limitType\": \"NUMBER_OF_ROWS\", \"limits\": {\"10000\": \"100\",\"100000\": \"5000\", \"1000000\": \"7000\"}}"

terraform_service_account = "sa-terraform@<HOST_PROJECT>.iam.gserviceaccount.com"





