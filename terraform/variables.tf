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
  default = "results"
}

variable "tagger_queue" {
  default = "tf-tagger-queue"
}

variable "inspector_queue" {
  default = "tf-inspector-queue"
}

variable "sa_dispatcher" {
  default = "tf-sa-dispatcher"
}

variable "sa_inspector" {
  default = "tf-sa-inspector"
}

variable "sa_listener" {
  default = "tf-sa-listener"
}

variable "sa_tagger" {
  default = "tf-sa-tagger"
}

variable "sa_inspector_tasks" {
  default = "tf-sa-inspector-tasks"
}

variable "sa_tagger_tasks" {
  default = "tf-sa-tagger-tasks"
}

variable "scheduler_name" {
  default = "tf-scheduler"
}
variable "sa_scheduler" {
  default = "tf-sa-scheduler"
}

variable "dlp_notifications_topic" {
  default = "tf-dlp-notifications"
}

variable "cf_dispatcher" {
  default = "tf-dispatcher"
}
variable "cf_inspector" {
  default = "tf-inspector"
}
variable "cf_listener" {
  default = "tf-listener"
}
variable "cf_tagger" {
  default = "tf-tagger"
}

# DLP scanning scope
# Optional fields. At least one should be provided among the _INCLUDE configs
# format: project.dataset.table1, project.dataset.table2, etc
variable "tables_include_list" {}
variable "datasets_include_list" {}
variable "projects_include_list" {}
variable "datasets_exclude_list" {}
variable "tables_exclude_list" {}

variable "taxonomy_name" {
  default = "confidential"
}

# for each project in scope, these policy tags will be created in the taxonomy and mapped in BQ configuration with the
# generated policy_tag_id
variable "infoTypeName_policyTagName_map" {
  default = [
    {
      info_type = "EMAIL_ADDRESS",
      policy_tag = "email"
    },
    {
      info_type = "PHONE_NUMBER",
      policy_tag = "phone"
    },
    {
      info_type = "ADDRESS",
      policy_tag = "address"
    }
  ]
}

variable "domain_mapping" {}






