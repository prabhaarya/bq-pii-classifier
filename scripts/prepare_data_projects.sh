#!/bin/bash

# $1 = Data Project
# $2 = Dispatcher email
# $3 = Tagger email
# $4 = DLP service account email

# to list datasets and tables in a project
gcloud projects add-iam-policy-binding $1 \
    --member="serviceAccount:$2" \
    --role="roles/bigquery.metadataViewer"

# to read and inspect bigquery data
gcloud projects add-iam-policy-binding $1 \
    --member="serviceAccount:$4" \
    --role="roles/bigquery.dataViewer"

# to read table schema and update tables policy tags
gcloud projects add-iam-policy-binding $1 \
    --member="serviceAccount:$3" \
    --role="roles/bigquery.dataOwner"