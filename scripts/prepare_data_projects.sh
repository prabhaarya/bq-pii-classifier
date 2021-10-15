#!/bin/bash

# $1 = Data Project
# $2 = Dispatcher email
# $3 = Inspector email
# $4 = Tagger email
# $5 = DLP service account email

# Dispatcher needs to list datasets and tables in a project
gcloud projects add-iam-policy-binding $1 \
    --member="serviceAccount:$2" \
    --role="roles/bigquery.metadataViewer"

# Inspector needs to view table's metadata (row count)
gcloud projects add-iam-policy-binding $1 \
    --member="serviceAccount:$3" \
    --role="roles/bigquery.metadataViewer"

# Tagger needs to read table schema and update tables policy tags
gcloud projects add-iam-policy-binding $1 \
    --member="serviceAccount:$4" \
    --role="roles/bigquery.dataOwner"

# DLP service account needs to read and inspect bigquery data
gcloud projects add-iam-policy-binding $1 \
    --member="serviceAccount:$5" \
    --role="roles/bigquery.dataViewer"