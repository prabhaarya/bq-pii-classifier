# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/data_catalog_policy_tag


### Create Taxonomy per domain  ###

resource "google_data_catalog_taxonomy" "domain_taxonomy" {
  count = length(var.taxonomy_parents)
  provider = google-beta
  project = var.project
  region = var.region
  display_name = "${var.taxonomy_name}_${var.taxonomy_parents[count.index]}"
  description = "A collection of policy tags assigned by BQ security classifier for domain '${var.taxonomy_parents[count.index]}'"
  activated_policy_types = [
    "FINE_GRAINED_ACCESS_CONTROL"]
}

# flatten projects and policy tag children to create the same policy tags for each domain
locals {
  child-tags-list = flatten([
  for domain in var.taxonomy_parents : [
  for entry in var.taxonomy_children : {
    domain   = domain
    info_type = lookup(entry, "info_type", "NA")
    policy_tag = lookup(entry, "policy_tag", "NA")
  }
  ]
  ])
}

resource "google_data_catalog_policy_tag" "domain_tags" {
  count = length(local.child-tags-list)
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.domain_taxonomy[floor(count.index/length(var.taxonomy_children))].id
  display_name = lookup(local.child-tags-list[count.index],"policy_tag","NA")
  # FIXME: this is a hack to propagate the project and infotype to the output variable "created_policy_tags". Find an alternative
  description = "${lookup(local.child-tags-list[count.index],"domain","NA")} | ${lookup(local.child-tags-list[count.index],"info_type","NA")}"
}



