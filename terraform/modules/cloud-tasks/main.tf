# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/cloud_tasks_queue

############## Cloud Tasks ######################################

# DISPATCH_RATE is actually the rate at which tokens in the bucket are refreshed. In conditions where there is a relatively steady flow of tasks, this is the equivalent of the rate at which tasks are dispatched.
# MAX_RUNNING is the maximum number of tasks in the queue that can run at once.
# Rate limiting: https://cloud.google.com/tasks/docs/configuring-queues#rate
# Retry params: https://cloud.google.com/tasks/docs/configuring-queues#retry
# API params (better explanation): https://cloud.google.com/tasks/docs/reference/rpc/google.cloud.tasks.v2

resource "google_cloud_tasks_queue" "inspector_queue" {
  project = var.project
  location = var.region
  name = var.inspector_queue

  # DLP Limits:
  # - 600 requests per min --> handle via DISPATCH_RATE
  # - 1000 running jobs --> handle via retries since submitting jobs is Async

  rate_limits {
    max_concurrent_dispatches = 50
    max_dispatches_per_second = 10
  }

  retry_config {
    max_attempts = 5
    max_retry_duration = "7200s"
    max_backoff = "120s"
    min_backoff = "60s"
    max_doublings = 5
  }
}


resource "google_cloud_tasks_queue" "tagger_queue" {
  project = var.project
  location = var.region
  name = var.tagger_queue

  # BigQuery Limits:
  # - 100 concurrent queries: Handle via MAX_RUNNING
  # - 5 operations every 10 seconds (per dataset) for dataset metadata update operations (including patch)
  # - Handle via DISPATCH_RATE with pessimistic setting assuming 1 dataset. Rely on retries as fallback


  rate_limits {
    max_concurrent_dispatches = 100
    max_dispatches_per_second = 0.5
  }

  retry_config {
    max_attempts = 5
    max_retry_duration = "7200s"
    max_backoff = "120s"
    min_backoff = "60s"
    max_doublings = 5
  }
}