package rundeck.services.asyncimport

interface AsyncImportStatusFileOperations {
    Long createStatusFile(AsyncImportStatusDTO newStatus);
}