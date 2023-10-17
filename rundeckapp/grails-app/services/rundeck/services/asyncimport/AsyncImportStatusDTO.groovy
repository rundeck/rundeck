package rundeck.services.asyncimport

class AsyncImportStatusDTO {
    String tempFilepath;
    String user;
    String projectName;
    int milestoneNumber;
    String milestone;
    String lastUpdated;
    String ownerMemberUuid;
    String errors;
    String lastUpdate;
    String jobUuidOption;

    AsyncImportStatusDTO() {
    }

    AsyncImportStatusDTO(AsyncImportStatusDTO newStatus) {
        this.tempFilepath = newStatus.tempFilepath
        this.user = newStatus.user
        this.projectName = newStatus.projectName
        this.milestone = newStatus.milestone
        this.lastUpdated = newStatus.lastUpdated
        this.ownerMemberUuid = newStatus.ownerMemberUuid
        this.milestoneNumber = newStatus.milestoneNumber
        this.errors = newStatus.errors
        this.lastUpdate = newStatus.lastUpdate
        this.jobUuidOption = newStatus.jobUuidOption
    }

}
