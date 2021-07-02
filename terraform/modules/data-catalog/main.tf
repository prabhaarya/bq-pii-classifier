# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/data_catalog_policy_tag

resource "google_data_catalog_taxonomy" "taxonomy" {
  provider = google-beta
  project = var.project
  region = var.region
  display_name = var.taxonomy_name
  description = "A collection of policy tags assigned by BQ security classifier"
  activated_policy_types = [
    "FINE_GRAINED_ACCESS_CONTROL"]
}

resource "google_data_catalog_policy_tag" "confidential" {
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.taxonomy.id
  display_name = "Confidential"
  description = "A policy tag category used for high security access"
}
###### Project 1 ########

resource "google_data_catalog_policy_tag" "confidential_project1" {
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.taxonomy.id
  display_name = "confidential_facilities"
  description = "facilities project confidential access"
  parent_policy_tag = google_data_catalog_policy_tag.confidential.id
}

resource "google_data_catalog_policy_tag" "confidential_project1_email" {
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.taxonomy.id
  display_name = "email_facilities"
  description = "An email address"
  parent_policy_tag = google_data_catalog_policy_tag.confidential_project1.id
}

###### Project 2 ########

resource "google_data_catalog_policy_tag" "confidential_project2" {
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.taxonomy.id
  display_name = "confidential_zbooks"
  description = "facilities project zbooks access"
  parent_policy_tag = google_data_catalog_policy_tag.confidential.id
}

resource "google_data_catalog_policy_tag" "confidential_project2_email" {
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.taxonomy.id
  display_name = "email_zbooks"
  description = "An email address"
  parent_policy_tag = google_data_catalog_policy_tag.confidential_project2.id
}


#########################




