
resource "google_logging_project_sink" "bigquery-logging-sink" {
  name = var.log_sink_name
  destination = "bigquery.googleapis.com/projects/${var.project}/datasets/${var.dataset}"
  filter = "resource.type=cloud_function resource.labels.region=${var.region} jsonPayload.message:[bq-security-classifier]"
  # Use a unique writer (creates a unique service account used for writing)
  unique_writer_identity = true
  bigquery_options {
    use_partitioned_tables = true
  }
}