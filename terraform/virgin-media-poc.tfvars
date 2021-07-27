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

project = "facilities-910444929556"
region = "europe-west2"
bigquery_dataset_name = "bq_security_classifier_4"
dlp_results_table_name = "results"
tagger_queue = "tf-tagger-queue-3"
inspector_queue = "tf-inspector-queue-3"

# DLP scanning scope
# Optional fields. At least one should be provided among the _INCLUDE configs
# format: project.dataset.table1, project.dataset.table2, etc
tables_include_list = ""
datasets_include_list = ""
projects_include_list = "facilities-910444929556, zbooks-910444929556"
datasets_exclude_list = "facilities-910444929556.bq_security_classifier_4"
tables_exclude_list = ""

# for each project in scope, these policy tags will be created in the taxonomy and mapped in BQ configuration with the
# generated policy_tag_id
# INFO_TYPEs configured in the DLP inspection job MUST be mapped here. Otherwise, mapping to policy tag ids will fail
infoTypeName_policyTagName_map = [
  {
    info_type = "EMAIL_ADDRESS",
    policy_tag = "email"
  },
  {
    info_type = "PHONE_NUMBER",
    policy_tag = "phone"
  },
  {
    info_type = "STREET_ADDRESS",
    policy_tag = "street_address"
  },
  {
    info_type = "PERSON_NAME",
    policy_tag = "person_name"
  },
  {
    info_type = "IP_ADDRESS",
    policy_tag = "ip_address"
  },
  {
    info_type = "IMEI_HARDWARE_ID",
    policy_tag = "imei_hardware"
  },
  {
    info_type = "MAC_ADDRESS",
    policy_tag = "mac_address"
  },
  {
    info_type = "URL",
    policy_tag = "url"
  },
  {
    info_type = "GENDER",
    policy_tag = "gender"
  },
  {
    info_type = "UK_NATIONAL_INSURANCE_NUMBER",
    policy_tag = "uk_national_insurance_number"
  },
  {
    info_type = "UK_DRIVERS_LICENSE_NUMBER",
    policy_tag = "uk_drivers_license_number"
  },
  {
    info_type = "CT_DPA_VERIFICATION",
    policy_tag = "dpa_verification"
  },
  {
    info_type = "CT_PAYMENT_METHOD",
    policy_tag = "payment_method"
  }
]

domain_mapping = [
  {
    project = "zbooks-910444929556",
    domain = "marketing"
  },
  {
    project = "facilities-910444929556",
    domain = "dwh",
    datasets = [
      {
        name = "ds_domain_marketing",
        domain = "marketing"
      },
      {
        name = "ds_domain_finance",
        domain = "finance"
      }
    ]
  }
]







