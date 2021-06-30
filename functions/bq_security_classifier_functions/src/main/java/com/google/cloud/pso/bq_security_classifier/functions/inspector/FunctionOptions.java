package com.google.cloud.pso.bq_security_classifier.functions.inspector;

public class FunctionOptions {
    private String inputProjectId;
    private String inputDatasetId;
    private String inputTableId;
    private String findingsProjectId;
    private String findingsDatasetId;
    private String findingsTableId;
    private String notificationPubSubTopic;
    private String dlpProject;
    private String dlpRegion;
    // Letters, numbers, hyphens, and underscores allowed.
    private String trackingId;

    public FunctionOptions(String inputProjectId, String inputDatasetId, String inputTableId, String findingsProjectId, String findingsDatasetId, String findingsTableId, String notificationPubSubTopic, String dlpProject, String dlpRegion, String trackingId) {
        this.inputProjectId = inputProjectId;
        this.inputDatasetId = inputDatasetId;
        this.inputTableId = inputTableId;
        this.findingsProjectId = findingsProjectId;
        this.findingsDatasetId = findingsDatasetId;
        this.findingsTableId = findingsTableId;
        this.notificationPubSubTopic = notificationPubSubTopic;
        this.dlpProject = dlpProject;
        this.dlpRegion = dlpRegion;
        this.trackingId = trackingId;
    }

    public String getInputProjectId() {
        return inputProjectId;
    }

    public void setInputProjectId(String inputProjectId) {
        this.inputProjectId = inputProjectId;
    }

    public String getInputDatasetId() {
        return inputDatasetId;
    }

    public void setInputDatasetId(String inputDatasetId) {
        this.inputDatasetId = inputDatasetId;
    }

    public String getInputTableId() {
        return inputTableId;
    }

    public void setInputTableId(String inputTableId) {
        this.inputTableId = inputTableId;
    }

    public String getFindingsProjectId() {
        return findingsProjectId;
    }

    public void setFindingsProjectId(String findingsProjectId) {
        this.findingsProjectId = findingsProjectId;
    }

    public String getFindingsDatasetId() {
        return findingsDatasetId;
    }

    public void setFindingsDatasetId(String findingsDatasetId) {
        this.findingsDatasetId = findingsDatasetId;
    }

    public String getFindingsTableId() {
        return findingsTableId;
    }

    public void setFindingsTableId(String findingsTableId) {
        this.findingsTableId = findingsTableId;
    }

    public String getDlpProject() {
        return dlpProject;
    }

    public void setDlpProject(String dlpProject) {
        this.dlpProject = dlpProject;
    }

    public String getDlpRegion() {
        return dlpRegion;
    }

    public void setDlpRegion(String dlpRegion) {
        this.dlpRegion = dlpRegion;
    }

    public String getNotificationPubSubTopic() {
        return notificationPubSubTopic;
    }

    public void setNotificationPubSubTopic(String notificationPubSubTopic) {
        this.notificationPubSubTopic = notificationPubSubTopic;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }


    @Override
    public String toString() {
        return "FunctionOptions{" +
                "inputProjectId='" + inputProjectId + '\'' +
                ", inputDatasetId='" + inputDatasetId + '\'' +
                ", inputTableId='" + inputTableId + '\'' +
                ", findingsProjectId='" + findingsProjectId + '\'' +
                ", findingsDatasetId='" + findingsDatasetId + '\'' +
                ", findingsTableId='" + findingsTableId + '\'' +
                ", notificationPubSubTopic='" + notificationPubSubTopic + '\'' +
                ", dlpProject='" + dlpProject + '\'' +
                ", dlpRegion='" + dlpRegion + '\'' +
                ", trackingId='" + trackingId + '\'' +
                '}';
    }
}
