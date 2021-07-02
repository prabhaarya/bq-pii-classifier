
# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/bigquery_table
# https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/bigquery_dataset

resource "google_bigquery_dataset" "results_dataset" {
  project = var.project
  location = var.region
  dataset_id = var.dlp_results_dataset_name
  description = "To store DLP results from BQ Security Classifier app"
}

resource "google_bigquery_table" "results_table" {
  project = var.project
  dataset_id = google_bigquery_dataset.results_dataset.dataset_id
  table_id = var.dlp_results_table_name

  time_partitioning {
    type = "DAY"
    expiration_ms = 604800000
  }

  clustering = [
    "job_name"]

  schema = file("modules/bigquery/schema/dlp_results.json")

  #TODO:  Allow destroying the table. Set to true for production use
  deletion_protection=false
}

resource "google_bigquery_table" "logging_table" {
  project = var.project
  dataset_id = google_bigquery_dataset.results_dataset.dataset_id
  # don't change the name so that cloud logging can find it
  table_id = "cloudfunctions_googleapis_com_cloud_functions"

  time_partitioning {
    type = "DAY"
    expiration_ms = 604800000
  }

  schema = file("modules/bigquery/schema/cloudfunctions_googleapis_com_cloud_functions.json")

  #TODO:  Allow destroying the table. Set to true for production use
  deletion_protection=false
}

resource "google_bigquery_table" "logging_view_app_log" {
  dataset_id = google_bigquery_dataset.results_dataset.dataset_id
  table_id   = "v_log_application"

  #TODO:  Allow destroying the table. Set to true for production use
  deletion_protection=false

  view {
    use_legacy_sql = false
    query = <<SQL
WITH l1 AS
(
SELECT
l.jsonPayload,
SPLIT(l.jsonPayload.message, "|") AS splits
FROM `${google_bigquery_table.logging_table.project}.${google_bigquery_table.logging_table.dataset_id}.${google_bigquery_table.logging_table.table_id}` l
)

SELECT
TRIM(l1.splits[OFFSET(0)]) AS application_name,
TRIM(l1.splits[OFFSET(1)]) AS log_name,
TRIM(l1.splits[OFFSET(2)]) AS tracker,
CONCAT("R-", SPLIT(l1.splits[OFFSET(2)], "-")[OFFSET(1)]) AS run_id,
l1.jsonPayload,
l1.splits
FROM l1
SQL
  }
}

resource "google_bigquery_table" "logging_view_tag_history" {
  dataset_id = google_bigquery_dataset.results_dataset.dataset_id
  table_id   = "v_log_tag_history"

  #TODO:  Allow destroying the table. Set to true for production use
  deletion_protection=false

  view {
    use_legacy_sql = false
    query = <<SQL
SELECT
TIMESTAMP_MILLIS(CAST(SUBSTR(run_id, 3) AS INT64)) AS start_time,
run_id,
tracker,
TRIM(splits[OFFSET(3)]) AS project_id,
TRIM(splits[OFFSET(4)]) AS dataset_id,
TRIM(splits[OFFSET(5)]) AS table_id,
TRIM(splits[OFFSET(6)]) AS field_id,
TRIM(splits[OFFSET(7)]) AS existing_policy_tag,
TRIM(splits[OFFSET(8)]) AS new_policy_tag,
TRIM(splits[OFFSET(9)]) AS operation,
TRIM(splits[OFFSET(10)]) AS details
FROM `${google_bigquery_table.logging_view_app_log.project}.${google_bigquery_table.logging_view_app_log.dataset_id}.${google_bigquery_table.logging_view_app_log.table_id}`
WHERE log_name = 'tag-history-log'

SQL
  }
}


resource "google_bigquery_table" "logging_view_steps" {
  dataset_id = google_bigquery_dataset.results_dataset.dataset_id
  table_id   = "v_steps"

  #TODO:  Allow destroying the table. Set to true for production use
  deletion_protection=false

  view {
    use_legacy_sql = false
    query = <<SQL
  WITH l1 AS
(
SELECT
run_id,
tracker,
TRIM(splits[OFFSET(3)]) AS function_name,
TRIM(splits[OFFSET(4)]) AS function_number,
TRIM(splits[OFFSET(5)]) AS step
FROM `${google_bigquery_table.logging_view_app_log.project}.${google_bigquery_table.logging_view_app_log.dataset_id}.${google_bigquery_table.logging_view_app_log.table_id}`
WHERE log_name = 'tracker-log'
)

SELECT
TIMESTAMP_MILLIS(CAST(SUBSTR(run_id, 3) AS INT64)) AS start_time,
run_id,
tracker,
function_name,
function_number,
step
FROM l1
SQL
  }
}

resource "google_bigquery_table" "logging_view_broken_steps" {
  dataset_id = google_bigquery_dataset.results_dataset.dataset_id
  table_id   = "v_broken_steps"

  #TODO:  Allow destroying the table. Set to true for production use
  deletion_protection=false

  view {
    use_legacy_sql = false
    query = <<SQL
SELECT
v.start_time,
v.run_id,
v.tracker,
ARRAY_AGG(STRUCT(v.function_number, v.function_name, v.step) ORDER BY v.function_number, v.step DESC) AS chains
FROM ${var.dlp_results_dataset_name}.${google_bigquery_table.logging_view_steps.table_id}  v
WHERE v.function_name <> 'functions.Dispatcher'
GROUP BY 1,2,3
HAVING ARRAY_LENGTH(chains) < 6
SQL
  }
}

# Logging BQ sink must be able to write data to logging table in the dataset
resource "google_bigquery_dataset_iam_member" "logging_sink_access" {
  dataset_id = var.dlp_results_dataset_name
  role       = "roles/bigquery.dataEditor"
  member     = var.logging_sink_sa
}


resource "google_bigquery_table" "view_fields_findings" {
dataset_id = google_bigquery_dataset.results_dataset.dataset_id
table_id   = "v_dlp_fields_findings"

#TODO:  Allow destroying the table. Set to true for production use
deletion_protection=false

view {
use_legacy_sql = false
query = <<SQL
WITH config AS
(
-- keep this in a WITH view to facilitate unit testing by creating static input
SELECT * FROM `${google_bigquery_table.config_view_infotypes_policytags_map.project}.${google_bigquery_table.config_view_infotypes_policytags_map.dataset_id}.${google_bigquery_table.config_view_infotypes_policytags_map.table_id}`

-- SELECT 'service-project' AS project, 'EMAIL_ADDRESS' AS info_type, 'email_policy_tag' AS policy_tag UNION ALL
-- SELECT 'service-project' AS project, 'PERSON_NAME' AS info_type, 'person_policy_tag' AS policy_tag


)
, likelihood AS
(
SELECT 'VERY_UNLIKELY' AS likelihood, 1 AS likelihood_rank UNION ALL
SELECT 'UNLIKELY' AS likelihood, 2 AS likelihood_rank UNION ALL
SELECT 'POSSIBLE' AS likelihood, 3 AS likelihood_rank UNION ALL
SELECT 'LIKELY' AS likelihood, 4 AS likelihood_rank UNION ALL
SELECT 'VERY_LIKELY' AS likelihood, 5 AS likelihood_rank

), dlp_results AS
(
-- keep this in a WITH view to facilitate unit testing by creating static input
SELECT
o.job_name,
l.record_location.field_id.name AS field_name,
o.info_type.name AS info_type,
o.likelihood,
l.record_location.record_key.big_query_key.table_reference.project_id AS project_id
FROM `${google_bigquery_table.results_table.project}.${google_bigquery_table.results_table.dataset_id}.${google_bigquery_table.results_table.table_id}` o
, UNNEST(location.content_locations) l

-- test one field, one likelihood
-- SELECT 'field1' AS field_name, 'EMAIL_ADDRESS' AS info_type_name, 'LIKELY' AS likelihood UNION ALL
-- SELECT 'field1' AS field_name, 'PERSON_NAME' AS info_type_name, 'LIKELY' AS likelihood UNION ALL
-- test one field, diff likelihood
-- SELECT 'field2' AS field_name, 'EMAIL_ADDRESS' AS info_type_name, 'LIKELY' AS likelihood UNION ALL
-- SELECT 'field2' AS field_name, 'PERSON_NAME' AS info_type_name, 'VERY_LIKELY' AS likelihood UNION ALL
-- test one field
-- SELECT 'field3' AS field_name, 'EMAIL_ADDRESS' AS info_type_name, 'POSSIBLE' AS likelihood
)
, ranking AS
(
SELECT DISTINCT
o.job_name,
o.field_name,
o.info_type,
lh.likelihood_rank,
c.policy_tag,
-- in case one field has more than one infotype, select the highest likelihood
RANK() OVER(PARTITION BY o.job_name, o.field_name ORDER BY lh.likelihood_rank DESC) AS rank,
FROM `dlp_results` o
INNER JOIN config c ON
c.project = o.project_id AND
c.info_type = o.info_type
INNER JOIN likelihood lh ON o.likelihood = lh.likelihood
)
, row_numbers AS
(
SELECT
r.*,
-- in case one field has more than one infotype with the same likelihood, select one randomly
ROW_NUMBER() OVER(PARTITION BY job_name, field_name, rank) AS row_number
FROM ranking r
)

SELECT job_name, field_name, info_type, policy_tag
FROM row_numbers
WHERE row_number = rank
SQL
}
}


######## CONFIG VIEWS #####################################################################

resource "google_bigquery_table" "config_view_infotypes_policytags_map" {
  dataset_id = google_bigquery_dataset.results_dataset.dataset_id
  table_id   = "v_config_infotypes_policytags_map"

  #TODO:  Allow destroying the table. Set to true for production use
  deletion_protection=false

  view {
    use_legacy_sql = false
    query = <<SQL
SELECT '${var.project}' AS project, 'EMAIL_ADDRESS' AS info_type, '${var.taxonomy_project1_email_id}' AS policy_tag
UNION ALL
SELECT 'zbooks-910444929556' AS project, 'EMAIL_ADDRESS' AS info_type, '${var.taxonomy_project2_email_id}' AS policy_tag
SQL
  }
}

