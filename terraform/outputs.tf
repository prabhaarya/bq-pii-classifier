output "created_policy_tags" {
  value = module.data-catalog.created_policy_tags
}

output "test_view" {
  value = module.bigquery.config_view_infotype_policytag_map
}