# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/data_loss_prevention_inspect_template

resource "google_data_loss_prevention_inspect_template" "inspection_template" {
  parent = "projects/${var.project}/locations/${var.region}"
  description = "DLP Inspection template used by the BQ security classifier app"
  display_name = "bq_security_classifier_inspection_template"

  # Info Types configured here must be mapped in the infoTypeName_policyTagName_map variable
  # passed to the main module, otherwise mapping to policy tags will fail.

  inspect_config {

    ### STANDARD INFOTYPES

    info_types {
      name = "EMAIL_ADDRESS"
    }

    info_types {
      name = "PHONE_NUMBER"
    }

    info_types {
      name = "STREET_ADDRESS"
    }

    info_types {
      name = "PERSON_NAME"
    }

    info_types {
      name = "IMEI_HARDWARE_ID"
    }

    info_types {
      name = "IP_ADDRESS"
    }

    info_types {
      name = "MAC_ADDRESS"
    }

    info_types {
      name = "URL"
    }

    info_types {
      name = "GENDER"
    }

    info_types {
      name = "UK_NATIONAL_INSURANCE_NUMBER"
    }

    info_types {
      name = "UK_DRIVERS_LICENSE_NUMBER"
    }

    min_likelihood = "LIKELY"

    ### CUSTOM INFOTYPES

    custom_info_types {
      info_type {
        name = "CT_DPA_VERIFICATION"
      }

      likelihood = "LIKELY"

      dictionary {
        word_list {
          words = ["where the child was born",
            "navy nickname",
            "date of birth",
            "airforce nickname",
            "email address",
            "previous company",
            "mothers name",
            "phone number",
            "starsign"]
        }
      }
    }

    custom_info_types {
      info_type {
        name = "CT_PAYMENT_METHOD"
      }

      likelihood = "LIKELY"

      dictionary {
        word_list {
          words = ["Debit Card", "Credit Card"]
        }
      }
    }

    #### RULE SETS


    rule_set {
      info_types {
        name = "EMAIL_ADDRESS"
      }
      rules {
        exclusion_rule {
          regex {
            pattern = ".+@virginmedia.co.uk"
          }
          matching_type = "MATCHING_TYPE_FULL_MATCH"
        }
      }
    }

    # to include findings text in the results table (e.g. user@domain.com -> EMAIL_ADDRESS)
    include_quote = false
  }
}