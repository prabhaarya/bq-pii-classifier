output "dispatcher_url" {
  value = google_cloudfunctions_function.function_dispatcher.https_trigger_url
}