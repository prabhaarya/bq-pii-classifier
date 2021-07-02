SELECT MAX(run_id) FROM `facilities-910444929556.bq_security_classifier_4.v_steps`

SELECT * FROM `facilities-910444929556.bq_security_classifier_4.v_steps` WHERE run_id = 'R-1625161810695'

SELECT * FROM `facilities-910444929556.bq_security_classifier_4.v_broken_steps` WHERE run_id = 'R-1625161810695'

SELECT th.*, m.info_type FROM `facilities-910444929556.bq_security_classifier_4.v_log_tag_history` th
INNER JOIN `facilities-910444929556.bq_security_classifier_4.v_config_infotypes_policytags_map` m
ON th.new_policy_tag = m.policy_tag
WHERE run_id = 'R-1625161810695'