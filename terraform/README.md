# create tf bucket

export PROJECT_ID=facilities-910444929556
export REGION_ID=europe-west2
export TF_BUCKET=tf-bq-security-classifier

gsutil mb -l $REGION_ID -p $PROJECT_ID gs://${TF_BUCKET} 

# tf commands

export GOOGLE_APPLICATION_CREDENTIALS=/Users/wadie/facilities-910444929556-d55ab795392d.json

alias tf=terraform

tf init

tf plan -var-file=dev.tfvars

tf apply -var-file=dev.tfvars -auto-approve
