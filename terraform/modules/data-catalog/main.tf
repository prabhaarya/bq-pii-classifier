# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/data_catalog_policy_tag


### Create Taxonomy  ###

resource "google_data_catalog_taxonomy" "taxonomy" {
  provider = google-beta
  project = var.project
  region = var.region
  display_name = var.taxonomy_name
  description = "A collection of policy tags assigned by BQ security classifier"
  activated_policy_types = [
    "FINE_GRAINED_ACCESS_CONTROL"]
}

### Create Root Confidential ###

resource "google_data_catalog_policy_tag" "root_confidential" {
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.taxonomy.id
  display_name = "confidential"
  description = "A policy tag category used for high security access"
}


### Create Parents  ###

resource "google_data_catalog_policy_tag" "parents" {
  count = length(var.taxonomy_parents)
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.taxonomy.id
  display_name = "confidential_${var.taxonomy_parents[count.index]}"
  description = "${var.taxonomy_parents[count.index]} project confidential access"
  parent_policy_tag = google_data_catalog_policy_tag.root_confidential.id
}

### Create Children  ###

# flatten projects and policy tag children to create the same policy tags for each project
locals {
  child-tags-list = flatten([
  for project in var.taxonomy_parents : [
  for entry in var.taxonomy_children : {
    project   = project
    info_type = lookup(entry, "info_type", "NA")
    policy_tag = lookup(entry, "policy_tag", "NA")
  }
  ]
  ])
}

resource "google_data_catalog_policy_tag" "children" {
  count = length(local.child-tags-list)
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.taxonomy.id
  display_name = "${lookup(local.child-tags-list[count.index],"policy_tag","NA")}_${lookup(local.child-tags-list[count.index],"project","NA")}"
  # FIXME: this is a hack to propagate the project and infotype to the output variable "created_policy_tags". Find an alternative
  description = "${lookup(local.child-tags-list[count.index],"project","NA")} | ${lookup(local.child-tags-list[count.index],"info_type","NA")}"

  # formula: e.g. given 3 projects and 2 infotypes this leads to 6 children with index mapped to children:(0,1,2) -> parent:1 , children:(3,4,5) -> parent:2
  parent_policy_tag = google_data_catalog_policy_tag.parents[floor(count.index/length(var.taxonomy_children))].id
}





