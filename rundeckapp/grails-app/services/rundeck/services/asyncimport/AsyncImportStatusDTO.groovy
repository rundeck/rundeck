package rundeck.services.asyncimport

class AsyncImportStatusDTO {
    String path;
    String user;
    String projectName;
    AsyncImportMilestone milestone;
    String lastUpdated;
    String ownerMemberUuid;
    Map<String, String> errors;
    Date estimatedProcessDuration;
    String lastUpdate;
}
