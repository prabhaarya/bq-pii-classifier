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

provider "google" {
  project = var.project
  region  = var.region
}

module "data-catalog" {
  source = "./modules/data-catalog"
  project = var.project
  region = var.region
}

module "cloud_logging" {
  source = "./modules/cloud-logging"

  dataset = var.dlp_results_dataset_name
  project = var.project
}

module "bigquery" {
  source = "./modules/bigquery"
  project = var.project
  region = var.region
  dlp_results_dataset_name = var.dlp_results_dataset_name
  dlp_results_table_name = var.dlp_results_table_name
  logging_sink_sa = module.cloud_logging.service_account

  # infoType-Policytag mapping
  taxonomy_email_id = module.data-catalog.email_id
}

module "cloud_tasks" {
  source = "./modules/cloud-tasks"
  project = var.project
  region = var.region
  inspector_queue = var.inspector_queue
  tagger_queue = var.tagger_queue
}

module "iam" {
  source = "./modules/iam"
  project = var.project
  region = var.region
  sa_dispatcher = var.sa_dispatcher
  sa_inspector = var.sa_inspector
  sa_listener = var.sa_listener
  sa_tagger = var.sa_tagger
  sa_inspector_tasks = var.sa_inspector_tasks
  sa_tagger_tasks = var.sa_tagger_tasks
  sa_scheduler = var.sa_scheduler
}

module "cloud_scheduler" {
  source = "./modules/cloud-scheduler"
  project = var.project
  scheduler_name = var.scheduler_name
  target_uri = module.cloud_functions.dispatcher_url
  service_account_email = module.iam.sa_scheduler_email

  tables_include_list = var.tables_include_list
  datasets_include_list = var.datasets_include_list
  projects_include_list = var.projects_include_list
  datasets_exclude_list = var.datasets_exclude_list
  tables_exclude_list = var.tables_exclude_list
}

module "pubsub" {
  source = "./modules/pubsub"
  project = var.project
  dlp_notifications_topic = var.dlp_notifications_topic
}

module "cloud_functions" {
  source = "./modules/cloud-functions"
  project = var.project
  region = var.region
  sa_dispatcher_email = module.iam.sa_dispatcher_email
  sa_inspector_email = module.iam.sa_inspector_email
  sa_listener_email = module.iam.sa_listener_email
  sa_tagger_email = module.iam.sa_tagger_email
  sa_inspector_tasks_email = module.iam.sa_inspector_tasks_email
  sa_tagger_tasks_email = module.iam.sa_tagger_tasks_email
  sa_scheduler_email = module.iam.sa_scheduler_email
  dlp_notifications_topic_fqn = module.pubsub.dlp_notifications_topic_fqn
  cf_dispatcher = var.cf_dispatcher
  cf_inspector = var.cf_inspector
  cf_listener = var.cf_listener
  cf_tagger = var.cf_tagger
  bq_results_dataset = var.dlp_results_dataset_name
  bq_results_table = var.dlp_results_table_name
  inspector_queue_name = var.inspector_queue
  tagger_queue_name = var.tagger_queue
  dlp_inspection_template_id = module.dlp.template_id
  bq_view_dlp_fields_findings = module.bigquery.bq_view_dlp_fields_findings
}

module "dlp" {
  source = "./modules/dlp"
  project = var.project
  region = var.region
}

