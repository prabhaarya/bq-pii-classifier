output "taxonomy_id" {
  value = google_data_catalog_taxonomy.taxonomy.id
}


output "confidential_project1" {
  value = google_data_catalog_policy_tag.confidential_project1.id
}

output "email_project1" {
  value = google_data_catalog_policy_tag.confidential_project1_email.id
}


output "confidential_project2" {
  value = google_data_catalog_policy_tag.confidential_project2.id
}

output "email_project2" {
  value = google_data_catalog_policy_tag.confidential_project2_email.id
}