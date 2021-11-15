output "inspector_queue_name" {
  value = google_cloud_tasks_queue.inspector_queue.name
}

output "tagger_queue_name" {
  value = google_cloud_tasks_queue.tagger_queue.name
}