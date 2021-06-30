output "taxonomy_id" {
  value = google_data_catalog_taxonomy.automated_sensitivity.id
}

output "confidential_id" {
  value = google_data_catalog_policy_tag.confidential.id
}

output "email_id" {
  value = google_data_catalog_policy_tag.email.id
}