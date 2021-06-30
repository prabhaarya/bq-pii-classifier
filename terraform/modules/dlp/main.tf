# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/data_loss_prevention_inspect_template

resource "google_data_loss_prevention_inspect_template" "inspection_template" {
  parent = "projects/${var.project}/locations/${var.region}"
  description = "DLP Inspection template used by the BQ security classifier app"
  display_name = "bq_security_classifier_inspection_template"

  inspect_config {

    info_types {
      name = "EMAIL_ADDRESS"
    }

    info_types {
      name = "PHONE_NUMBER"
    }

//    info_types {
//      name = "PERSON_NAME"
//    }

    min_likelihood = "LIKELY"

    custom_info_types {
      info_type {
        name = "MY_CUSTOM_TYPE"
      }

      likelihood = "LIKELY"

      regex {
        pattern = "test*"
      }
    }

    include_quote = false
  }
}