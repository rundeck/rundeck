package rundeck.services.asyncimport

interface AsyncImportStatusFileOperations {
    Boolean createStatusFile(String projectName);
    AsyncImportStatusDTO getAsyncImportStatusForProject(String projectName, ByteArrayOutputStream out);
}