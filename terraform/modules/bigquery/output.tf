output "bq_view_dlp_fields_findings" {
  value = google_bigquery_table.view_fields_findings.table_id
}