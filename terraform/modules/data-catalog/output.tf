
output "created_policy_tags" {
  value = [for entry in google_data_catalog_policy_tag.domain_tags: {
    policy_tag_id = entry.id
    domain = trim(element(split("|", entry.description),0)," ")
    info_type = trim(element(split("|", entry.description),1), " ")
  }]
}

