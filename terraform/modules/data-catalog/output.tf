output "taxonomy_id" {
  value = google_data_catalog_taxonomy.taxonomy.id
}


output "created_policy_tags" {
  value = [for entry in google_data_catalog_policy_tag.children: {
    policy_tag_id = entry.id
    project = trim(element(split("|", entry.description),0)," ")
    info_type = trim(element(split("|", entry.description),1), " ")
  }]
}