# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/google_service_account
# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/google_service_account_iam
# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/google_project_iam#google_project_iam_member


############## Service Accounts ######################################

resource "google_service_account" "sa_dispatcher" {
  project = var.project
  account_id = var.sa_dispatcher
  display_name = "CF runtime SA for functions.Dispatcher"
}

resource "google_service_account" "sa_inspector" {
  project = var.project
  account_id = var.sa_inspector
  display_name = "CF runtime SA for Inspector function"
}

resource "google_service_account" "sa_listener" {
  project = var.project
  account_id = var.sa_listener
  display_name = "CF runtime SA for Listener function"
}

resource "google_service_account" "sa_tagger" {
  project = var.project
  account_id = var.sa_tagger
  display_name = "CF runtime SA for Tagger function"
}

resource "google_service_account" "sa_inspector_tasks" {
  project = var.project
  account_id = var.sa_inspector_tasks
  display_name = "To authorize Cloud Tasks HTTP requests to Inspector CF"
}

resource "google_service_account" "sa_tagger_tasks" {
  project = var.project
  account_id = var.sa_tagger_tasks
  display_name = "To authorize Cloud Tasks HTTP requests to Tagger CF"
}

resource "google_service_account" "sa_scheduler" {
  project = var.project
  account_id = var.sa_scheduler
  display_name = "Cloud Scheduler client SA for BQ Security Classifier solution"
}

############## Service Accounts Access ################################

# Use google_project_iam_member because it's Non-authoritative.
# It Updates the IAM policy to grant a role to a new member.
# Other members for the role for the project are preserved.

#### Dispatcher SA Permissions ###

# Grant sa_dispatcher access to list datasets and tables
resource "google_project_iam_member" "sa_dispatcher_bq_metadata_viewer" {
  project = var.project
  role = "roles/bigquery.metadataViewer"
  member = "serviceAccount:${google_service_account.sa_dispatcher.email}"
}

# Grant sa_dispatcher serviceAccountUser role to impersonate sa_inspector_tasks while submitting tasks
resource "google_service_account_iam_member" "sa_dispatcher_account_user_sa_inspector_tasks" {
  service_account_id = google_service_account.sa_inspector_tasks.name
  role = "roles/iam.serviceAccountUser"
  member = "serviceAccount:${google_service_account.sa_dispatcher.email}"
}

// TODO: grant access to inspector queue only instead of project-level role
resource "google_project_iam_member" "sa_dispatcher_tasks_enqueuer" {
  project = var.project
  role = "roles/cloudtasks.enqueuer"
  member = "serviceAccount:${google_service_account.sa_dispatcher.email}"
}

#### Inspector SA Permissions ###

# Grant sa_inspector access to view bq table metadata (e.g. rows count)
resource "google_project_iam_member" "sa_inspector_bq_metadata_viewer" {
  project = var.project
  role = "roles/bigquery.metadataViewer"
  member = "serviceAccount:${google_service_account.sa_inspector.email}"
}

# Grant sa_inspector access to list dlp jobs
resource "google_project_iam_member" "sa_inspector_dlp_jobs_editor" {
  project = var.project
  role = "roles/dlp.jobsEditor"
  member = "serviceAccount:${google_service_account.sa_inspector.email}"
}

# Grant sa_inspector access to read dlp templates
resource "google_project_iam_member" "sa_inspector_dlp_template_reader" {
  project = var.project
  role = "roles/dlp.inspectTemplatesReader"
  member = "serviceAccount:${google_service_account.sa_inspector.email}"
}


#### Listener SA Permissions ###

# Grant sa_listener serviceAccountUser role to impersonate sa_tagger_tasks while submitting tasks
resource "google_service_account_iam_member" "sa_listener_account_user_sa_tagger_tasks" {
  service_account_id = google_service_account.sa_tagger_tasks.name
  role = "roles/iam.serviceAccountUser"
  member = "serviceAccount:${google_service_account.sa_listener.email}"
}

// TODO: grant access to tagger queue only instead of project-level role
resource "google_project_iam_member" "sa_listener_tasks_enqueuer" {
  project = var.project
  role = "roles/cloudtasks.enqueuer"
  member = "serviceAccount:${google_service_account.sa_listener.email}"
}


#### Tagger SA Permissions ###

resource "google_project_iam_custom_role" "tagger-role" {
  project = var.project
  role_id = "tf_tagger_role"
  title = "tf_tagger_role"
  description = "Used to grant permissions to sa_tagger"
  permissions = [
    "bigquery.tables.setCategory",
    "dlp.jobs.get",
    "datacatalog.taxonomies.get"]
}

resource "google_project_iam_member" "sa_tagger_role" {
  project = var.project
  role = google_project_iam_custom_role.tagger-role.name
  member = "serviceAccount:${google_service_account.sa_tagger.email}"
}

# TODO: can we use dataViewer instead?
resource "google_project_iam_member" "sa_tagger_bq_editor" {
  project = var.project
  role = "roles/bigquery.dataEditor"
  member = "serviceAccount:${google_service_account.sa_tagger.email}"
}

# to submit query jobs
resource "google_project_iam_member" "sa_tagger_bq_job_user" {
  project = var.project
  role = "roles/bigquery.jobUser"
  member = "serviceAccount:${google_service_account.sa_tagger.email}"
}

############## DLP Service Account ################################################

resource "google_project_iam_member" "dlp_sa_binding" {
  project = var.project
  role = "roles/datacatalog.categoryFineGrainedReader"
  member = "serviceAccount:${var.dlp_service_account}"
}

############## Data Catalog Taxonomies Permissions ################################

# Create a flat list of (member, taxonomies) to loop on based on the provided mapping
locals {
  taxonomies_members = flatten([
  for taxonomy in var.taxonomies : [
  for member in lookup(var.domain_iam_mapping, taxonomy.display_name) : {
    member   = member
    taxonomy = taxonomy.name
  }
  ]
  ])
}

# Grant permissions for every member to the taxonomy based on the provided mapping.
resource "google_data_catalog_taxonomy_iam_member" "taxonomy_reader" {
  provider = google-beta
  # Create IAM binding for each domain/taxonomy
  count = length(local.taxonomies_members)
  taxonomy = local.taxonomies_members[count.index].taxonomy
  role = "roles/datacatalog.categoryFineGrainedReader"
  # lookup the members list by the domain name
  member = local.taxonomies_members[count.index].member
}


