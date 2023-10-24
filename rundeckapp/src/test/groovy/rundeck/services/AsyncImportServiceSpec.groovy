package rundeck.services

import com.dtolabs.rundeck.app.internal.framework.RundeckFramework
import com.dtolabs.rundeck.app.support.ProjectArchiveParams
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import groovy.json.JsonSlurper
import org.apache.commons.io.IOUtils
import org.grails.plugins.testing.GrailsMockMultipartFile
import rundeck.services.asyncimport.AsyncImportException
import rundeck.services.asyncimport.AsyncImportMilestone
import rundeck.services.asyncimport.AsyncImportService
import rundeck.services.asyncimport.AsyncImportStatusDTO
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

class AsyncImportServiceSpec extends Specification implements ServiceUnitTest<AsyncImportService>, GrailsWebUnitTest{

    def setup() {

    }

    def cleanup() {

    }

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
        1 * fwkProject.storeFileResource(_,_) >> 4L
        bytes == 4L
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
        1 * fwkProject.storeFileResource(_,_) >> 4L
        bytes == 4L
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

    def "Gracefully invoke async import milestone 1"(){
        // This test will create real files in /tmp
        given: "The invocation through controller (with context)"
        def projectName = "test"
        def workingDirs = getTempDirsPath(projectName)
        def auth = Mock(UserAndRolesAuthContext)
        def path = getClass().getClassLoader().getResource("async-import-sample-project.jar")
        def file = new File(path.toURI())
        def is = new FileInputStream(file)
        def params = new ProjectArchiveParams().with {
            it.asyncImport = true
            return it
        }
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(_, _) >> {
                it[1].write(mockStatusFileBytes())
                return 4L
            }
        }
        def framework = Mock(IFramework)
        service.frameworkService = Mock(FrameworkService){
            getFrameworkProject(projectName) >> fwkProject
            getRundeckFramework() >> framework
        }
        service.projectService = Mock(ProjectService){
            it.importToProject(
                    _,
                    _,
                    _,
                    _,
                    _) >> [success: true]
        }

        when: "The method gets invoked"
        def result = service.beginMilestone1(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )

        then: "the will be results"
        result.success == true // The project w/o executions will be imported (import to project invoked)
        Files.exists(Paths.get(workingDirs.workingDir)) // The working dir will be created
        Files.exists(Paths.get(workingDirs.projectCopy)) // The copy of the uploaded project will be created

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }
    }

    private def getTempDirsPath(String projectName) {
        def tmpCopy = service.TEMP_DIR + File.separator + service.TEMP_PROJECT_SUFFIX.toString() + projectName
        def tmpWorkingDir = service.BASE_WORKING_DIR.toString() + projectName
        return [projectCopy: tmpCopy, workingDir: tmpWorkingDir]
    }

}
