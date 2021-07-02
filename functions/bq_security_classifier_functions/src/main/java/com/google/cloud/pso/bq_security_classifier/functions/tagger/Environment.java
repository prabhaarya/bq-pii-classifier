package com.google.cloud.pso.bq_security_classifier.functions.tagger;

import com.google.cloud.pso.bq_security_classifier.helpers.Utils;

public class Environment {

    public String getProjectId(){
        return Utils.getConfigFromEnv("PROJECT_ID", true);
    }

    public String getDatasetId(){
        return Utils.getConfigFromEnv("DATASET_ID", true);
    }

    public String getDlpResultsTable(){
        return Utils.getConfigFromEnv("DLP_RESULTS_TABLE", true);
    }

    public String getBqViewFieldsFindings(){
        return Utils.getConfigFromEnv("BQ_VIEW_FIELDS_FINDINGS", true);
    }

}
