output "sa_dispatcher_email" {
  value = google_service_account.sa_dispatcher.email
}
output "sa_inspector_email" {
  value = google_service_account.sa_inspector.email
}
output "sa_listener_email" {
  value = google_service_account.sa_listener.email
}
output "sa_tagger_email" {
  value = google_service_account.sa_tagger.email
}
output "sa_inspector_tasks_email" {
  value = google_service_account.sa_inspector_tasks.email
}
output "sa_tagger_tasks_email" {
  value = google_service_account.sa_tagger_tasks.email
}
output "sa_scheduler_email" {
  value = google_service_account.sa_scheduler.email
}