package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import groovy.json.JsonSlurper
import rundeck.services.asyncimport.AsyncImportException
import rundeck.services.asyncimport.AsyncImportMilestone
import rundeck.services.asyncimport.AsyncImportService
import rundeck.services.asyncimport.AsyncImportStatusDTO
import spock.lang.Specification

class AsyncImportServiceSpec extends Specification implements ServiceUnitTest<AsyncImportService>, GrailsWebUnitTest{

    def mockStatusFileBytes(){
        return "{\"errors\":null,\"jobUuidOption\":\"remove\",\"lastUpdate\":\"Movingfile:#897of1037.\",\"lastUpdated\":\"MonOct2310:30:40ART2023\",\"milestone\":null,\"milestoneNumber\":2,\"projectName\":\"test3\",\"tempFilepath\":\"/tmp/AImportTMP-test3\"}".bytes
    }

    def statusFileResourcepath = (projectName) -> "${AsyncImportService.JSON_FILE_PREFIX}${projectName}${AsyncImportService.JSON_FILE_EXT}"

    def "Try to create a status file without a project name or updated data, get an error"(){
        given:
        service.frameworkService = Mock(FrameworkService)
        when:
        def bytes = service.saveAsyncImportStatusForProject(null, null)
        then:
        thrown AsyncImportException
    }

    def "Create gracefully a new status file"(){
        given: "The project name"
        def projectName = "test"
        def fwkProject = Mock(IRundeckProject)
        service.frameworkService = Mock(FrameworkService){
            it.getFrameworkProject(projectName) >> fwkProject
        }
        when: "The method is called"
        def bytes = service.saveAsyncImportStatusForProject(projectName, null)
        then: "The framework resource will be created"
        1 * fwkProject.storeFileResource(_,_) >> -1
        bytes == -1
    }

    def "Update gracefully a existing status file"(){
        given: "The project name"
        def projectName = "test"
        def fwkProject = Mock(IRundeckProject)
        service.frameworkService = Mock(FrameworkService){
            it.getFrameworkProject(projectName) >> fwkProject
        }
        def newStatus = new AsyncImportStatusDTO(projectName, AsyncImportMilestone.M1_CREATED.milestoneNumber).with {
            it.lastUpdate = "Updated"
            return it
        }

        when: "The method is called"
        def bytes = service.saveAsyncImportStatusForProject(null, newStatus)

        then: "The framework resource will be updated"
        1 * fwkProject.storeFileResource(_,_) >> -1
        bytes == -1
    }

    def "Get info about the status file in storage"(){
        given:
        def projectName = "test"
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(statusFileResourcepath(projectName), _) >> {
                it[1].write(mockStatusFileBytes())
                return 4L
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        service.frameworkService = Mock(FrameworkService){
            it.getFrameworkProject(projectName) >> fwkProject
        }

        when: "We try to get info from resource"
        def result = service.getAsyncImportStatusForProject(projectName, out)
        def status = new JsonSlurper().parseText(out.toString()) as AsyncImportStatusDTO

        then:
        result != null
        status != null
    }

    def "Status file updater helper updates the status file leaving existing data intact"(){
        given:
        def projectName = "test"
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(statusFileResourcepath(projectName), _) >> {
                it[1].write(mockStatusFileBytes())
                return 4L
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        service.frameworkService = Mock(FrameworkService){
            it.getFrameworkProject(projectName) >> fwkProject
        }
        def newStatus = new AsyncImportStatusDTO(projectName, 2).with {
            it.lastUpdate = "hey"
            return it
        }

        when: "We try to update, all the old props get copied to the new file and then it writes a resource in storage."
        service.asyncImportStatusFileUpdater(newStatus, out)

        then:
        newStatus.jobUuidOption == 'remove' // this value come from the mock

    }


}
