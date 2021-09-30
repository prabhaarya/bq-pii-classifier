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
  region = var.region
  impersonate_service_account = var.terraform_service_account
}

provider "google-beta" {
  project = var.project
  region = var.region
  impersonate_service_account = var.terraform_service_account
}

# Enable APIS

resource "google_project_service" "enable_service_usage_api" {
  project = var.project
  service = "serviceusage.googleapis.com"

  disable_dependent_services = true
  disable_on_destroy         = false
}

resource "google_project_service" "enable_cloud_functions" {
  project = var.project
  service = "cloudfunctions.googleapis.com"

  disable_dependent_services = true
  disable_on_destroy         = false
}

# Enable Cloud Build API
resource "google_project_service" "enable_cloud_build" {
  project = var.project
  service = "cloudbuild.googleapis.com"

  disable_dependent_services = true
  disable_on_destroy         = false
}

# Enable Cloud Scheduler API
resource "google_project_service" "enable_appengine" {
  project = var.project
  service = "appengine.googleapis.com"

  disable_dependent_services = true
  disable_on_destroy         = false
}

# Enable Cloud Scheduler API
resource "google_project_service" "enable_cloud_scheduler" {
  project = var.project
  service = "cloudscheduler.googleapis.com"

  disable_dependent_services = true
  disable_on_destroy         = false
}

locals {

  project_and_domains = distinct([
  for entry in var.domain_mapping : {
    project = lookup(entry, "project"),
    domain = lookup(entry, "domain")
  }
  ])

  # Only projects with configured domains
  project_and_domains_filtered = [for entry in local.project_and_domains: entry if lookup(entry, "domain") != ""]

  datasets_and_domains = distinct(flatten([
  for entry in var.domain_mapping : [
  for dataset in lookup(entry, "datasets", []) : {
    project = lookup(entry, "project"),
    dataset = lookup(dataset, "name"),
    domain = lookup(dataset, "domain")
  }
  ]]))

  # Only datasets with configured domains
  datasets_and_domains_filtered = [for entry in local.datasets_and_domains: entry if lookup(entry, "domain") != ""]

  # Get distinct domains set on project entries
  project_domains = distinct([
  for entry in local.project_and_domains_filtered : lookup(entry, "domain")
  ])

  # Get distinct domains set on dataset level
  dataset_domains = distinct([
  for entry in local.datasets_and_domains_filtered : lookup(entry, "domain")
  ])

  // Concat project and dataset domains and filter out empty strings
  domains = distinct(concat(local.project_domains, local.dataset_domains))

  taxonomies = join(",", [for taxonomy in module.data-catalog.created_taxonomies: taxonomy.name])
}

module "data-catalog" {
  source = "./modules/data-catalog"
  project = var.project
  region = var.region
  taxonomy_parents = local.domains
  taxonomy_children = var.infoTypeName_policyTagName_map
}

module "cloud_logging" {
  source = "./modules/cloud-logging"

  dataset = var.bigquery_dataset_name
  project = var.project
}

module "bigquery" {
  source = "./modules/bigquery"
  project = var.project
  region = var.region
  dataset = var.bigquery_dataset_name
  dlp_results_table_name = var.dlp_results_table_name
  logging_sink_sa = module.cloud_logging.service_account

  # Data for config views
  created_policy_tags = module.data-catalog.created_policy_tags
  dataset_domains_mapping = local.datasets_and_domains_filtered
  projects_domains_mapping = local.project_and_domains_filtered
}

module "cloud_tasks" {
  source = "./modules/cloud-tasks"
  project = var.project
  region = var.region
  inspector_queue = var.inspector_queue
  tagger_queue = var.tagger_queue

  depends_on = [google_project_service.enable_appengine]
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
  taxonomies = module.data-catalog.created_taxonomies
  domain_iam_mapping = var.domain_iam_mapping
  dlp_service_account = var.dlp_service_account
}

module "cloud_scheduler" {
  source = "./modules/cloud-scheduler"
  project = var.project
  region = var.region
  scheduler_name = var.scheduler_name
  target_uri = module.cloud_functions.dispatcher_url
  service_account_email = module.iam.sa_scheduler_email

  tables_include_list = var.tables_include_list
  datasets_include_list = var.datasets_include_list
  projects_include_list = var.projects_include_list
  datasets_exclude_list = var.datasets_exclude_list
  tables_exclude_list = var.tables_exclude_list

  depends_on = [google_project_service.enable_appengine]
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
  bq_results_dataset = var.bigquery_dataset_name
  bq_results_table = var.dlp_results_table_name
  inspector_queue_name = var.inspector_queue
  tagger_queue_name = var.tagger_queue
  dlp_inspection_template_id = module.dlp.template_id
  bq_view_dlp_fields_findings = module.bigquery.bq_view_dlp_fields_findings
  taxonomies = local.taxonomies
}

module "dlp" {
  source = "./modules/dlp"
  project = var.project
  region = var.region
}


