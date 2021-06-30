# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/data_catalog_policy_tag

resource "google_data_catalog_taxonomy" "automated_sensitivity" {
  provider = google-beta
  project = var.project
  region = var.region
  display_name = "automated_sensitivity"
  description = "A collection of policy tags assigned by BQ security classifier"
  activated_policy_types = [
    "FINE_GRAINED_ACCESS_CONTROL"]
}

resource "google_data_catalog_policy_tag" "confidential" {
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.automated_sensitivity.id
  display_name = "Confidential"
  description = "A policy tag category used for high security access"
}

resource "google_data_catalog_policy_tag" "email" {
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.automated_sensitivity.id
  display_name = "email"
  description = "An email address"
  parent_policy_tag = google_data_catalog_policy_tag.confidential.id
}

resource "google_data_catalog_policy_tag" "internal" {
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.automated_sensitivity.id
  display_name = "Internal"
  description = "A policy tag category used for internal access"
}

resource "google_data_catalog_policy_tag" "public" {
  provider = google-beta
  taxonomy = google_data_catalog_taxonomy.automated_sensitivity.id
  display_name = "Public"
  description = "A policy tag category used for public access"
}


