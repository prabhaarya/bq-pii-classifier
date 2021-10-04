
# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/cloud_scheduler_job


resource "google_cloud_scheduler_job" "scheduler_job" {
  name             = var.scheduler_name
  description      = "CRON job to trigger BQ Security Classifier"
  schedule         = var.cron_expression

  retry_config {
    retry_count = 1
  }

  http_target {
    http_method = "POST"
    uri         = var.target_uri

    //body        = base64encode("{\"tablesInclude\":\"${var.tables_include_list}\"}")
    body = base64encode(jsonencode({
      tablesInclude = var.tables_include_list
      datasetsInclude = var.datasets_include_list
      projectsInclude = var.projects_include_list
      datasetsExclude = var.datasets_exclude_list
      tablesExclude = var.tables_exclude_list

    }))

    oidc_token {
      service_account_email = var.service_account_email
    }
  }


}




