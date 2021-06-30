variable "project" {}
variable "region" {}
variable "sa_dispatcher_email" {}
variable "sa_inspector_email" {}
variable "sa_listener_email" {}
variable "sa_tagger_email" {}
variable "sa_inspector_tasks_email"{}
variable "sa_tagger_tasks_email"{}
variable "sa_scheduler_email"{}
variable "dlp_notifications_topic_fqn"{}
variable "cf_dispatcher" {}
variable "cf_inspector" {}
variable "cf_listener" {}
variable "cf_tagger" {}
variable "inspector_queue_name" {}
variable "tagger_queue_name" {}
variable "bq_results_dataset" {}
variable "bq_results_table" {}
variable "dlp_inspection_template_id" {}
variable "bq_view_dlp_fields_findings" {}