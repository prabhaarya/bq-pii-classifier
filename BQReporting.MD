SELECT MAX(run_id) max_run_id, TIMESTAMP_MILLIS(CAST(SUBSTR(MAX(run_id), 3) AS INT64)) AS start_time, FROM `facilities-910444929556.bq_security_classifier_4.v_steps`

SELECT * FROM `facilities-910444929556.bq_security_classifier_4.v_steps` WHERE run_id = 'R-1625475326997'

SELECT * FROM `facilities-910444929556.bq_security_classifier_4.v_broken_steps` WHERE run_id = 'R-1625475326997'

SELECT th.start_time,
th.run_id,
th.tracker,
th.project_id,
th.dataset_id,
th.table_id,
th.field_id,
 m.info_type,
 th.existing_policy_tag,
 th.new_policy_tag,
 th.operation,
 th.details
 FROM `facilities-910444929556.bq_security_classifier_4.v_log_tag_history` th
INNER JOIN `facilities-910444929556.bq_security_classifier_4.v_config_infotypes_policytags_map` m
ON th.new_policy_tag = m.policy_tag
WHERE run_id = 'R-1625483476534'