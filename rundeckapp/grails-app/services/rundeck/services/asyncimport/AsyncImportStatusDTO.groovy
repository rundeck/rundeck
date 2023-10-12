package rundeck.services.asyncimport

class AsyncImportStatusDTO {
    String tempFilepath;
    String user;
    String projectName;
    String milestone;
    String lastUpdated;
    String ownerMemberUuid;
    String errors;
    String lastUpdate;

    AsyncImportStatusDTO() {
    }

    AsyncImportStatusDTO(AsyncImportStatusDTO newStatus) {
        this.tempFilepath = newStatus.tempFilepath
        this.user = newStatus.user
        this.projectName = newStatus.projectName
        this.milestone = newStatus.milestone
        this.lastUpdated = newStatus.lastUpdated
        this.ownerMemberUuid = newStatus.ownerMemberUuid
        this.errors = newStatus.errors
        this.lastUpdate = newStatus.lastUpdate
    }

    AsyncImportStatusDTO(String path, String user, String projectName, AsyncImportMilestone milestone, String lastUpdated, String ownerMemberUuid, String errors, Date estimatedProcessDuration, String lastUpdate) {
        this.tempFilepath = tempFilepath
        this.user = user
        this.projectName = projectName
        this.milestone = milestone
        this.lastUpdated = lastUpdated
        this.ownerMemberUuid = ownerMemberUuid
        this.errors = errors
        this.lastUpdate = lastUpdate
    }

}
