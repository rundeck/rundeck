package rundeck.services

import com.dtolabs.rundeck.app.support.ProjectArchiveParams
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import groovy.json.JsonBuilder
import rundeck.services.asyncimport.AsyncImportException
import rundeck.services.asyncimport.AsyncImportMilestone
import rundeck.services.asyncimport.AsyncImportService
import rundeck.services.asyncimport.AsyncImportStatusDTO
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.zip.ZipOutputStream

class AsyncImportServiceSpec extends Specification implements ServiceUnitTest<AsyncImportService>, GrailsWebUnitTest{

    private def mockStatusFile(String projectName, String tempPath = null){
        def statusFile = new AsyncImportStatusDTO(projectName, AsyncImportMilestone.M2_DISTRIBUTION.milestoneNumber).with {
            it.errors = null
            it.milestoneNumber = AsyncImportMilestone.M1_CREATED.milestoneNumber
            it.jobUuidOption = "remove"
            it.lastUpdate = "This is a test"
            it.lastUpdated = new Date().toString()
            it.milestone = AsyncImportMilestone.M1_CREATED.milestoneNumber
            it.projectName = projectName
            it.tempFilepath = tempPath ? tempPath : "unknown-path"
            return it
        }
        return new JsonBuilder(statusFile).toString()
    }

    private final def statusFileResourcepath = (projectName) -> "${AsyncImportService.JSON_FILE_PREFIX}${projectName}${AsyncImportService.JSON_FILE_EXT}"
    private final def sampleProjectInternalProjectPath = "rundeck-test"

    def "Try to create a status file without updated data, get an error"(){
        given:
        service.frameworkService = Mock(FrameworkService)
        when:
        def bytes = service.saveAsyncImportStatusForProject("test", null)
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
                it[1].write(mockStatusFile(projectName).bytes)
                return 4L
            }
        }
        service.frameworkService = Mock(FrameworkService){
            it.getFrameworkProject(projectName) >> fwkProject
        }

        when: "We try to get info from resource"
        def result = service.getAsyncImportStatusForProject(projectName)

        then:
        result != null
    }

    def "Get info about the status file in storage with exception"(){
        given:
        def projectName = "test"
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(statusFileResourcepath(projectName), _) >> {
                it[1].write("nothing like a status file content".bytes)
                return 4L
            }
        }
        service.frameworkService = Mock(FrameworkService){
            it.getFrameworkProject(projectName) >> fwkProject
        }

        when: "We try to get info from resource"
        def result = service.getAsyncImportStatusForProject(projectName)

        then:
        thrown Exception
    }

    def "Status file updater helper updates the status file leaving existing data intact"(){
        given:
        def projectName = "test"
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(statusFileResourcepath(projectName), _) >> {
                it[1].write(mockStatusFile(projectName).bytes)
                return 4L
            }
        }
        service.frameworkService = Mock(FrameworkService){
            it.getFrameworkProject(projectName) >> fwkProject
        }
        def newStatus = new AsyncImportStatusDTO(projectName, 2).with {
            it.lastUpdate = "hey"
            return it
        }

        when: "We try to update, all the old props get copied to the new file and then it writes a resource in storage."
        service.asyncImportStatusFileUpdater(newStatus)

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
            it.importExecutions = true
            return it
        }
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(_, _) >> {
                it[1].write(mockStatusFile(projectName).bytes)
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
        def result = service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )

        then: "the will be results"
        result.success == true // The project w/o executions will be imported (import to project invoked)
        1 * service.projectService.beginAsyncImportMilestone(_,_,_,_)

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }
    }

    def "Milestone 1 doesn't trigger M2 if 'import executions' flag is false"(){
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
            it.importExecutions = false
            return it
        }
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(_, _) >> {
                it[1].write(mockStatusFile(projectName).bytes)
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

        when: "The method gets invoked with import executions to false"
        def result = service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )

        then: "The milestone wont be triggering the M2"
        result.success == true
        0 * service.projectService.beginAsyncImportMilestone(_,_,_,_)

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }
    }

    def "Milestone 1 doesn't trigger M2 if there's no 'executions' dir in uploaded project"(){
        // This test will create real files in /tmp
        given: "The invocation through controller (with context)"
        def projectName = "test"
        def workingDirs = getTempDirsPath(projectName)
        def auth = Mock(UserAndRolesAuthContext)
        def path = getClass().getClassLoader().getResource("async-import-sample-project-wo-execs.jar")
        def file = new File(path.toURI())
        def is = new FileInputStream(file)
        def params = new ProjectArchiveParams().with {
            it.asyncImport = true
            return it
        }
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(_, _) >> {
                it[1].write(mockStatusFile(projectName).bytes)
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

        when: "The method gets invoked with no executions in uploaded file"
        def result = service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )

        then: "The milestone wont be triggering the M2"
        result.success == true
        0 * service.projectService.beginAsyncImportMilestone(_,_,_,AsyncImportMilestone.M2_DISTRIBUTION.milestoneNumber)

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }
    }

    def "Gracefully invoke async import milestone 1, OS independent"(){
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
                it[1].write(mockStatusFile(projectName).bytes)
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
        def result = service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )

        then: "the will be results"
        result.success == true // The project w/o executions will be imported (import to project invoked)

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }
    }

    def "After milestone 1 call, working dir and copy must be created"(){
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
                it[1].write(mockStatusFile(projectName).bytes)
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
        def result = service.startAsyncImport(
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

    def "After milestone 1 call, model project dir and subdirs will be created"(){
        // This test will create real files in /tmp
        given: "The invocation through controller (with context)"
        def projectName = "test"
        def workingDirs = getTempDirsPath(projectName)
        def modelProjectPath = "${workingDirs.workingDir}${File.separator}${service.MODEL_PROJECT_NAME_SUFFIX}"
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
                it[1].write(mockStatusFile(projectName).bytes)
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
        def result = service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )

        then: "the will be results"
        Files.exists(Paths.get(modelProjectPath)) // Model project dir exists
        Files.list(Paths.get(modelProjectPath)).collect(Collectors.toList()).size() > 0 // Model project is not empty

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }
    }

    def "After milestone 1 call, model project has to have only jobs dir"(){
        // This test will create real files in /tmp
        given: "The invocation through controller (with context)"
        def projectName = "test"
        def workingDirs = getTempDirsPath(projectName)
        def modelProjectPath = "${workingDirs.workingDir}${File.separator}${service.MODEL_PROJECT_NAME_SUFFIX}"
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
                it[1].write(mockStatusFile(projectName).bytes)
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
        def result = service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )

        then: "the will be results"
        Files.exists(Paths.get(modelProjectPath)) // Model project dir exists
        Files.list(Paths.get(modelProjectPath + File.separator + sampleProjectInternalProjectPath))
                .collect(Collectors.toList()).size() == 1 // Model project has only 1 subdir (jobs)

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }
    }

    def "Async import milestone 1 can handle errors too"(){
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
                it[1].write(mockStatusFile(projectName).bytes)
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
                    _) >> [success: false, errors: "a lot of errors"]
        }

        when: "The method gets invoked"
        def result = service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )

        then: "the will be results"
        result.success == false // The project w/o executions will be imported (import to project invoked)
        !Files.exists(Paths.get(workingDirs.workingDir)) // The working dir will be deleted if the import is not successful
        !Files.exists(Paths.get(workingDirs.projectCopy)) // As well uploaded project

    }

    def "Invoke async import milestone 2 not having tmp filepath in status file, gets an error"(){
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
            it.importExecutions = true
            return it
        }
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(_, _) >> {
                it[1].write(mockStatusFile(projectName).bytes)
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
        service.configurationService = Mock(ConfigurationService){
            getInteger(service.MAX_EXECS_PER_DIR_PROP_NAME, _) >> 5
        }

        when: "The methods get invoked"
        service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )
        // then we invoke the milestone 2 programatically (bc there's no context in tests and events doesn't work)
        service.distributeExecutionFiles(
                projectName,
                auth,
                fwkProject
        )

        then:
        Files.exists(Paths.get(workingDirs.workingDir)) // The working dir will be created
        Files.exists(Paths.get(workingDirs.projectCopy)) // The copy of the uploaded project will be created
        thrown AsyncImportException

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }
    }

    def "Invoke async import milestone 2"(){
        // This test will create real files in /tmp
        given: "The invocation through controller (with context)"
        def projectName = "test"
        def workingDirs = getTempDirsPath(projectName)
        def distributedExecutionsPath = "${workingDirs.workingDir}${File.separator}${service.DISTRIBUTED_EXECUTIONS_FILENAME}"
        def auth = Mock(UserAndRolesAuthContext)
        def path = getClass().getClassLoader().getResource("async-import-sample-project.jar")
        def file = new File(path.toURI())
        def is = new FileInputStream(file)
        def params = new ProjectArchiveParams().with {
            it.asyncImport = true
            it.importExecutions = true
            return it
        }
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(_, _) >> {
                it[1].write(mockStatusFile(projectName,workingDirs.projectCopy).bytes)
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
        service.configurationService = Mock(ConfigurationService){
            getInteger(service.MAX_EXECS_PER_DIR_PROP_NAME, _) >> 5
        }

        when: "The methods get invoked"
        service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )
        // then we invoke the milestone 2 programatically (bc there's no context in tests and events doesn't work)
        service.distributeExecutionFiles(
                projectName,
                auth,
                fwkProject
        )

        then: "Working dir will be created and inside, the distributed executions dir"
        Files.exists(Paths.get(workingDirs.workingDir)) // The working dir will be created
        !Files.exists(Paths.get(workingDirs.projectCopy)) // The copy of the uploaded project will be deleted after distribution
        Files.exists(Paths.get(distributedExecutionsPath))

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }
    }

    def "Invoke async import milestone 2, executions get distributed by config flag"(){
        // This test will create real files in /tmp
        given: "The invocation through controller (with context)"
        def projectName = "test"
        def workingDirs = getTempDirsPath(projectName)
        def distributedExecutionsPath = "${workingDirs.workingDir}${File.separator}${service.DISTRIBUTED_EXECUTIONS_FILENAME}"
        def auth = Mock(UserAndRolesAuthContext)
        def path = getClass().getClassLoader().getResource("async-import-sample-project.jar")
        def file = new File(path.toURI())
        def is = new FileInputStream(file)
        def params = new ProjectArchiveParams().with {
            it.asyncImport = true
            it.importExecutions = true
            return it
        }
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(_, _) >> {
                it[1].write(mockStatusFile(projectName,workingDirs.projectCopy).bytes)
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
        service.configurationService = Mock(ConfigurationService){
            getInteger(service.MAX_EXECS_PER_DIR_PROP_NAME, _) >> configDistributionFlag
        }

        when: "The methods get invoked"
        service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )
        // then we invoke the milestone 2 programmatically (bc there's no context in tests and events doesn't work)
        service.distributeExecutionFiles(
                projectName,
                auth,
                fwkProject
        )

        then: "Working dir will be created and inside, the distributed executions dir"
        Files.exists(Paths.get(workingDirs.workingDir)) // The working dir will be created
        !Files.exists(Paths.get(workingDirs.projectCopy)) // The copy of the uploaded project will be deleted after distribution
        Files.exists(Paths.get(distributedExecutionsPath)) // Distributed executions dir exist
        Files.list(Paths.get(distributedExecutionsPath)).collect(Collectors.toList()).size() == dirQty // Bundles by the flag

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }

        where:
        configDistributionFlag | dirQty
        1                      | 10
        5                      | 2
        1000                   | 1

    }

    def "Invoke async import milestone 2 not having status file, gets an exception"(){
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
            it.importExecutions = true
            return it
        }
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(_, _) >> {
                return null
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
        service.configurationService = Mock(ConfigurationService){
            getInteger(service.MAX_EXECS_PER_DIR_PROP_NAME, _) >> 5
        }

        when: "The methods get invoked"
        service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )
        // then we invoke the milestone 2 programatically (bc there's no context in tests and events doesn't work)
        service.distributeExecutionFiles(
                projectName,
                auth,
                fwkProject
        )

        then:
        thrown Exception

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }
    }

    def "Gracefully invoke async import milestone 3"(){
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
            it.importExecutions = true
            return it
        }
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(_, _) >> {
                it[1].write(mockStatusFile(projectName,workingDirs.projectCopy).bytes)
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
        service.configurationService = Mock(ConfigurationService){
            getInteger(service.MAX_EXECS_PER_DIR_PROP_NAME, _) >> 5
        }

        when: "The methods get invoked"
        service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )
        service.distributeExecutionFiles(
                projectName,
                auth,
                fwkProject
        )
        service.uploadBundledExecutions(
                projectName,
                auth,
                fwkProject
        )

        then:
        3 * service.projectService.importToProject(_,_,_,_,_) >> [success: true] // one invocation from m1 call and two from m3 (10 execs divided by 5 = 2 dirs)
        // All temp dirs cleaned
        !Files.exists(Paths.get(workingDirs.workingDir))
        !Files.exists(Paths.get(workingDirs.projectCopy))

    }

    def "getPathWithLogic basic usage"(){
        given:
        def className = this.getClass().toString()
        def tempDir = AsyncImportService.TEMP_DIR
        def mockedFileInTmp = new File("${tempDir}${File.separator}${className}")
        Predicate<? super Path> dirMatcher = dirPath -> dirPath.getFileName().toString().endsWith(className)

        when:
        mockedFileInTmp.mkdirs()
        def dirFound = service.getPathWithLogic(Paths.get(tempDir), dirMatcher)

        then:
        dirFound != null

        cleanup:
        if( Files.exists(Paths.get(mockedFileInTmp.toString())) ) service.deleteNonEmptyDir(mockedFileInTmp.toString())
    }

    def "Translate temp path by os basic usage"(){
        when:
        def translatedTempDir = service.stripSlashFromString(mockedEnvVar)

        then:
        translatedTempDir == result

        where:
        mockedEnvVar | result
        "a\\path\\"  | "a\\path"
        "a/path/"    | "a/path"

    }

    def "Create temp copy from stream"(){
        given:
        def path = getClass().getClassLoader().getResource("async-import-sample-project.jar")
        def file = new File(path.toURI())
        def is = new FileInputStream(file)
        def tempDir = AsyncImportService.TEMP_DIR
        def desiredName = "copied-project"
        def desiredPath = Paths.get("${tempDir}${File.separator}${desiredName}")

        when:
        service.extractStream(desiredPath.toString(), is)

        then:
        Files.exists(desiredPath)
        Files.isDirectory(desiredPath)
        Files.list(desiredPath).count() > 0

        cleanup:
        if( Files.exists(desiredPath) ) AsyncImportService.deleteNonEmptyDir(desiredPath.toString())

    }

    def "Create a project copy test"(){
        given:
        def path = getClass().getClassLoader().getResource("async-import-sample-project.jar")
        def file = new File(path.toURI())
        def is = new FileInputStream(file)
        def tempDir = AsyncImportService.TEMP_DIR
        def desiredName = "copied-project"
        def desiredPath = Paths.get("${tempDir}${File.separator}${desiredName}")

        when:
        AsyncImportService.createProjectCopy(desiredPath, is)

        then:
        Files.exists(desiredPath)
        Files.isDirectory(desiredPath)
        Files.list(desiredPath).count() > 0

        cleanup:
        if( Files.exists(desiredPath) ) AsyncImportService.deleteNonEmptyDir(desiredPath.toString())
    }

    def "Delete non empty dir test"(){
        given:
        def tempDir = AsyncImportService.TEMP_DIR
        def dirA = new File("${tempDir}${File.separator}dirA")
        def fileA = Paths.get("${dirA}${File.separator}a.txt")

        when:
        dirA.mkdirs()
        Files.createFile(fileA)
        AsyncImportService.deleteNonEmptyDir(dirA.toString())

        then:
        !Files.exists(fileA)
        !Files.exists(Paths.get(dirA.toString()))
    }

    def "Zip a whole directory recursively"(){
        given:
        def tempDir = AsyncImportService.TEMP_DIR
        def dirA = new File("${tempDir}${File.separator}dirA")
        def fileA = Paths.get("${dirA}${File.separator}a.txt")

        def zippedFilename = "${dirA.toString()}.jar"

        when:
        dirA.mkdirs()
        Files.createFile(fileA)
        try(FileOutputStream fos = new FileOutputStream(zippedFilename)){
            ZipOutputStream zos = new ZipOutputStream(fos)
            AsyncImportService.zipDir(dirA.toString(), "", zos)
        }catch(Exception e){
            e.printStackTrace()
        }

        then:
        Files.exists(Paths.get(zippedFilename))

        cleanup:
        if( Files.exists(Paths.get(zippedFilename)) ) Files.delete(Paths.get(zippedFilename))
        if( Files.exists(Paths.get(dirA.toString())) ) AsyncImportService.deleteNonEmptyDir(dirA.toString())
    }

    def "Create dirs method test"(){
        given:
        def tempDir = AsyncImportService.TEMP_DIR
        def dirA = new File("${tempDir}${File.separator}dirA")
        def dirB = new File("${tempDir}${File.separator}dirB")
        def files = List.of(dirA, dirB)

        when:
        AsyncImportService.createDirs(files)

        then:
        dirA.exists()
        dirB.exists()

        cleanup:
        if( Files.exists(Paths.get(dirA.toString())) ) AsyncImportService.deleteNonEmptyDir(dirA.toString())
        if( Files.exists(Paths.get(dirB.toString())) ) AsyncImportService.deleteNonEmptyDir(dirB.toString())
    }

    def "check executions inside project path"(){
        given:
        def pathWithExecs = getClass().getClassLoader().getResource("async-import-sample-project.jar")
        def pathWithOutExecs = getClass().getClassLoader().getResource("async-import-sample-project-wo-execs.jar")

        Predicate<? super Path> internalProjectMatcher = path -> path.fileName.toString().startsWith(AsyncImportService.MODEL_PROJECT_INTERNAL_PREFIX)

        def fileWithExecs = new File(pathWithExecs.toURI())
        def isExecs = new FileInputStream(fileWithExecs)

        def fileWithOutExecs = new File(pathWithOutExecs.toURI())
        def isWoExecs = new FileInputStream(fileWithOutExecs)

        def tempDir = AsyncImportService.TEMP_DIR
        def parentDirForProjectWithExecs = Paths.get("${tempDir}${File.separator}project-w-execs")
        def parentDirForProjectWithOutExecs = Paths.get("${tempDir}${File.separator}project-wo-execs")

        when:
        AsyncImportService.extractStream(parentDirForProjectWithExecs.toString(), isExecs)
        AsyncImportService.extractStream(parentDirForProjectWithOutExecs.toString(), isWoExecs)

        def internalProjectPathWithExecs = AsyncImportService.getPathWithLogic(
                parentDirForProjectWithExecs,
                internalProjectMatcher
        )

        def internalProjectPathWithOutExecs = AsyncImportService.getPathWithLogic(
                parentDirForProjectWithOutExecs,
                internalProjectMatcher
        )

        def parentAhasExecs = AsyncImportService.checkExecutionsExistenceInPath(internalProjectPathWithExecs)
        def parentBhasExecs = AsyncImportService.checkExecutionsExistenceInPath(internalProjectPathWithOutExecs)

        then:
        parentAhasExecs
        !parentBhasExecs

        cleanup:
        if( Files.exists(parentDirForProjectWithExecs) ) AsyncImportService.deleteNonEmptyDir(parentDirForProjectWithExecs.toString())
        if( Files.exists(parentDirForProjectWithOutExecs) ) AsyncImportService.deleteNonEmptyDir(parentDirForProjectWithOutExecs.toString())
    }

    def "getExecutionBundles basic usage"(){
        given: "The invocation through controller (with context)"
        def projectName = "test"
        def workingDirs = getTempDirsPath(projectName)
        def distributedExecutionsPath = "${workingDirs.workingDir}${File.separator}${service.DISTRIBUTED_EXECUTIONS_FILENAME}"
        def auth = Mock(UserAndRolesAuthContext)
        def path = getClass().getClassLoader().getResource("async-import-sample-project.jar")
        def file = new File(path.toURI())
        def is = new FileInputStream(file)
        def params = new ProjectArchiveParams().with {
            it.asyncImport = true
            it.importExecutions = true
            return it
        }
        def fwkProject = Mock(IRundeckProject){
            it.loadFileResource(_, _) >> {
                it[1].write(mockStatusFile(projectName,workingDirs.projectCopy).bytes)
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
        service.configurationService = Mock(ConfigurationService){
            getInteger(service.MAX_EXECS_PER_DIR_PROP_NAME, _) >> 5
        }

        when: "The methods get invoked"
        service.startAsyncImport(
                projectName,
                auth,
                fwkProject,
                is,
                params
        )
        service.distributeExecutionFiles(
                projectName,
                auth,
                fwkProject
        )

        def listOfDistributedExecutionsBundles = AsyncImportService.getExecutionBundles(Paths.get(distributedExecutionsPath))

        then: "The bundles will be parsed as a iterable"
        listOfDistributedExecutionsBundles.size() > 0

        cleanup:
        if( Files.exists(Paths.get(workingDirs.workingDir)) ){
            service.deleteNonEmptyDir(workingDirs.workingDir)
        }
        if( Files.exists(Paths.get(workingDirs.projectCopy)) ){
            service.deleteNonEmptyDir(workingDirs.projectCopy)
        }

    }

    def "getFilesPathsByPrefixAndExtensionInPath basic usage"(){
        given:
        def testName = specificationContext.currentIteration.name.replaceAll("\\s","")
        def possibleNames = ["1", "2", "3"]
        def tempPath = Paths.get(service.TEMP_DIR)
        def testDir = new File("${tempPath}${File.separator}${testName}")

        when:
        testDir.mkdir()
        if (Files.exists(Paths.get(testDir.toString()))) {
            possibleNames.each {name ->{
                def execFilename = "${AsyncImportService.EXECUTION_FILE_PREFIX}${name}${AsyncImportService.EXECUTION_FILE_EXT}"
                def execPath = Paths.get("${testDir.toString()}${File.separator}${execFilename}")

                def outputFilename = "${AsyncImportService.OUTPUT_FILE_PREFIX}${name}${AsyncImportService.OUTPUT_FILE_EXT}"
                def outputPath = Paths.get("${testDir.toString()}${File.separator}${outputFilename}")

                def stateFilename = "${AsyncImportService.STATE_FILE_PREFIX}${name}${AsyncImportService.STATE_FILE_EXT}"
                def statePath = Paths.get("${testDir.toString()}${File.separator}${stateFilename}")

                Files.createFile(execPath)
                Files.createFile(outputPath)
                Files.createFile(statePath)
            }}
        }
        List<Path> executions = service.getFilesPathsByPrefixAndExtensionInPath(
                testDir.toString(),
                AsyncImportService.EXECUTION_FILE_PREFIX,
                AsyncImportService.EXECUTION_FILE_EXT
        )
        List<Path> logs = service.getFilesPathsByPrefixAndExtensionInPath(
                testDir.toString(),
                AsyncImportService.OUTPUT_FILE_PREFIX,
                AsyncImportService.OUTPUT_FILE_EXT
        )
        List<Path> states = service.getFilesPathsByPrefixAndExtensionInPath(
                testDir.toString(),
                AsyncImportService.STATE_FILE_PREFIX,
                AsyncImportService.STATE_FILE_EXT
        )

        then:
        executions.size() == 3
        logs.size() == 3
        states.size() == 3

        cleanup:
        if( Files.exists(Paths.get(testDir.toString())) ) AsyncImportService.deleteNonEmptyDir(testDir.toString())
    }

    def "copy dir except basic usage"(){
        given:
        def tempDir = AsyncImportService.TEMP_DIR
        def dirA = new File("${tempDir}${File.separator}dirA")
        def dirB = new File("${tempDir}${File.separator}dirB")

        def fileA = Paths.get("${dirA}${File.separator}a.txt")
        def fileB = Paths.get("${dirA}${File.separator}b.txt")

        when:
        dirA.mkdirs()
        dirB.mkdirs()
        Files.createFile(fileA)
        Files.createFile(fileB)

        service.copyDirExcept(
                dirA.toString(),
                dirB.toString(),
                "a.txt"
        )

        then:
        Files.list(Paths.get(dirB.toString())).count() == 1
        Files.list(Paths.get(dirA.toString())).count() == 2 // Only copy, dont move
        Files.list(Paths.get(dirB.toString())).anyMatch { path -> path.fileName.toString() == "b.txt"}

        cleanup:
        if( Files.exists(Paths.get(dirA.toString())) ) AsyncImportService.deleteNonEmptyDir(dirA.toString())
        if( Files.exists(Paths.get(dirB.toString())) ) AsyncImportService.deleteNonEmptyDir(dirB.toString())

    }

    private def getTempDirsPath(String projectName) {
        def tmpCopy = service.TEMP_DIR + File.separator + service.TEMP_PROJECT_SUFFIX.toString() + projectName
        def tmpWorkingDir = service.BASE_WORKING_DIR.toString() + projectName
        return [projectCopy: tmpCopy, workingDir: tmpWorkingDir]
    }

}
