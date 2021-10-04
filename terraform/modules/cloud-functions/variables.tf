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
variable "taxonomies" {}
variable "is_dry_run" {
  type = string
  default = "False"
  description = "Applying Policy Tags in the Tagger function (False) or just logging actions (True)"
}

variable "table_scan_limits_json_config" {
  type = string
  description = "JSON config to specify table scan limits intervals"
  // Example
  // "{"limitType": "NUMBER_OF_ROWS", "limits": {"10000": "100","100000": "5000", "1000000": "7000"}}"
  // "{"limitType": "PERCENTAGE_OF_ROWS", "limits": {"10000": "10","100000": "5", "1000000": "1"}}"
}