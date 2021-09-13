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

# DLP scanning scope
# Optional fields. At least one should be provided among the _INCLUDE configs
# format: project.dataset.table1, project.dataset.table2, etc
variable "tables_include_list" {}
variable "datasets_include_list" {}
variable "projects_include_list" {}
variable "datasets_exclude_list" {}
variable "tables_exclude_list" {}

# for each project in scope, these policy tags will be created in the taxonomy and mapped in BQ configuration with the
# generated policy_tag_id
variable "infoTypeName_policyTagName_map" {}
//Example:
//infoTypeName_policyTagName_map = [
//  {
//    info_type = "EMAIL_ADDRESS",
//    policy_tag = "email"
//  },
//  {
//    info_type = "PHONE_NUMBER",
//    policy_tag = "phone"
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


variable "domain_iam_mapping" {
  description = "Mapping for domains and IAM members to grant required permissions to read sensitive BQ columns belonging to that domain"
}
//Example:
//domain_iam_mapping = {
//  domain1 = ["group:groupname@example.com", "user:username@example.com"],
//  domain2 = ["serviceAccount:sa@example.com"],
//}

variable "dlp_service_account" {}






