package rundeck.services.asyncimport

class AsyncImportStatusDTO {
    String path;
    String user;
    String projectName;
    String milestone;
    String lastUpdated;
    String ownerMemberUuid;
    Map<String, String> errors;
    Date estimatedProcessDuration;
    String lastUpdate;

    AsyncImportStatusDTO() {
    }

    AsyncImportStatusDTO(AsyncImportStatusDTO newStatus) {
        this.path = newStatus.path
        this.user = newStatus.user
        this.projectName = newStatus.projectName
        this.milestone = newStatus.milestone
        this.lastUpdated = newStatus.lastUpdated
        this.ownerMemberUuid = newStatus.ownerMemberUuid
        this.errors = newStatus.errors
        this.estimatedProcessDuration = newStatus.estimatedProcessDuration
        this.lastUpdate = newStatus.lastUpdate
    }

    AsyncImportStatusDTO(String path, String user, String projectName, AsyncImportMilestone milestone, String lastUpdated, String ownerMemberUuid, Map<String, String> errors, Date estimatedProcessDuration, String lastUpdate) {
        this.path = path
        this.user = user
        this.projectName = projectName
        this.milestone = milestone
        this.lastUpdated = lastUpdated
        this.ownerMemberUuid = ownerMemberUuid
        this.errors = errors
        this.estimatedProcessDuration = estimatedProcessDuration
        this.lastUpdate = lastUpdate
    }

}
