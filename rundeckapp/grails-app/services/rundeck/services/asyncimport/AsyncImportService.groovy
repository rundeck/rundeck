package rundeck.services.asyncimport

import grails.converters.JSON
import rundeck.services.FrameworkService

import java.nio.charset.StandardCharsets

class AsyncImportService implements AsyncImportStatusFileOperations {

    FrameworkService frameworkService

    // Constants
    static final String JSON_FILE_PREFFIX = 'AImport-status-'
    static final String JSON_FILE_EXT = '.json'

    @Override
    Long createStatusFile(AsyncImportStatusDTO newStatus) {
        def resource
        try {
            def jsonStatus = newStatus as JSON
            def inputStream = new ByteArrayInputStream(jsonStatus.toString().getBytes(StandardCharsets.UTF_8));
            if (!newStatus.projectName || newStatus.projectName.size() <= 0) {
                log.error("No project name provided in new status.")
                return 0
            }
            final def fwkProject = frameworkService.getFrameworkProject(newStatus.projectName)
            final def filename = JSON_FILE_PREFFIX + newStatus.projectName + JSON_FILE_EXT
            resource = fwkProject.storeFileResource(filename, inputStream)
            inputStream.close();
            return resource
        } catch (IOException e) {
            return 0
            e.printStackTrace();
        }
    }
}
