
# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/pubsub_topic

resource "google_pubsub_topic" "dlp_notifications_topic" {
  project = var.project
  name = var.dlp_notifications_topic
}