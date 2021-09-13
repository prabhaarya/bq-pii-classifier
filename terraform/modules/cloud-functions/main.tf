

# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/cloudfunctions_function


locals {
  timestamp = formatdate("YYMMDDhhmmss", timestamp())
  functions_dir = abspath("../functions/bq_security_classifier_functions/")
}

###### Dispatcher CF ###############

# Compress source code
data "archive_file" "source" {
  type        = "zip"
  source_dir  = local.functions_dir
  output_path = "/tmp/${local.timestamp}.zip"
}

# Create bucket that will host the source code
resource "google_storage_bucket" "source_bucket" {
  name = "${var.project}-functions-source"
  location = var.region
  uniform_bucket_level_access = true
}

# Add source code zip to bucket
resource "google_storage_bucket_object" "zip" {
  # Append file MD5 to force bucket to be recreated
  name   = "source.zip#${data.archive_file.source.output_md5}"
  bucket = google_storage_bucket.source_bucket.name
  source = data.archive_file.source.output_path
}

# Create Cloud Function
resource "google_cloudfunctions_function" "function_dispatcher" {
  name    = var.cf_dispatcher
  runtime = "java11"
  available_memory_mb   = 512
  source_archive_bucket = google_storage_bucket.source_bucket.name
  source_archive_object = google_storage_bucket_object.zip.name
  trigger_http          = true
  entry_point           = "com.google.cloud.pso.bq_security_classifier.functions.dispatcher.Dispatcher"
  timeout = 540
  service_account_email = var.sa_dispatcher_email
  #environment_variables = yamldecode(file("${local.dispatcher_dir}/config.yaml"))
  environment_variables = {
    PROJECT_ID = var.project
    REGION_ID = var.region
    QUEUE_ID = var.inspector_queue_name
    SA_EMAIL = var.sa_inspector_tasks_email
    HTTP_ENDPOINT = google_cloudfunctions_function.function_inspector.https_trigger_url
  }

}


# SA_SCHEDULER must be able to invoke CF bq_security_classifier_01_dispatcher
resource "google_cloudfunctions_function_iam_member" "dispatcher_invoker" {
project        = google_cloudfunctions_function.function_dispatcher.project
region         = google_cloudfunctions_function.function_dispatcher.region
cloud_function = google_cloudfunctions_function.function_dispatcher.name
role   = "roles/cloudfunctions.invoker"
member = "serviceAccount:${var.sa_scheduler_email}"
}


###### Inspector CF ###############

# Create Cloud Function
resource "google_cloudfunctions_function" "function_inspector" {
  name    = var.cf_inspector
  runtime = "java11"
  available_memory_mb   = 512
  source_archive_bucket = google_storage_bucket.source_bucket.name
  source_archive_object = google_storage_bucket_object.zip.name
  trigger_http          = true
  entry_point           = "com.google.cloud.pso.bq_security_classifier.functions.inspector.Inspector"
  timeout = 540
  service_account_email = var.sa_inspector_email
  #environment_variables = yamldecode(file("${local.inspector_dir}/config.yaml"))
  environment_variables = {
    PROJECT_ID = var.project
    REGION_ID = var.region
    DLP_INSPECTION_TEMPLATE_ID = var.dlp_inspection_template_id
    MIN_LIKELIHOOD = "LIKELY"
    MAX_FINDINGS_PER_ITEM = "50"
    #  Select value from  SAMPLE_METHOD_UNSPECIFIED=0  TOP=1  RANDOM_START=2
    SAMPLING_METHOD = "2"
    ROWS_LIMIT = "1000"
    DLP_NOTIFICATION_TOPIC = var.dlp_notifications_topic_fqn
    BQ_RESULTS_DATASET = var.bq_results_dataset
    BQ_RESULTS_TABLE = var.bq_results_table
  }
}

# SA_INSPECTOR_TASKS must be able to invoke CF bq_security_classifier_02_inspector
resource "google_cloudfunctions_function_iam_member" "inspector_invoker" {
  project        = google_cloudfunctions_function.function_inspector.project
  region         = google_cloudfunctions_function.function_inspector.region
  cloud_function = google_cloudfunctions_function.function_inspector.name
  role   = "roles/cloudfunctions.invoker"
  member = "serviceAccount:${var.sa_inspector_tasks_email}"
}


###### Listener CF ###############

# Create Cloud Function
resource "google_cloudfunctions_function" "function_listener" {
  name    = var.cf_listener
  runtime = "java11"
  available_memory_mb   = 256
  source_archive_bucket = google_storage_bucket.source_bucket.name
  source_archive_object = google_storage_bucket_object.zip.name
  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource = var.dlp_notifications_topic_fqn
  }
  entry_point           = "com.google.cloud.pso.bq_security_classifier.functions.listener.Listener"
  timeout = 540
  service_account_email = var.sa_listener_email
  #environment_variables = yamldecode(file("${local.listener_dir}/config.yaml"))
  environment_variables = {
    PROJECT_ID = var.project
    REGION_ID = var.region
    QUEUE_ID = var.tagger_queue_name
    SA_EMAIL = var.sa_tagger_tasks_email
    HTTP_ENDPOINT = google_cloudfunctions_function.function_tagger.https_trigger_url
  }
}

###### Tagger CF ###############


# Create Cloud Function
resource "google_cloudfunctions_function" "function_tagger" {
  name    = var.cf_tagger
  runtime = "java11"
  available_memory_mb   = 512
  source_archive_bucket = google_storage_bucket.source_bucket.name
  source_archive_object = google_storage_bucket_object.zip.name
  trigger_http          = true
  entry_point           = "com.google.cloud.pso.bq_security_classifier.functions.tagger.Tagger"
  timeout = 540
  service_account_email = var.sa_tagger_email
  #environment_variables = yamldecode(file("${local.tagger_dir}/config.yaml"))
  environment_variables = {
    PROJECT_ID = var.project
    DATASET_ID = var.bq_results_dataset
    BQ_VIEW_FIELDS_FINDINGS = var.bq_view_dlp_fields_findings
    DLP_RESULTS_TABLE = var.bq_results_table
    TAXONOMIES = var.taxonomies
  }
}


# SA_TAGGER_TASKS must be able to invoke CF bq_security_classifier_04_tagger
resource "google_cloudfunctions_function_iam_member" "tagger_invoker" {
  project        = google_cloudfunctions_function.function_tagger.project
  region         = google_cloudfunctions_function.function_tagger.region
  cloud_function = google_cloudfunctions_function.function_tagger.name
  role   = "roles/cloudfunctions.invoker"
  member = "serviceAccount:${var.sa_tagger_tasks_email}"
}






