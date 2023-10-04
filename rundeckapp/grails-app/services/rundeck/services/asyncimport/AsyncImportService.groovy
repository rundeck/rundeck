package rundeck.services.asyncimport

import grails.converters.JSON
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import groovy.json.JsonSlurper
import rundeck.services.FrameworkService

import java.nio.charset.StandardCharsets

class AsyncImportService implements AsyncImportStatusFileOperations, EventPublisher {

    FrameworkService frameworkService

    // Constants
    static final String JSON_FILE_PREFFIX = 'AImport-status-'
    static final String JSON_FILE_EXT = '.json'

    @Override
    Long createStatusFile(String projectName) {
        try {
            saveAsyncImportStatusForProject(projectName, null)
        } catch (IOException e) {
            e.printStackTrace();
            throw e
        }
    }

    @Override
    AsyncImportStatusDTO getAsyncImportStatusForProject(String projectName) {
        try{
            final def fwkProject = frameworkService.getFrameworkProject(projectName)
            ByteArrayOutputStream output = new ByteArrayOutputStream()
            fwkProject.loadFileResource(JSON_FILE_PREFFIX + projectName + JSON_FILE_EXT, output)
            def obj = new JsonSlurper().parseText(output.toString()) as AsyncImportStatusDTO
            log.debug("Object extracted: ${obj.toString()}")
            return obj
        }catch(Exception e){
            log.error("Error during the async import file extraction process: ${e.message}")
        }
        return null
    }

    @Override
    Long updateAsyncImportStatus(AsyncImportStatusDTO updatedStatus) {
        try {
            return saveAsyncImportStatusForProject(null, updatedStatus)
        } catch (IOException e) {
            e.printStackTrace();
            throw e
        }
    }

    Long saveAsyncImportStatusForProject(String projectName = null, AsyncImportStatusDTO newStatus = null){
        def resource
        def statusPersist
        try {
            if( newStatus != null ){
                statusPersist = new AsyncImportStatusDTO(newStatus)
            }else{
                statusPersist = new AsyncImportStatusDTO()
                statusPersist.projectName = projectName
                statusPersist.lastUpdated = new Date().toString()
                statusPersist.lastUpdate = AsyncImportMilestone.M1_CREATED.name
            }
            def jsonStatus = statusPersist as JSON
            def inputStream = new ByteArrayInputStream(jsonStatus.toString().getBytes(StandardCharsets.UTF_8));
            if (!statusPersist.projectName || statusPersist.projectName.size() <= 0) {
                log.error("No project name provided in new status.")
                throw new MissingPropertyException("No project name provided in new status.")
            }
            final def fwkProject = frameworkService.getFrameworkProject(statusPersist.projectName)
            final def filename = JSON_FILE_PREFFIX + statusPersist.projectName + JSON_FILE_EXT
            resource = fwkProject.storeFileResource(filename, inputStream)
            inputStream.close();
            return resource
        } catch (IOException e) {
            e.printStackTrace();
            throw e
        }
    }

    @Subscriber(AsyncImportEvents.ASYNC_IMPORT_EVENT_TEST_UPDATE)
    def updateAsyncImportFileLastUpdateForProject(String projectName, String lastUpdate){
        try {
           if( !projectName ){
               log.error("No project name in async import event notification.")
           }
            def oldStatusFileContent = getAsyncImportStatusForProject(projectName)
            def newStatusFileContent = new AsyncImportStatusDTO(oldStatusFileContent)
                newStatusFileContent.lastUpdated = new Date().toString()
                newStatusFileContent.lastUpdate = lastUpdate
            saveAsyncImportStatusForProject(null, newStatusFileContent)
        } catch (IOException e) {
            e.printStackTrace();
            throw e
        }
    }
}
