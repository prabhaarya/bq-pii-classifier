output "bq_view_dlp_fields_findings" {
  value = google_bigquery_table.view_fields_findings.table_id
}

output "config_view_infotype_policytag_map" {
  value = google_bigquery_table.config_view_infotypes_policytags_map
}

output "results_dataset" {
  value = google_bigquery_dataset.results_dataset.dataset_id
}

output "results_table" {
  value = google_bigquery_table.results_table.table_id
}