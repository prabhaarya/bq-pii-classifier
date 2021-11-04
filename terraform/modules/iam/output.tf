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

output "local_parent_tags_with_members_list" {
  value = local.parent_tags_with_members_list
}

output "local_iam_members_list" {
  value = local.iam_members_list
}



//output "debug_policy_tag_readers" {
//  value = google_data_catalog_policy_tag_iam_member.policy_tag_reader
//}