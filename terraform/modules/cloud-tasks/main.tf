############## Cloud Tasks ######################################

resource "google_cloud_tasks_queue" "inspector_queue" {
  project = var.project
  location = var.region
  name = var.inspector_queue

  rate_limits {
    max_concurrent_dispatches = 50
    max_dispatches_per_second = 50
  }

  retry_config {
    max_attempts = 1
    max_retry_duration = "4s"
    max_backoff = "3s"
    min_backoff = "2s"
    max_doublings = 1
  }
}

# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/cloud_tasks_queue

resource "google_cloud_tasks_queue" "tagger_queue" {
  project = var.project
  location = var.region
  name = var.tagger_queue

  rate_limits {
    max_concurrent_dispatches = 50
    max_dispatches_per_second = 50
  }

  retry_config {
    max_attempts = 1
    max_retry_duration = "4s"
    max_backoff = "3s"
    min_backoff = "2s"
    max_doublings = 1
  }
}