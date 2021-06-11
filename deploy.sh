export PROJECT_ID=facilities-910444929556
export REGION=europe-west2
export SA_DISPATCHER=sa-sec-classifier-dispatcher
export SA_INSPECTOR=sa-sec-classifier-inspector
export SA_LISTENER=sa-sec-classifier-listener
export SA_TAGGER=sa-sec-classifier-tagger

cd functions/bq_security_classifier_01_dispatcher

gcloud functions deploy bq_security_classifier_01_dispatcher \
--region "${REGION}" \
--entry-point functions.Dispatcher \
--runtime java11 \
--trigger-http \
--timeout 540s \
--service-account ${SA_DISPATCHER}@${PROJECT_ID}.iam.gserviceaccount.com \
--env-vars-file config.yaml

cd ../bq_security_classifier_02_inspector

gcloud functions deploy bq_security_classifier_02_inspector \
--region "${REGION}" \
--entry-point functions.Inspector \
--runtime java11 \
--trigger-http \
--timeout 540s \
--service-account ${SA_INSPECTOR}@${PROJECT_ID}.iam.gserviceaccount.com \
--env-vars-file config.yaml

cd ../bq_security_classifier_03a_listener

gcloud functions deploy bq_security_classifier_03a_listener \
--region="${REGION}" \
--trigger-topic dlp-job-notifications \
--entry-point functions.Listener \
--runtime java11 \
--timeout 540s \
--env-vars-file config.yaml \
--service-account ${SA_LISTENER}@${PROJECT_ID}.iam.gserviceaccount.com

cd ../bq_security_classifier_03b_tagger

gcloud functions deploy bq_security_classifier_03b_tagger \
--region="${REGION}" \
 --entry-point functions.Tagger \
 --runtime java11 \
 --trigger-http \
 --timeout=540s \
 --env-vars-file config.yaml \
 --service-account=${SA_TAGGER}@${PROJECT_ID}.iam.gserviceaccount.com
