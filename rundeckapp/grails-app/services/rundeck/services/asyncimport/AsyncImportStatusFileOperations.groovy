package rundeck.services.asyncimport

interface AsyncImportStatusFileOperations {
    Long createStatusFile(String projectName);
    AsyncImportStatusDTO getAsyncImportStatusForProject(String projectName);
    Long updateAsyncImportStatus(AsyncImportStatusDTO updatedStatus);
}