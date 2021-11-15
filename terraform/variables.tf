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

variable "project" {}

variable "region" {}

variable "env" {}

variable "bigquery_dataset_name" {
  default = "bq_security_classifier"
}

variable "dlp_results_table_name" {
  default = "dlp_results"
}

variable "tagger_queue" {
  default = "sc-tagger-queue"
}

variable "inspector_queue" {
  default = "sc-inspector-queue"
}

variable "sa_dispatcher" {
  default = "sa-sc-dispatcher"
}

variable "sa_inspector" {
  default = "sa-sc-inspector"
}

variable "sa_listener" {
  default = "sa-sc-listener"
}

variable "sa_tagger" {
  default = "sa-sc-tagger"
}

variable "sa_inspector_tasks" {
  default = "sa-sc-inspector-tasks"
}

variable "sa_tagger_tasks" {
  default = "sa-sc-tagger-tasks"
}

variable "scheduler_name" {
  default = "sc-scheduler"
}
variable "sa_scheduler" {
  default = "sa-sc-scheduler"
}

variable "dlp_notifications_topic" {
  default = "sc-dlp-notifications"
}

variable "cf_dispatcher" {
  default = "sc-dispatcher"
}
variable "cf_inspector" {
  default = "sc-inspector"
}
variable "cf_listener" {
  default = "sc-listener"
}
variable "cf_tagger" {
  default = "sc-tagger"
}

variable "tagger_role" {
  default = "tf_tagger_role"
}

variable "log_sink_name" {
  default = "bigquery-logging-sink"
}

variable "cf_source_bucket" {
  default = "functions-source"
}




# DLP scanning scope
# Optional fields. At least one should be provided among the _INCLUDE configs
# format: project.dataset.table1, project.dataset.table2, etc
variable "tables_include_list" {}
variable "datasets_include_list" {}
variable "projects_include_list" {}
variable "datasets_exclude_list" {}
variable "tables_exclude_list" {}

# for each domain in scope, these policy tags will be created in a domain-specific taxonomy
# and mapped in BQ configuration with the generated policy_tag_id. Each policy tag will be created
# under a parent node based on the 'classification' field
# INFO_TYPEs configured in the DLP inspection job MUST be mapped here. Otherwise, mapping to policy tag ids will fail
variable "classification_taxonomy" {}
//Example:
//classification_taxonomy = [
//  {
//    info_type = "EMAIL_ADDRESS",
//    policy_tag = "email",
//    classification = "P1"
//  },
//  {
//    info_type = "PHONE_NUMBER",
//    policy_tag = "phone"
//    classification = "P2"
//  }
//  ]

variable "domain_mapping" {
  description = "Mapping between domains and GCP projects or BQ Datasets. Dataset-level mapping will overwrite project-level mapping for a given project."
}
// Example:
//domain_mapping = [
//  {
//    project = "marketing-project",
//    domain = "marketing"
//  },
//  {
//    project = "dwh-project",
//    domain = "dwh",
//    datasets = [
//      {
//        name = "marketing_dataset",
//        domain = "marketing"
//      },
//      {
//        name = "finance_dataset",
//        domain = "finance"
//      }
//    ]
//  }
//]


variable "iam_mapping" {
  description = "List of mappings between domains/classification and IAM members to grant required permissions to read sensitive BQ columns belonging to that domain/classification"
}
//Example:
//iam_mapping = {
//  marketing_P1 = ["user:marketing-p1-reader@wadie.joonix.net"],
//  marketing_P2 = ["user:marketing-p2-reader@wadie.joonix.net"],
//  finance_P1 = ["user:finance-p1-reader@wadie.joonix.net"],
//  finance_P2 = ["user:finance-p2-reader@wadie.joonix.net"],
//  dwh_P1 = ["user:dwh-p1-reader@wadie.joonix.net"],
//  dwh_P2 = ["user:dwh-p2-reader@wadie.joonix.net"],
//}

variable "dlp_service_account" {
  description = "service account email for DLP to grant permissions to via Terraform"
}

variable "terraform_service_account" {
  description = "service account used by terraform to deploy to GCP"
}

variable "is_dry_run" {
  type = string
  default = "False"
  description = "Applying Policy Tags in the Tagger function (False) or just logging actions (True)"
}

variable "cron_expression" {
  type = string
  description = "Cron expression used by the Cloud Scheduler to run a full scan"
}

variable "table_scan_limits_json_config" {
  type = string
  description = "JSON config to specify table scan limits intervals"
  // Example
  // "{"limitType": "NUMBER_OF_ROWS", "limits": {"10000": "100","100000": "5000", "1000000": "7000"}}"
  // "{"limitType": "PERCENTAGE_OF_ROWS", "limits": {"10000": "10","100000": "5", "1000000": "1"}}"
}






