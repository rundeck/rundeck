package rundeck.services.asyncimport

interface AsyncImportStatusFileOperations {
    Boolean createStatusFile(String projectName);
    AsyncImportStatusDTO getAsyncImportStatusForProject(String projectName);
    Long updateAsyncImportStatus(AsyncImportStatusDTO updatedStatus);
}