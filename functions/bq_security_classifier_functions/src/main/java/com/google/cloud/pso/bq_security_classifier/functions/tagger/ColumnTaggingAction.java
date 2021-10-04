package com.google.cloud.pso.bq_security_classifier.functions.tagger;

public enum ColumnTaggingAction {

    KEEP_EXISTING,
    OVERWRITE,
    NO_CHANGE,
    CREATE,

    DRY_RUN_KEEP_EXISTING,
    DRY_RUN_OVERWRITE,
    DRY_RUN_NO_CHANGE,
    DRY_RUN_CREATE
}
