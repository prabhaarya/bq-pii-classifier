package functions;

public class InspectorTask {

    private String targetTableProject;
    private String targetTableDataset;
    private String targetTable;
    private String resultsTableProject;
    private String resultsTableDataset;
    private String resultsTable;
    private String pubSubNotificationTopic;
    private String dlpProject;
    private String dlpRegion;
    private String queuePath;
    private String httpEndPoint;
    private String trackingId;


    public String getTargetTableProject() {
        return targetTableProject;
    }

    public void setTargetTableProject(String targetTableProject) {
        this.targetTableProject = targetTableProject;
    }

    public String getTargetTableDataset() {
        return targetTableDataset;
    }

    public void setTargetTableDataset(String targetTableDataset) {
        this.targetTableDataset = targetTableDataset;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getResultsTableProject() {
        return resultsTableProject;
    }

    public void setResultsTableProject(String resultsTableProject) {
        this.resultsTableProject = resultsTableProject;
    }

    public String getResultsTableDataset() {
        return resultsTableDataset;
    }

    public void setResultsTableDataset(String resultsTableDataset) {
        this.resultsTableDataset = resultsTableDataset;
    }

    public String getResultsTable() {
        return resultsTable;
    }

    public void setResultsTable(String resultsTable) {
        this.resultsTable = resultsTable;
    }

    public String getPubSubNotificationTopic() {
        return pubSubNotificationTopic;
    }

    public void setPubSubNotificationTopic(String pubSubNotificationTopic) {
        this.pubSubNotificationTopic = pubSubNotificationTopic;
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

    public String getQueuePath() {
        return queuePath;
    }

    public void setQueuePath(String queuePath) {
        this.queuePath = queuePath;
    }


    public String getHttpEndPoint() {
        return httpEndPoint;
    }

    public void setHttpEndPoint(String httpEndPoint) {
        this.httpEndPoint = httpEndPoint;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    @Override
    public String toString() {
        return "functions.InspectorTask{" +
                "targetTableProject='" + targetTableProject + '\'' +
                ", targetTableDataset='" + targetTableDataset + '\'' +
                ", targetTable='" + targetTable + '\'' +
                ", resultsTableProject='" + resultsTableProject + '\'' +
                ", resultsTableDataset='" + resultsTableDataset + '\'' +
                ", resultsTable='" + resultsTable + '\'' +
                ", pubSubNotificationTopic='" + pubSubNotificationTopic + '\'' +
                ", dlpProject='" + dlpProject + '\'' +
                ", dlpRegion='" + dlpRegion + '\'' +
                ", queuePath='" + queuePath + '\'' +
                ", httpEndPoint='" + httpEndPoint + '\'' +
                ", trackingId='" + trackingId + '\'' +
                '}';
    }
}
