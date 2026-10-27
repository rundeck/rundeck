/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.app.support.ProjectArchiveExportRequest
import com.dtolabs.rundeck.app.support.ProjectArchiveImportRequest
import com.dtolabs.rundeck.app.support.ProjectArchiveParams
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.RuleSetValidation
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.authorization.providers.Validator
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import org.grails.async.factory.BoundPromise
import org.rundeck.app.data.providers.v1.report.ExecReportDataProvider
import com.dtolabs.rundeck.net.api.RundeckApi
import com.dtolabs.rundeck.net.api.RundeckClient
import com.dtolabs.rundeck.net.model.ProjectImportStatus
import com.dtolabs.rundeck.util.ZipBuilder
import grails.async.Promise
import grails.async.Promises
import grails.events.bus.EventBus
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import groovy.mock.interceptor.MockFor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.grails.async.factory.SynchronousPromiseFactory
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.jetbrains.annotations.NotNull
import org.rundeck.app.acl.ACLFileManager
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.app.authorization.BaseAuthContextEvaluator
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.project.BuiltinExportComponents
import org.rundeck.app.components.project.BuiltinImportComponents
import org.rundeck.app.components.project.ProjectComponent
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
import rundeck.data.report.SaveReportRequestImpl
import org.rundeck.app.data.providers.GormExecReportDataProvider
import org.rundeck.app.services.ExecutionFile
import org.rundeck.core.auth.AuthConstants
import org.slf4j.Logger
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.mock.BehaviorDelegate
import retrofit2.mock.Calls
import retrofit2.mock.NetworkBehavior
import rundeck.*
import rundeck.codecs.JobsXMLCodec
import rundeck.services.asyncimport.AsyncImportException
import rundeck.services.asyncimport.AsyncImportMilestone
import rundeck.services.asyncimport.AsyncImportService
import rundeck.services.asyncimport.AsyncImportStatusDTO
import rundeck.services.logging.ProducedExecutionFile
import rundeck.services.scm.ScmPluginConfigData
import spock.lang.Specification
import spock.lang.Unroll
import retrofit2.mock.MockRetrofit
import retrofit2.converter.jackson.JacksonConverterFactory

import java.nio.charset.StandardCharsets
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static org.junit.Assert.*

class HoldingPromiseFactory extends org.grails.async.factory.SynchronousPromiseFactory {
    @Override
    public <T> Promise<T> createPromise(Closure<T>... closures) {
        return (Promise<T>) new BoundPromise(new ProjectService.DeleteResponse(success: false))
    }
}

class ThreadedPromiseFactory extends org.grails.async.factory.SynchronousPromiseFactory {
    @Override
    public <T> Promise<T> createPromise(Closure<T>... closures) {
        def result = new Object[1]
        def failure = new Throwable[1]
        def worker = new Thread({
            try {
                result[0] = closures[0].call()
            } catch (Throwable t) {
                failure[0] = t
            }
        })
        worker.start()
        worker.join(10000)
        if (failure[0]) {
            throw failure[0]
        }
        return (Promise<T>) new BoundPromise((T) result[0])
    }
}

class CapturingPromiseFactory extends org.grails.async.factory.SynchronousPromiseFactory {
    Closure captured

    @Override
    public <T> Promise<T> createPromise(Closure<T>... closures) {
        captured = closures[0]
        return (Promise<T>) new BoundPromise(new ProjectService.DeleteResponse(success: true))
    }
}

/**
 * Created by greg on 8/5/15.
 */
class ProjectServiceSpec extends Specification implements ServiceUnitTest<ProjectService>, GrailsWebUnitTest, DataTest {

    void setupSpec() {
        mockDomain Project
        mockDomain BaseReport
        mockDomain ExecReport
        mockDomain ScheduledExecution
        mockDomain Execution
        mockDomain Workflow
        mockDomain LogFileStorageRequest
        mockDomain CommandExec
        mockDomain JobExec
        mockDomain JobFileRecord
        mockCodec JobsXMLCodec

    }

    void setup() {
        Project.metaClass.static.lock = { Serializable id -> Project.get(id) }
        def configService = Stub(ConfigurationService) {
            getString('projectService.projectExgitportCache.spec', _) >> 'refreshAfterWrite=2m'
        }

        defineBeans {
            configurationService(InstanceFactoryBean, configService)
        }

        def providerExec = new GormExecReportDataProvider()
        service.execReportDataProvider = providerExec


        // Change the default promise factory so the async project deletion in ProjectService.deleteProject()
        // happens synchronously in tests
        Promises.promiseFactory = new SynchronousPromiseFactory()
    }

    def "loadJobFileRecord"() {
        given:
        def ofileuuid = UUID.randomUUID().toString()
        def ojobid = UUID.randomUUID().toString()
        def newjobid = UUID.randomUUID().toString()
        def oldexecid = '123'

        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                                                       uuid: newjobid,
                                                       adhocFilepath: '/this/is/a/path', groupPath: 'some/where',
                                                       description: 'a job', argString: '-a b -c d',
                                                       workflow: new Workflow(
                                                               keepgoing: true,
                                                               commands: [new CommandExec(
                                                                       [adhocRemoteString: 'test buddy', argString:
                                                                               '-delay 12 -monkey cheese -particle']
                                                               )]
                                                       ),
                                                       ).save()

        Execution exec = new Execution(
                scheduledExecution: se,
                argString: "-ftest1 $ofileuuid",
                user: "testuser",
                project: "AProject",
                loglevel: 'WARN',
                doNodedispatch: true,
                dateStarted: new Date(0),
                dateCompleted: new Date(3600000),
                nodeInclude: 'test1',
                nodeExcludeTags: 'monkey',
                status: 'true',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'exec command')])
        ).save()

        def xml = """
<jobFileRecord>
  <execId>$oldexecid</execId>
  <uuid>$ofileuuid</uuid>
  <recordName>ftest1</recordName>
  <jobId>$ojobid</jobId>
  <fileName>resource.yaml</fileName>
  <sha>071bbe64581d4c33737af25b61ac3d612c3dcdf9d3e869fde77e66e16112daba</sha>
  <size>352</size>
  <dateCreated>2017-02-28T00:17:41Z</dateCreated>
  <lastUpdated>2017-02-28T00:17:42Z</lastUpdated>
  <expirationDate>2017-02-28T00:18:11Z</expirationDate>
  <user>admin</user>
  <fileState>deleted</fileState>
  <storageReference>211ace71-df1c-4b3d-b351-40b8ac007cb9</storageReference>
  <storageType>filesystem-temp</storageType>
  <storageMeta />
  <serverNodeUUID>3425B691-7319-4EEE-8425-F053C628B4BA</serverNodeUUID>
  <recordType>option</recordType>
</jobFileRecord>"""
        service.configurationService = Mock(ConfigurationService)

        when:
        def result = service.loadJobFileRecord(xml.toString(), [123: exec.id], [(ojobid): se.extid])
        def newfileid = result.uuid
        then:
        result != null
        result.jobId == newjobid
        result.execution.id == exec.id
        newfileid != ofileuuid
        result.execution.argString == "-ftest1 $newfileid".toString()

    }
    def "importProjectConfig"(){
        given:
        def tempfile = File.createTempFile("test-importProjectConfig",".properties")
        def origprops = [a:'b',x:'%PROJECT_BASEDIR%/sub/path/file.txt'] as Properties
        tempfile.withOutputStream {
            origprops.store(it,'test')
        }
        tempfile.deleteOnExit()
        def dbproj = new Project(name:  'myproject')
        dbproj.save(flush: true)
        def project = Mock(IRundeckProject){
            getName()>>'myproject'
        }
        def fwk = Mock(Framework){
            1 * getFrameworkProjectsBaseDir() >> new File('/projects/dir')
        }

        when:
        service.importProjectConfig(tempfile,project,fwk)

        then:
        1 * project.setProjectProperties([a:'b',x:'/projects/dir/myproject/sub/path/file.txt'] as Properties)
    }
    def "importProjectMdFiles"(){
        given:
        def tempfile1 = File.createTempFile("test-importProjectConfig1",".md")
        tempfile1.text='file1'
        tempfile1.deleteOnExit()
        def tempfile2 = File.createTempFile("test-importProjectConfig1",".md")
        tempfile2.text='file2'
        tempfile2.deleteOnExit()
        def mdfiles=[
                'readme.md':tempfile1,
                'motd.md':tempfile2
        ]
        def project = Mock(IRundeckProject){
            getName()>>'myproject'

        }

        when:
        service.importProjectMdFiles(mdfiles,project)

        then:
        1 * project.storeFileResource('readme.md',{it.text=='file1'})
        1 * project.storeFileResource('motd.md',{it.text=='file2'})
    }
    def "importProjectACLPolicies valid"(){
        given:
        def tempfile1 = File.createTempFile("test-importProjectConfig1",".md")
        tempfile1.text='file1'
        tempfile1.deleteOnExit()
        def tempfile2 = File.createTempFile("test-importProjectConfig1",".md")
        tempfile2.text='file2'
        tempfile2.deleteOnExit()
        def policyfiles=[
                'test.aclpolicy':tempfile1,
                'test2.aclpolicy':tempfile2
        ]
        def project = Mock(IRundeckProject){
            getName()>>'myProject'
        }
        service.aclFileManagerService=Mock(AclFileManagerService){
            forContext(AppACLContext.project('myProject'))>>Mock(ACLFileManager){
                _ * getValidator()>>Mock(Validator) {
                    1 * validateYamlPolicy( 'files/acls/test.aclpolicy', _) >> Mock(RuleSetValidation) {
                        isValid() >> true
                    }
                    1 * validateYamlPolicy( 'files/acls/test2.aclpolicy', _) >> Mock(RuleSetValidation) {
                        isValid() >> true
                    }
                    0 * _(*_)
                }
            }
            0 * _(*_)
        }

        when:
        def result=service.importProjectACLPolicies(policyfiles,project)

        then:
        result==[]
        1 * service.aclFileManagerService.forContext(AppACLContext.project('myProject')).storePolicyFile('test.aclpolicy', _)
        1 * service.aclFileManagerService.forContext(AppACLContext.project('myProject')).storePolicyFile('test2.aclpolicy', _)
    }
    def "importProjectACLPolicies invalid"(){
        given:
        def tempfile1 = File.createTempFile("test-importProjectConfig1",".md")
        tempfile1.text='file1'
        tempfile1.deleteOnExit()
        def tempfile2 = File.createTempFile("test-importProjectConfig1",".md")
        tempfile2.text='file2'
        tempfile2.deleteOnExit()
        def policyfiles=[
                'test.aclpolicy':tempfile1,
                'test2.aclpolicy':tempfile2
        ]
        def project = Mock(IRundeckProject){
            getName()>>'myproject'
        }
        service.aclFileManagerService=Mock(AclFileManagerService){
            forContext(AppACLContext.project('myproject'))>>Mock(ACLFileManager){
                _ * getValidator()>>Mock(Validator) {
                    1 * validateYamlPolicy( 'files/acls/test.aclpolicy', _) >> Mock(RuleSetValidation) {
                        isValid()>>false
                        getErrors()>>['blah':['blah']]
                        toString()>>'test validation failure'
                    }
                    1 * validateYamlPolicy( 'files/acls/test2.aclpolicy', _) >> Mock(RuleSetValidation) {
                        isValid() >> true
                    }
                    0 * _(*_)
                }
            }
            0 * _(*_)
        }

        when:
        def result=service.importProjectACLPolicies(policyfiles,project)

        then:
        0 * service.aclFileManagerService.forContext(AppACLContext.project('myproject')).storePolicyFile('test.aclpolicy', _)
        1 * service.aclFileManagerService.forContext(AppACLContext.project('myproject')).storePolicyFile('test2.aclpolicy', _)
        result==['files/acls/test.aclpolicy: test validation failure']
    }

    def "replacePlaceholderForProjectProperties"(){
        given:
        def project = Mock(IRundeckProject){
            getName()>>'myproject'
        }
        def fwk = Mock(Framework){
            1 * getFrameworkProjectsBaseDir() >> new File(dir)
        }

        def props = [x: before]

        when:
        def result = service.replacePlaceholderForProperties(project, fwk, props)

        then:
        result!=null
        result.x==after

        where:
         dir      | before                                | after
         '/a/dir' | '/sub/path/file.txt'                  | '/sub/path/file.txt'
         '/a/dir' | '%PROJECT_BASEDIR%/sub/path/file.txt' | '/a/dir/myproject/sub/path/file.txt'
         '/a/dir' | '/sub/path/%PROJECT_BASEDIR%file.txt' | '/sub/path/%PROJECT_BASEDIR%file.txt'
         '/a/dir' | '/sub/path/file.txt%PROJECT_BASEDIR%' | '/sub/path/file.txt%PROJECT_BASEDIR%'
    }

    def "replaceInitialStringInValues"(){
        given:

        def props = [x: before]

        when:
        def result = service.replaceInitialStringInValues(props, string, replacement)

        then:
        result!=null
        result.x==after

        where:
            replacement         | string   | before                       | after
            '%PROJECT_BASEDIR%' | '/a/dir' | '/sub/path/file.txt'         | '/sub/path/file.txt'
            '%PROJECT_BASEDIR%' | '/a/dir' | '/a/dir/sub/path/file.txt'   | '%PROJECT_BASEDIR%/sub/path/file.txt'
            '/a/dir' | '%PROJECT_BASEDIR%' | '%PROJECT_BASEDIR%/sub/path/file.txt' | '/a/dir/sub/path/file.txt'
            '%PROJECT_BASEDIR%' | '/a/dir' | '/b/a/dir/sub/path/file.txt' | '/b/a/dir/sub/path/file.txt'
    }

    def "get getFilesystemProjectsBasedir with Framework"() {
        given:
            def project = Mock(IRundeckProject) {
                getName() >> 'myproject'
            }
            def basedir = new File(dir)
            def fwk = Mock(Framework) {
                1 * getFrameworkProjectsBaseDir() >> basedir
            }

            def path = new File(basedir, project.name).absolutePath

        when:
            def result = service.getFilesystemProjectsBasedir(fwk, project)

        then:
            result != null
            result == path

        where:
            dir = '/a/dir'
    }

    def "delete project disables scm plugins"() {
        given:
        new Project(name: 'myproject').save(flush: true, failOnError: true)
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.configurationService=Mock(ConfigurationService){
            getBoolean('projectService.deferredProjectDelete',_)>>false
        }

        def fwk = Mock(Framework)

        when:
        def result = service.deleteProject(project, fwk, null, null)


        then:
        1 * service.scmService.removeAllPluginConfiguration('myproject')
        1 * service.executionService.deleteBulkExecutionIds(*_)
        3 * fwk.getFrameworkProjectMgr() >> Mock(ProjectManager) {
            1 * removeFrameworkProject('myproject')
            0 * disableFrameworkProject('myproject', _ as Long)
        }
        1 * service.fileUploadService.deleteRecordsForProject('myproject')
        result.success

    }

    def "delete project notifies event bus success"() {
        given:
            new Project(name: 'myproject').save(flush: true, failOnError: true)
            def project = Mock(IRundeckProject) {
                getName() >> 'myproject'
            }
            service.scmService = Mock(ScmService)
            service.executionService = Mock(ExecutionService)
            service.fileUploadService = Mock(FileUploadService)
            service.targetEventBus = Mock(EventBus)
            service.configurationService=Mock(ConfigurationService){
                getBoolean('projectService.deferredProjectDelete',_)>>false
            }
            def fwk = Mock(Framework)

        when:
            def result = service.deleteProject(project, fwk, null, null)

        then:
            1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
            1 * service.eventBus.notify('projectWasDeleted', ['myproject'])
            3 * fwk.getFrameworkProjectMgr() >> Mock(ProjectManager) {
                1 * removeFrameworkProject('myproject')
                0 * disableFrameworkProject('myproject', _ as Long)
            }
            result.success
    }
    def "delete project notifies event bus failure"() {
        given:
            new Project(name: 'myproject').save(flush: true, failOnError: true)
            def project = Mock(IRundeckProject) {
                getName() >> 'myproject'
            }
            service.scmService = Mock(ScmService)
            service.executionService = Mock(ExecutionService)
            service.fileUploadService = Mock(FileUploadService){
                deleteRecordsForProject(_)>>{throw new Exception("test exception")}
            }
            service.configurationService=Mock(ConfigurationService){
                getBoolean('projectService.deferredProjectDelete',_)>>false
            }
            service.targetEventBus = Mock(EventBus)
            def fwk = Mock(Framework)

        when:
            def result = service.deleteProject(project, fwk, null, null)

        then:
            1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
            1 * service.eventBus.notify('projectDeleteFailed', ['myproject'])
            1 * fwk.getFrameworkProjectMgr() >> Mock(ProjectManager) {
                1 * isFrameworkProjectDisabled('myproject') >> false
                0 * disableFrameworkProject('myproject', _ as Long)
                0 * removeFrameworkProject('myproject')
            }
            !result.success
    }
    def "delete project calls component projectDelete"() {
        given:
        new Project(name: 'myproject').save(flush: true, failOnError: true)
        ProjectComponent component1 = Mock(ProjectComponent){
            getName()>>'comp1'
            isComponentEnabled() >> true
        }
        ProjectComponent component2 = Mock(ProjectComponent){
            getName()>>'comp2'
            isComponentEnabled() >> true
        }
        service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
            Map<String, ProjectComponent> beans = [comp1: component1,comp2:component2]
        }


        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)

        def prjMgr = Mock(ProjectManager) {
            removeFrameworkProject(_) >> {}
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> { prjMgr }
        }

        when:
        service.deleteProject(project, fwk, null, null)

        then:
        1 * component1.projectDeleted('myproject')
        1 * component2.projectDeleted('myproject')
    }

    def "delete project without deferral wont disable project"() {
        given:
        new Project(name: 'myproject').save(flush: true, failOnError: true)
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService=Mock(ConfigurationService){
            getBoolean('projectService.deferredProjectDelete',_) >> false
        }
        def fwk = Mock(Framework)

        when:
        def result = service.deleteProject(project, fwk, null, null)

        then:
        1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
        1 * service.eventBus.notify('projectWasDeleted', ['myproject'])
        3 * fwk.getFrameworkProjectMgr() >> Mock(ProjectManager) {
            1 * isFrameworkProjectDisabled('myproject') >> false
            0 * disableFrameworkProject('myproject', _ as Long)
            1 * removeFrameworkProject('myproject')
        }
        result.success
    }

    def "delete project with deferral disables project"() {
        given:
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService=Mock(ConfigurationService){
            getBoolean('projectService.deferredProjectDelete',_) >> true
        }
        if (!Project.findByName('myproject')) {
            new Project(name: 'myproject').save(flush: true, failOnError: true)
        }
        def fwk = Mock(Framework)

        when:
        def result = service.deleteProject(project, fwk, null, null)

        then:
        1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
        1 * service.eventBus.notify('projectWasDeleted', ['myproject'])
        4 * fwk.getFrameworkProjectMgr() >> Mock(ProjectManager) {
            1 * isFrameworkProjectDisabled('myproject') >> false
            1 * disableFrameworkProject('myproject', _ as Long) >> true
            1 * removeFrameworkProject('myproject')
        }
        result.success
    }

    def "delete project deferral switch config override"() {
        given:
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> configValue
        }
        if (!Project.findByName('myproject')) {
            new Project(name: 'myproject').save(flush: true, failOnError: true)
        }
        def fwk = Mock(Framework)

        when:
        def result = service.deleteProject(project, fwk, null, null, deferParam)

        then:
        1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
        1 * service.eventBus.notify('projectWasDeleted', ['myproject'])
        fwk.getFrameworkProjectMgr() >> Mock(ProjectManager) {
            1 * isFrameworkProjectDisabled('myproject') >> false
            disablingCalls * disableFrameworkProject('myproject', _ as Long) >> true
            1 * removeFrameworkProject('myproject')
        }
        result.success

        where:
        deferParam | configValue | disablingCalls
        null       | true        | 1
        null       | false       | 0
        true       | true        | 1
        true       | false       | 1
        false      | true        | 0
        false      | false       | 0

    }

    def "delete of a disabled project resumes cleanup when this JVM has not started it"() {
        given:
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        if (!Project.findByName('myproject')) {
            new Project(name: 'myproject').save(flush: true, failOnError: true)
        }
        def fwk = Mock(Framework)

        when:
        def result = service.deleteProject(project, fwk, null, null)

        then:
        result.success
        result.error == null
        1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
        1 * service.eventBus.notify('projectWasDeleted', ['myproject'])
        3 * fwk.getFrameworkProjectMgr() >> Mock(ProjectManager) {
            1 * isFrameworkProjectDisabled('myproject') >> true
            0 * disableFrameworkProject('myproject', _ as Long)
            0 * enableFrameworkProject('myproject')
            1 * removeFrameworkProject('myproject')
        }
    }

    def "explicit synchronous delete of a disabled project waits for cleanup result"() {
        given:
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        new Project(
            name: 'myproject',
            state: org.rundeck.app.data.model.v1.project.RdProject.State.DISABLED
        ).save(flush: true, failOnError: true)
        def mgr = Mock(ProjectManager) {
            1 * isFrameworkProjectDisabled('myproject') >> true
            0 * disableFrameworkProject('myproject', _ as Long)
            1 * removeFrameworkProject('myproject') >> {
                assert org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive()
            }
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def result = service.deleteProject(project, fwk, null, 'admin', Boolean.FALSE)

        then:
        result.success
        result.error == null
        !service.deferredDeletesInFlight.containsKey('myproject')
        1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
        1 * service.eventBus.notify('projectWasDeleted', ['myproject'])
    }

    def "second delete is rejected while a deferred delete is still in flight"() {
        given:
        Promises.promiseFactory = new HoldingPromiseFactory()
        def disabled = false
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        if (!Project.findByName('myproject')) {
            new Project(name: 'myproject').save(flush: true, failOnError: true)
        }
        def mgr = Mock(ProjectManager) {
            isFrameworkProjectDisabled('myproject') >> { disabled }
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def first = service.deleteProject(project, fwk, null, null)
        def second = service.deleteProject(project, fwk, null, null)

        then:
        first.success
        !second.success
        second.error == 'Cannot delete an already disabled project.'
        service.deferredDeletesInFlight.containsKey('myproject')
        1 * mgr.disableFrameworkProject('myproject', _ as Long) >> { disabled = true }
        0 * mgr.removeFrameworkProject('myproject')
        0 * mgr.enableFrameworkProject('myproject')
    }

    def "a recreated project replaces a stale deferred-delete claim"() {
        given:
        Promises.promiseFactory = new HoldingPromiseFactory()
        def disabled = false
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        new Project(name: 'myproject').save(flush: true, failOnError: true)
        def mgr = Mock(ProjectManager) {
            isFrameworkProjectDisabled('myproject') >> { disabled }
            disableFrameworkProject('myproject', _ as Long) >> { disabled = true }
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def first = service.deleteProject(project, fwk, null, null)
        def claimed = service.deferredDeletesInFlight['myproject']
        Project.findByName('myproject').delete(flush: true)
        new Project(name: 'myproject').save(flush: true, failOnError: true)
        def second = service.deleteProject(project, fwk, null, null)

        then:
        first.success
        second.success
        claimed != null
        claimed != service.deferredDeletesInFlight['myproject']
        0 * mgr.removeFrameworkProject('myproject')
    }

    def "deferred delete does not disable a replacement row"() {
        given:
        def original = new Project(name: 'myproject').save(flush: true, failOnError: true)
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        def mgr = Mock(ProjectManager) {
            1 * isFrameworkProjectDisabled('myproject') >> false
            1 * disableFrameworkProject('myproject', original.id) >> false
            0 * removeFrameworkProject(_)
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def result = service.deleteProject(project, fwk, null, 'admin')

        then:
        !result.success
        result.error == 'Project changed before disabling.'
        service.deferredDeletesInFlight.isEmpty()
        0 * service.eventBus.notify(_, _)
    }

    def "synchronous delete does not clean up a replacement row"() {
        given:
        def original = new Project(name: 'myproject').save(flush: true, failOnError: true)
        def replacement = [null]
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService) {
            0 * removeAllPluginConfiguration(_)
        }
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> false
        }
        def mgr = Mock(ProjectManager) {
            1 * isFrameworkProjectDisabled('myproject') >> false
            0 * disableFrameworkProject(_, _)
            0 * removeFrameworkProject(_)
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }
        Project.metaClass.static.lock = { Serializable id ->
            Project.get(original.id)?.delete(flush: true)
            replacement[0] = new Project(name: 'myproject').save(flush: true, failOnError: true)
            Project.get(id)
        }

        when:
        def result = service.deleteProject(project, fwk, null, 'admin', Boolean.FALSE)

        then:
        !result.success
        result.error.contains('Project changed before cleanup')
        Project.get(replacement[0].id)
        1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
        1 * service.eventBus.notify('projectDeleteFailed', ['myproject'])

        cleanup:
        Project.metaClass.static.lock = { Serializable id -> Project.get(id) }
    }

    def "post-commit cleanup failure keeps the delete tombstone retryable"() {
        given:
        def projectRow = new Project(name: 'myproject').save(flush: true, failOnError: true)
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> false
        }
        def manager = Mock(ProjectManager) {
            isFrameworkProjectDisabled('myproject') >> false
            1 * removeFrameworkProject('myproject')
            1 * isFrameworkProjectRemovalPending('myproject', projectRow.id) >> true
        }
        def framework = Mock(Framework) {
            getFrameworkProjectMgr() >> manager
        }

        when:
        def result = service.deleteProject(project, framework, null, 'admin', Boolean.FALSE)

        then:
        !result.success
        result.error == 'Failed to finish deleting project myproject'
        Project.get(projectRow.id).state == org.rundeck.app.data.model.v1.project.RdProject.State.DISABLED
        1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
        1 * service.eventBus.notify('projectDeleteFailed', ['myproject'])
    }

    def "trusted recovery rejects an enabled replacement under the row lock"() {
        given:
        def replacement = new Project(
            name: 'myproject',
            state: org.rundeck.app.data.model.v1.project.RdProject.State.ENABLED
        ).save(flush: true, failOnError: true)
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService) {
            0 * removeAllPluginConfiguration(_)
        }
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        def mgr = Mock(ProjectManager) {
            1 * isFrameworkProjectDisabled('myproject') >> true
            0 * disableFrameworkProject(_, _)
            0 * removeFrameworkProject(_)
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def result = service.deleteProject(
            project,
            fwk,
            null,
            'system',
            Boolean.TRUE,
            true
        )

        then:
        result.success
        Project.get(replacement.id)
        !service.deferredDeletesInFlight.containsKey('myproject')
        1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
        1 * service.eventBus.notify('projectDeleteFailed', ['myproject'])
    }

    def "a delayed stale request cannot replace the recreated project's active claim"() {
        given:
        def original = new Project(name: 'myproject').save(flush: true, failOnError: true)
        Long staleId = original.id
        original.delete(flush: true)
        def replacement = new Project(name: 'myproject').save(flush: true, failOnError: true)
        def claim = ProjectService.getDeclaredMethod('claimDeferredDelete', String, Long)
        claim.accessible = true

        when:
        Long replacementClaim = claim.invoke(service, 'myproject', replacement.id) as Long
        Long staleClaim = claim.invoke(service, 'myproject', staleId) as Long

        then:
        replacementClaim == replacement.id
        staleClaim == null
        service.deferredDeletesInFlight['myproject'] == replacement.id
    }

    def "a deferred delete blocked by a running execution releases its claim"() {
        given:
        def disabled = false
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.execReportDataProvider = Mock(ExecReportDataProvider) {
            deleteWithTransaction(_) >> { throw new RuntimeException('it is currently being executed') }
        }
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        if (!Project.findByName('myproject')) {
            new Project(name: 'myproject').save(flush: true, failOnError: true)
        }
        def mgr = Mock(ProjectManager) {
            isFrameworkProjectDisabled('myproject') >> { disabled }
            disableFrameworkProject('myproject', _ as Long) >> { disabled = true }
            0 * removeFrameworkProject('myproject')
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def result = service.deleteProject(project, fwk, null, null)

        then:
        result.success
        !service.deferredDeletesInFlight.containsKey('myproject')
        1 * service.eventBus.notify('projectDeleteFailed', ['myproject'])
    }

    def "a running ad-hoc execution releases the claim so delete can be retried"() {
        given:
        def disabled = false
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService) {
            deleteBulkExecutionIds([7L], null, null, false) >> [success: false, failures: [[message: 'Failed to delete execution {{Execution 7}}: The execution is currently running']]]
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.execReportDataProvider = Mock(ExecReportDataProvider)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        if (!Project.findByName('myproject')) {
            new Project(name: 'myproject').save(flush: true, failOnError: true)
        }
        Execution.metaClass.static.findAllByProject = { String name -> [[id: 7L]] }
        def mgr = Mock(ProjectManager) {
            isFrameworkProjectDisabled('myproject') >> { disabled }
            disableFrameworkProject('myproject', _ as Long) >> { disabled = true }
            0 * removeFrameworkProject('myproject')
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def first = service.deleteProject(project, fwk, null, null)
        def second = service.deleteProject(project, fwk, null, null)

        then:
        first.success
        second.success
        second.error == null
        !service.deferredDeletesInFlight.containsKey('myproject')
        2 * service.eventBus.notify('projectDeleteFailed', ['myproject'])

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(Execution)
    }

    def "project removal waits until execution cleanup succeeds"() {
        given:
        new Project(
            name: 'myproject',
            state: org.rundeck.app.data.model.v1.project.RdProject.State.DISABLED
        ).save(flush: true, failOnError: true)
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService) {
            1 * deleteBulkExecutionIds([7L], null, 'system', true) >> [success: false, failures: [[message: 'unauthorized']]]
        }
        service.fileUploadService = Mock(FileUploadService)
        service.execReportDataProvider = Mock(ExecReportDataProvider)
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.targetEventBus = Mock(EventBus)
        Execution.metaClass.static.findAllByProject = { String name -> [[id: 7L]] }
        def mgr = Mock(ProjectManager) {
            0 * removeFrameworkProject(_)
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def result = service.deleteProject(project, fwk, null, 'system', Boolean.FALSE, true)

        then:
        !result.success
        result.error.contains('unauthorized')

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(Execution)
    }

    def "project delete recovery waits for startup execution cleanup"() {
        given:
        def order = []
        service.executionService = Mock(ExecutionService) {
            getStartupCleanupFuture() >> Mock(java.util.concurrent.Future) {
                get() >> { order << 'cleanup'; null }
            }
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> Mock(ProjectManager) {
                listDisabledFrameworkProjectNames() >> { order << 'resume'; [] }
            }
        }
        service.frameworkService = new Expando()
        service.frameworkService.rundeckFramework = fwk

        when:
        service.resumeDisabledProjectsFromBootstrap()

        then:
        order == ['cleanup', 'resume']
    }

    def "failed deferred delete stays disabled and is resumed on the next bootstrap"() {
        given:
        def disabled = false
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.execReportDataProvider = Mock(ExecReportDataProvider) {
            deleteWithTransaction(_) >> { throw new RuntimeException('db') }
        }
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        if (!Project.findByName('myproject')) {
            new Project(name: 'myproject').save(flush: true, failOnError: true)
        }
        def mgr = Mock(ProjectManager) {
            isFrameworkProjectDisabled('myproject') >> { disabled }
            disableFrameworkProject('myproject', _ as Long) >> { disabled = true }
            0 * enableFrameworkProject('myproject')
            0 * removeFrameworkProject('myproject')
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def first = service.deleteProject(project, fwk, null, null)
        def second = service.deleteProject(project, fwk, null, null)

        then:
        first.success
        !second.success
        second.error == 'Cannot delete an already disabled project.'
        1 * service.eventBus.notify('projectDeleteFailed', ['myproject'])
        service.deferredDeletesInFlight.containsKey('myproject')

        when:
        service.deferredDeletesInFlight.clear()
        def resumeAuth = Mock(AuthContext)
        service.rundeckAuthContextProcessor = new Expando()
        service.rundeckAuthContextProcessor.getAuthContextForUserAndRolesAndProject = { String user, roles, String projectName ->
            resumeAuth
        }
        mgr.listDisabledFrameworkProjectNames() >> ['myproject']
        mgr.getFrameworkProject('myproject') >> project
        service.resumeDisabledProjects(fwk)

        then:
        1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
        1 * service.eventBus.notify('projectDeleteFailed', ['myproject'])
        0 * mgr.enableFrameworkProject('myproject')
    }

    def "bootstrap resume continues when one disabled project cannot be loaded"() {
        given:
        def kept = Mock(IRundeckProject) {
            getName() >> 'kept'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        service.rundeckAuthContextProcessor = new Expando()
        service.rundeckAuthContextProcessor.getAuthContextForUserAndRolesAndProject = { String user, roles, String projectName ->
            Mock(AuthContext)
        }
        if (!Project.findByName('kept')) {
            new Project(name: 'kept').save(flush: true, failOnError: true)
        }
        def mgr = Mock(ProjectManager) {
            listDisabledFrameworkProjectNames() >> ['gone', 'kept']
            getFrameworkProject('gone') >> { throw new IllegalArgumentException('Project not found in cache or DB: gone') }
            getFrameworkProject('kept') >> kept
            isFrameworkProjectDisabled('gone') >> true
            isFrameworkProjectDisabled('kept') >> true
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        service.resumeDisabledProjects(fwk)

        then:
        1 * service.eventBus.notify('projectWillBeDeleted', ['kept'])
        0 * service.eventBus.notify('projectWillBeDeleted', ['gone'])
    }

    def "a delete is not queued when the project row id cannot be read"() {
        given:
        Promises.promiseFactory = new HoldingPromiseFactory()
        def row = new Project(name: 'myproject').save(flush: true, failOnError: true)
        def reads = 0
        Project.metaClass.static.findByName = { String n ->
            if (++reads == 1) {
                throw new RuntimeException('db')
            }
            row
        }
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        def mgr = Mock(ProjectManager) {
            isFrameworkProjectDisabled('myproject') >> true
            0 * disableFrameworkProject(_, _)
            0 * removeFrameworkProject(_)
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def failedRead = service.deleteProject(project, fwk, null, null)
        def queued = service.deleteProject(project, fwk, null, null)
        def duplicate = service.deleteProject(project, fwk, null, null)

        then:
        !failedRead.success
        failedRead.error.contains('project id is not available')
        queued.success
        !duplicate.success
        duplicate.error == 'Cannot delete an already disabled project.'
        service.deferredDeletesInFlight['myproject'] == row.id

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(Project)
    }

    def "deferred delete reads the project row on the worker thread"() {
        given:
        Promises.promiseFactory = new ThreadedPromiseFactory()
        if (!Project.findByName('myproject')) {
            new Project(name: 'myproject').save(flush: true, failOnError: true)
        }
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        def mgr = Mock(ProjectManager) {
            isFrameworkProjectDisabled('myproject') >> false
            disableFrameworkProject('myproject', _ as Long) >> true
            1 * removeFrameworkProject('myproject')
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def result = service.deleteProject(project, fwk, null, null)

        then:
        result.success
        1 * service.eventBus.notify('projectWasDeleted', ['myproject'])
        0 * service.eventBus.notify('projectDeleteFailed', ['myproject'])
    }

    def "a queued delete does not remove a project recreated before the task runs"() {
        given:
        def factory = new CapturingPromiseFactory()
        Promises.promiseFactory = factory
        def original = new Project(name: 'myproject').save(flush: true, failOnError: true)
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.execReportDataProvider = Mock(ExecReportDataProvider)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        def mgr = Mock(ProjectManager) {
            isFrameworkProjectDisabled('myproject') >> true
            0 * disableFrameworkProject(_, _)
            0 * removeFrameworkProject(_)
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def queued = service.deleteProject(project, fwk, null, null)
        def claimed = service.deferredDeletesInFlight['myproject']
        original.delete(flush: true)
        def replacement = new Project(name: 'myproject').save(flush: true, failOnError: true)
        def taskResult = factory.captured.call()

        then:
        queued.success
        claimed == original.id
        !taskResult.success
        taskResult.error == 'Project changed before cleanup'
        Project.get(replacement.id)
        !service.deferredDeletesInFlight.containsKey('myproject')
        0 * service.scheduledExecutionService.deleteScheduledExecution(*_)
        0 * service.eventBus.notify('projectWillBeDeleted', _)
    }

    def "cleanup stops when the claimed project is recreated before the row lock"() {
        given:
        def original = new Project(name: 'myproject').save(flush: true, failOnError: true)
        Project replacement
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService) {
            0 * removeAllPluginConfiguration(_)
        }
        service.executionService = Mock(ExecutionService)
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.execReportDataProvider = Mock(ExecReportDataProvider)
        service.targetEventBus = Mock(EventBus)
        def mgr = Mock(ProjectManager) {
            0 * removeFrameworkProject(_)
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        def result = service.deleteProjectInternal(
            project,
            fwk,
            null,
            'system',
            true,
            original.id
        )

        then:
        1 * service.eventBus.notify('projectWillBeDeleted', ['myproject']) >> {
            original.delete(flush: true)
            replacement = new Project(name: 'myproject').save(flush: true, failOnError: true)
            null
        }
        1 * service.eventBus.notify('projectDeleteFailed', ['myproject'])
        !result.success
        result.error.contains('Project changed before cleanup')
        Project.get(replacement.id)
        0 * service.scheduledExecutionService.deleteScheduledExecution(*_)
    }

    def "resume does not delete a name recreated as an enabled project"() {
        given:
        def recreated = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> true
        }
        service.rundeckAuthContextProcessor = new Expando()
        service.rundeckAuthContextProcessor.getAuthContextForUserAndRolesAndProject = { String user, roles, String projectName ->
            Mock(AuthContext)
        }
        def mgr = Mock(ProjectManager) {
            listDisabledFrameworkProjectNames() >> ['myproject']
            isFrameworkProjectDisabled('myproject') >> false
            getFrameworkProject('myproject') >> recreated
            0 * disableFrameworkProject(_, _)
            0 * removeFrameworkProject(_)
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> mgr
        }

        when:
        service.resumeDisabledProjects(fwk)
        def direct = service.deleteProject(recreated, fwk, null, 'system', Boolean.TRUE, true)

        then:
        !direct.success
        direct.error == 'Project is not disabled.'
        0 * service.eventBus.notify('projectWillBeDeleted', _)
    }

    def "import project archive only nodes without config"() {
        setup:

        def project = Mock(IRundeckProject) {
            getName() >> 'importtest'
        }
        def framework = Mock(Framework) {
            getFrameworkProjectsBaseDir() >> { File.createTempDir() }
        }
        def authCtx = Mock(UserAndRolesAuthContext) {
            getUsername() >> { "user" }
            getRoles() >> { ["admin"] as Set }
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * loadImportedJobs(_,_,_,_,_,_) >> { [] }
            1 * issueJobChangeEvents([]) >> {}
        }
        service.logFileStorageService = Mock(LogFileStorageService) {
            getFileForExecutionFiletype(_, _, _, _) >> { File.createTempFile("import", "import") }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){

        }
        ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
            getProject() >> 'importtest'
            getImportConfig() >> false
            getImportNodesSources() >> true
            getImportACL() >> true
            getImportScm() >> true
        }
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                uuid: UUID.randomUUID().toString(),
                adhocFilepath: '/this/is/a/path', groupPath: 'some/where',
                description: 'a job', argString: '-a b -c d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString:
                                        '-delay 12 -monkey cheese -particle']
                        )]
                ),
        )
        def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:se,associations: [:])
        service.rundeckJobDefinitionManager=Mock(RundeckJobDefinitionManager){
            decodeFormat('xml',_)>>[importedJob]
        }

        Properties expectedProperties = new Properties()
        expectedProperties.put("resources.source.1.type", "local")
        LinkedHashSet expectedPropertiesToMerge = ["resources.source."]
        when:
        def result = service.
                importToProject(
                        project, framework, authCtx, getClass().getClassLoader().getResourceAsStream(
                        "test-rdproject.jar"
                ), rq
                )

        then:
        result
        1 * project.mergeProjectProperties(expectedProperties, expectedPropertiesToMerge)
    }

    def "import project archive only config without nodes"() {
        setup:

        def project = Mock(IRundeckProject) {
            getName() >> 'importtest'
        }
        def framework = Mock(Framework) {
            getFrameworkProjectsBaseDir() >> { File.createTempDir() }
        }
        def authCtx = Mock(UserAndRolesAuthContext) {
            getUsername() >> { "user" }
            getRoles() >> { ["admin"] as Set }
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * loadImportedJobs(_,_,_,_,_,_) >> { [] }
            1 * issueJobChangeEvents([]) >> {}
        }
        service.logFileStorageService = Mock(LogFileStorageService) {
            getFileForExecutionFiletype(_, _, _, _) >> { File.createTempFile("import", "import") }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){

        }
        ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
            getProject() >> 'importtest'
            getImportConfig() >> true
            getImportNodesSources() >> false
            getImportACL() >> true
            getImportScm() >> true
        }
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                uuid: UUID.randomUUID().toString(),
                adhocFilepath: '/this/is/a/path', groupPath: 'some/where',
                description: 'a job', argString: '-a b -c d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString:
                                        '-delay 12 -monkey cheese -particle']
                        )]
                ),
        )
        def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:se,associations: [:])
        service.rundeckJobDefinitionManager=Mock(RundeckJobDefinitionManager){
            decodeFormat('xml',_)>>[importedJob]
        }

        Properties expectedProperties = new Properties()
        expectedProperties.putAll([
                "project.disable.schedule"             : "false",
                "project.name"                         : "test",
                "project.jobs.gui.groupExpandLevel"    : "1",
                "project.ssh-authentication"           : "privateKey",
                "service.NodeExecutor.default.provider": "jsch-ssh",
                "project.ssh-command-timeout"          : "0",
                "project.label"                        : "Import Source",
                "project.disable.executions"           : "false",
                "project.ssh-keypath"                  : "/Users/stephen/.ssh/id_rsa",
                "project.description"                  : "Import Source",
                "service.FileCopier.default.provider"  : "jsch-scp",
                "project.ssh-connect-timeout"          : "0"
        ])

        when:
        def result = service.
                importToProject(
                        project, framework, authCtx, getClass().getClassLoader().getResourceAsStream(
                        "test-rdproject.jar"
                ), rq
                )

        then:
        result
        0 * project.mergeProjectProperties(_, _)
        1 * project.setProjectProperties(expectedProperties)
    }

    def "import project archive with config and nodes"() {
        setup:

        def project = Mock(IRundeckProject) {
            getName() >> 'importtest'
        }
        def framework = Mock(Framework) {
            getFrameworkProjectsBaseDir() >> { File.createTempDir() }
        }
        def authCtx = Mock(UserAndRolesAuthContext) {
            getUsername() >> { "user" }
            getRoles() >> { ["admin"] as Set }
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * loadImportedJobs(_,_,_,_,_,_) >> { [] }
            1 * issueJobChangeEvents([]) >> {}
        }
        service.logFileStorageService = Mock(LogFileStorageService) {
            getFileForExecutionFiletype(_, _, _, _) >> { File.createTempFile("import", "import") }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){

        }
        ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
            getProject() >> 'importtest'
            getImportConfig() >> true
            getImportNodesSources() >> true
            getImportACL() >> true
            getImportScm() >> true
        }
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                uuid: UUID.randomUUID().toString(),
                adhocFilepath: '/this/is/a/path', groupPath: 'some/where',
                description: 'a job', argString: '-a b -c d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString:
                                        '-delay 12 -monkey cheese -particle']
                        )]
                ),
        )
        def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:se,associations: [:])
        service.rundeckJobDefinitionManager=Mock(RundeckJobDefinitionManager){
            decodeFormat('xml',_)>>[importedJob]
        }

        Properties expectedProperties = new Properties()
        expectedProperties.putAll([
                "project.disable.schedule"             : "false",
                "project.name"                         : "test",
                "project.jobs.gui.groupExpandLevel"    : "1",
                "project.ssh-authentication"           : "privateKey",
                "service.NodeExecutor.default.provider": "jsch-ssh",
                "project.ssh-command-timeout"          : "0",
                "project.label"                        : "Import Source",
                "project.disable.executions"           : "false",
                "project.ssh-keypath"                  : "/Users/stephen/.ssh/id_rsa",
                "project.description"                  : "Import Source",
                "service.FileCopier.default.provider"  : "jsch-scp",
                "project.ssh-connect-timeout"          : "0",
                "resources.source.1.type"              : "local"
        ])
        LinkedHashSet expectedPropertiesToMerge = ["resources.source"]

        when:
        def result = service.
                importToProject(
                        project, framework, authCtx, getClass().getClassLoader().getResourceAsStream(
                        "test-rdproject.jar"
                ), rq
                )

        then:
        result
        0 * project.mergeProjectProperties(_, _)
        1 * project.setProjectProperties(expectedProperties)
    }

    def "import project archive does not fail when webhooks are enabled but project archive has no webhook defs"() {
        setup:

        ProjectComponent component = Mock(ProjectComponent){
            getName()>>'webhooks'
            getImportFilePatterns()>>['webhooks.yaml']
            isComponentEnabled() >> true
        }
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [webhooks: component]
            }

            def project = Mock(IRundeckProject) {
            getName() >> 'importtest'
        }
        def framework = Mock(Framework) {
            getFrameworkProjectsBaseDir() >> { File.createTempDir() }
        }
        def authCtx = Mock(UserAndRolesAuthContext) {
            getUsername() >> {"user"}
            getRoles() >> {["admin"] as Set}
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * loadImportedJobs(_,_,_,_,_,_) >> { [] }
            1 * issueJobChangeEvents(_) >> {}
        }
        service.logFileStorageService = Mock(LogFileStorageService) {
            getFileForExecutionFiletype(_,_,_,_) >> { File.createTempFile("import","import") }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){

        }

        ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest){
            getProject()>>'importtest'
            getImportConfig()>>true
            getImportNodesSources()>>true
            getImportACL()>>true
            getImportScm()>>true
            getImportComponents()>>[webhooks:true]
        }

            ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                                                           uuid: UUID.randomUUID().toString(),
                                                           adhocFilepath: '/this/is/a/path', groupPath: 'some/where',
                                                           description: 'a job', argString: '-a b -c d',
                                                           workflow: new Workflow(
                                                                   keepgoing: true,
                                                                   commands: [new CommandExec(
                                                                           [adhocRemoteString: 'test buddy', argString:
                                                                                   '-delay 12 -monkey cheese -particle']
                                                                   )]
                                                           ),
                                                           )
            def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:se,associations: [:])
        service.rundeckJobDefinitionManager=Mock(RundeckJobDefinitionManager){
            decodeFormat('xml',_)>>[importedJob]
        }

        when:
        def result = service.importToProject(project,framework,authCtx, getClass().getClassLoader().getResourceAsStream("test-rdproject.jar"),rq)

        then:
        result
        0 * component.doImport(*_)
    }

    def "import project archive with project components none enabled"() {
        setup:

            ProjectComponent component = Mock(ProjectComponent) {
                getName() >> 'webhooks'
                getImportFilePatterns() >> ['webhooks.yaml']
                isComponentEnabled() >> true
            }
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [webhooks: component]
            }

            def project = Mock(IRundeckProject) {
                getName() >> 'importtest'
            }
            def framework = Mock(Framework) {
                getFrameworkProjectsBaseDir() >> { File.createTempDir() }
            }
            def authCtx = Mock(UserAndRolesAuthContext) {
                getUsername() >> { "user" }
                getRoles() >> { ["admin"] as Set }
            }
            service.scheduledExecutionService = Mock(ScheduledExecutionService) {
                1 * loadImportedJobs(_,_,_,_,_,_) >> { [] }
                1 * issueJobChangeEvents([]) >> {}
            }
            service.logFileStorageService = Mock(LogFileStorageService) {
                getFileForExecutionFiletype(_, _, _, _) >> { File.createTempFile("import", "import") }
            }
            service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){

            }
            ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
                getProject() >> 'importtest'
                getImportConfig() >> true
                getImportNodesSources() >> true
                getImportACL() >> true
                getImportScm() >> true
            }
            ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                                                           uuid: UUID.randomUUID().toString(),
                                                           adhocFilepath: '/this/is/a/path', groupPath: 'some/where',
                                                           description: 'a job', argString: '-a b -c d',
                                                           workflow: new Workflow(
                                                               keepgoing: true,
                                                               commands: [new CommandExec(
                                                                   [adhocRemoteString: 'test buddy', argString:
                                                                       '-delay 12 -monkey cheese -particle']
                                                               )]
                                                           ),
                                                           )
            def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:se,associations: [:])
            service.rundeckJobDefinitionManager=Mock(RundeckJobDefinitionManager){
                decodeFormat('xml',_)>>[importedJob]
            }
        when:
            def result = service.
                importToProject(
                    project, framework, authCtx, getClass().getClassLoader().getResourceAsStream(
                    "test-rdproject.jar"
                ), rq
                )

        then:
            result
            0 * component.doImport(*_)
    }

    def "import project archive with component with matching pattern"() {
        setup:
            ProjectComponent component = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [test1: component]
            }

            def project = Mock(IRundeckProject) {
                getName() >> 'importtest'
            }
            def framework = Mock(Framework) {
                getFrameworkProjectsBaseDir() >> { File.createTempDir() }
            }
            def authCtx = Mock(UserAndRolesAuthContext) {
                getUsername() >> { "user" }
                getRoles() >> { ["admin"] as Set }
            }
            service.logFileStorageService = Mock(LogFileStorageService) {
                getFileForExecutionFiletype(_, _, _, _) >> { File.createTempFile("import", "import") }
            }
            service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){

            }
            ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
                getProject() >> 'importtest'
                getImportConfig() >> true
                getImportACL() >> true
                getImportScm() >> true
                getImportComponents() >> [webhooks: true]
                getImportOpts() >> [webhooks: [some: 'thing']]
            }

            def tempfile2 = File.createTempFile("test-archive", ".jar")
            tempfile2.deleteOnExit()
            def jarStream = new JarOutputStream(tempfile2.newOutputStream())
            ZipBuilder builder = new ZipBuilder(jarStream)
            builder.dir('test-project/') {
                builder.file('webhooks.yaml') { Writer writer ->
                    writer << 'test-content'
                }
            }
            jarStream.close()
            component.getImportFilePatterns() >> ['webhooks.yaml']
            component.getName() >> 'webhooks'

        when:
            def result = tempfile2.withInputStream { service.importToProject(project, framework, authCtx, it, rq) }

        then:
            result

            1 * component.doImport(_, _, { it.containsKey('webhooks.yaml') }, [some: 'thing']) >> []

        cleanup:
            tempfile2.delete()
    }

    @Unroll
    def "import project archive with component ordering"() {

            ProjectComponent component = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            ProjectComponent component2 = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [bean1: component, bean2: component2]
            }

            def project = Mock(IRundeckProject) {
                getName() >> 'importtest'
            }
            def framework = Mock(Framework) {
                getFrameworkProjectsBaseDir() >> { File.createTempDir() }
            }
            def authCtx = Mock(UserAndRolesAuthContext) {
                getUsername() >> { "user" }
                getRoles() >> { ["admin"] as Set }
            }
            service.logFileStorageService = Mock(LogFileStorageService) {
                getFileForExecutionFiletype(_, _, _, _) >> { File.createTempFile("import", "import") }
            }
            service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){

            }
            ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
                getProject() >> 'importtest'
                getImportConfig() >> true
                getImportACL() >> true
                getImportScm() >> true
                getImportComponents() >> [(name1): true, (name2): true]
                getImportOpts() >> [(name1): [some: 'thing']]
            }

            def tempfile2 = File.createTempFile("test-archive", ".jar")
            tempfile2.deleteOnExit()
            def jarStream = new JarOutputStream(tempfile2.newOutputStream())
            ZipBuilder builder = new ZipBuilder(jarStream)
            builder.dir('test-project/') {
                builder.file('webhooks.yaml') { Writer writer ->
                    writer << 'test-content'
                }
                builder.file('test2.yaml') { Writer writer ->
                    writer << 'test-content'
                }
            }
            jarStream.close()
            component.getImportFilePatterns() >> ['webhooks.yaml']
            component.getName() >> name1
            component2.getImportFilePatterns() >> ['test2.yaml']
            component2.getName() >> name2

            def orderTest=[]
        given: "components with import ordering"
            component.getImportMustRunAfter() >> comp1After
            component.getImportMustRunBefore() >> comp1Before

            component2.getImportMustRunAfter() >> comp2After
            component2.getImportMustRunBefore() >> comp2Before


        when: "importing a project"
            def result = tempfile2.withInputStream { service.importToProject(project, framework, authCtx, it, rq) }

        then: "components run in correct order"
            result
            orderTest == expectOrder

            1 * component.doImport(_, _, { it.containsKey('webhooks.yaml') }, [some: 'thing']) >>{
                orderTest<<name1

                []
            }
            1 * component2.doImport(_, _, { it.containsKey('test2.yaml') }, _) >> {
                orderTest<<name2
                []
            }

        cleanup:
            tempfile2.delete()

        where:
            name1       | name2   | comp1After | comp1Before | comp2After     | comp2Before  || expectOrder
            'webhooks'  | 'test2' | null       | null        | null           | null         || ['test2', 'webhooks']
            'Awebhooks' | 'test2' | null       | null        | null           | null         || ['Awebhooks', 'test2']
            'webhooks'  | 'test2' | ['test2']  | null        | null           | null         || ['test2', 'webhooks']
            'webhooks'  | 'test2' | null       | ['test2']   | null           | null         || ['webhooks', 'test2']
            'webhooks'  | 'test2' | null       | null        | ['webhooks']   | null         || ['webhooks', 'test2']
            'webhooks'  | 'test2' | null       | null        | null           | ['webhooks'] || ['test2', 'webhooks']
            'webhooks'  | 'test2' | [BuiltinImportComponents.jobs.name()] | null | null | [BuiltinImportComponents.jobs.name()] || ['test2', 'webhooks']
            'webhooks'  | 'test2' | null       | [BuiltinImportComponents.jobs.name()]    | [BuiltinImportComponents.jobs.name()]       | null         || ['webhooks', 'test2']
            'webhooks'  | 'test2' | null       | [BuiltinImportComponents.jobs.name()]    | [BuiltinImportComponents.executions.name()] | null         || ['webhooks', 'test2']
    }
    def "import project archive with importComponent option false"() {
        setup:
            ProjectComponent component = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [test1: component]
            }

            def project = Mock(IRundeckProject) {
                getName() >> 'importtest'
            }
            def framework = Mock(Framework) {
                getFrameworkProjectsBaseDir() >> { File.createTempDir() }
            }
            def authCtx = Mock(UserAndRolesAuthContext) {
                getUsername() >> { "user" }
                getRoles() >> { ["admin"] as Set }
            }
            service.logFileStorageService = Mock(LogFileStorageService) {
                getFileForExecutionFiletype(_, _, _, _) >> { File.createTempFile("import", "import") }
            }
            service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){

            }
            ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
                getProject() >> 'importtest'
                getImportConfig() >> true
                getImportACL() >> true
                getImportScm() >> true
                getImportComponents() >> [webhooks: false]
                getImportOpts() >> [webhooks: [some: 'thing']]
            }

            def tempfile2 = File.createTempFile("test-archive", ".jar")
            tempfile2.deleteOnExit()
            def jarStream = new JarOutputStream(tempfile2.newOutputStream())
            ZipBuilder builder = new ZipBuilder(jarStream)
            builder.dir('test-project/') {
                builder.file('webhooks.yaml') { Writer writer ->
                    writer << 'test-content'
                }
                builder.dir('files/') {
                    builder.file('readme.md') { Writer writer ->
                        writer << '#Test Readme content'
                    }
                }
            }
            jarStream.close()
            component.getImportFilePatterns() >> ['webhooks.yaml']
            component.getName() >> 'webhooks'

        when:
            def result = tempfile2.withInputStream { service.importToProject(project, framework, authCtx, it, rq) }

        then:
            result
            1 * project.storeFileResource('readme.md', _) >> 1234L
            0 * component.doImport(_, _, { it.containsKey('webhooks.yaml') }, [some: 'thing']) >> []

        cleanup:
            tempfile2.delete()
    }
    def "import project archive with component unauthorized"() {
        setup:
            ProjectComponent component = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [test1: component]
            }

            def project = Mock(IRundeckProject) {
                getName() >> 'importtest'
            }
            def framework = Mock(Framework) {
                getFrameworkProjectsBaseDir() >> { File.createTempDir() }
            }
            def authCtx = Mock(UserAndRolesAuthContext) {
                getUsername() >> { "user" }
                getRoles() >> { ["admin"] as Set }
            }
            service.logFileStorageService = Mock(LogFileStorageService) {
                getFileForExecutionFiletype(_, _, _, _) >> { File.createTempFile("import", "import") }
            }
            component.getImportAuthRequiredActions()>>['admin']
            service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
                1 * authorizeApplicationResourceAny(_, _, ['admin']) >> false
            }
            ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
                getProject() >> 'importtest'
                getImportConfig() >> true
                getImportACL() >> true
                getImportScm() >> true
                getImportComponents() >> [webhooks: true]
                getImportOpts() >> [webhooks: [some: 'thing']]
            }

            def tempfile2 = File.createTempFile("test-archive", ".jar")
            tempfile2.deleteOnExit()
            def jarStream = new JarOutputStream(tempfile2.newOutputStream())
            ZipBuilder builder = new ZipBuilder(jarStream)
            builder.dir('test-project/') {
                builder.file('webhooks.yaml') { Writer writer ->
                    writer << 'test-content'
                }
                builder.dir('files/') {
                    builder.file('readme.md') { Writer writer ->
                        writer << '#Test Readme content'
                    }
                }
            }
            jarStream.close()
            component.getImportFilePatterns() >> ['webhooks.yaml']
            component.getName() >> 'webhooks'

        when:
            def result = tempfile2.withInputStream { service.importToProject(project, framework, authCtx, it, rq) }

        then:
            result
            1 * project.storeFileResource('readme.md', _) >> 1234L
            0 * component.doImport(_, _, { it.containsKey('webhooks.yaml') }, [some: 'thing']) >> []

        cleanup:
            tempfile2.delete()
    }

    /**
     * Regression test for RUN-4950 / HackerOne #3960872. See PR description for details.
     */
    def "importExecutionsToProject drops archive-supplied outputfilepath when referenced log file is absent from archive"() {
        setup:
            service.logFileStorageService = Mock(LogFileStorageService)

            //forged path: points at another project's log file, which is never included in the archive
            def forgedPath = '/var/lib/rundeck/logs/rundeck/othersecretproject/run/logs/999.rdlog'
            def execXml = """<executions>
  <execution id='1'>
    <dateStarted>2019-11-07T19:03:41Z</dateStarted>
    <dateCompleted>2019-11-07T19:03:42Z</dateCompleted>
    <status>succeeded</status>
    <outputfilepath>${forgedPath}</outputfilepath>
    <failedNodeList />
    <succeededNodeList>Stephens-MBP</succeededNodeList>
    <abortedby />
    <cancelled>false</cancelled>
    <argString />
    <loglevel>INFO</loglevel>
    <doNodedispatch>false</doNodedispatch>
    <executionType>user</executionType>
    <project>test</project>
    <user>admin</user>
    <workflow keepgoing='false' strategy='node-first'>
    </workflow>
  </execution>
</executions>"""
            def tempXmlFile = File.createTempFile("execution-1", ".xml")
            tempXmlFile.text = execXml
            tempXmlFile.deleteOnExit()

            def execerrors = []

        when:
            //execout is empty: no file in the archive matches the forged outputfilepath
            service.importExecutionsToProject(
                    [tempXmlFile],
                    [:],
                    'sandbox',
                    null,
                    [:],
                    [],
                    [(tempXmlFile): 'executions/execution-1.xml'],
                    execerrors
            )

        then:
            Execution.count() == 1
            def imported = Execution.list().first()
            imported.project == 'sandbox'
            imported.outputfilepath == null
            execerrors.find { it.contains('NO matching outfile') }

        cleanup:
            tempXmlFile.delete()
    }

    @Unroll
    def "import project archive with component with matching pattern #pattern"() {
        setup:
            ProjectComponent component = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [test1: component]
            }

            def project = Mock(IRundeckProject) {
                getName() >> 'importtest'
            }
            def framework = Mock(Framework) {
                getFrameworkProjectsBaseDir() >> { File.createTempDir() }
            }
            def authCtx = Mock(UserAndRolesAuthContext) {
                getUsername() >> { "user" }
                getRoles() >> { ["admin"] as Set }
            }
            service.logFileStorageService = Mock(LogFileStorageService) {
                getFileForExecutionFiletype(_, _, _, _) >> { File.createTempFile("import", "import") }
            }
            service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){

            }
            ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
                getProject() >> 'importtest'
                getImportConfig() >> true
                getImportACL() >> true
                getImportScm() >> true
                getImportComponents() >> [webhooks: true]
                getImportOpts() >> [webhooks: [some: 'thing']]
            }

            def tempfile2 = File.createTempFile("test-archive", ".jar")
            tempfile2.deleteOnExit()
            def jarStream = new JarOutputStream(tempfile2.newOutputStream())
            ZipBuilder builder = new ZipBuilder(jarStream)
            builder.dir('test-project/') {
                builder.dir('something-else') {
                    builder.file('blah.blah') { Writer writer ->
                        writer << 'test-content'
                    }
                }
            }
            jarStream.close()
            component.getImportFilePatterns() >> [pattern]
            component.getName() >> 'webhooks'

        when:
            def result = tempfile2.withInputStream { service.importToProject(project, framework, authCtx, it, rq) }

        then:
            result

            1 * component.doImport(_, _, { it.containsKey('something-else/blah.blah') }, [some: 'thing']) >> []

        cleanup:
            tempfile2.delete()

        where:
            pattern                    | _
            'something-else/blah.blah' | _
            'something-else/.*.blah'    | _
            'something-else/*.*'       | _
            'something-else/*'         | _
            '*/blah.blah'              | _
            '*/.*.blah'              | _
            '*/*.*'                    | _
    }

    def "import project archive throws error when empty or non-zip content"() {
        setup:
        ProjectComponent component = Mock(ProjectComponent){
            isComponentEnabled() >> true
        }
        service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
            Map<String, ProjectComponent> beans = [test1: component]
        }

        def project = Mock(IRundeckProject) {
            getName() >> 'importtest'
        }
        def framework = Mock(Framework) {
            getFrameworkProjectsBaseDir() >> { File.createTempDir() }
        }
        def authCtx = Mock(UserAndRolesAuthContext) {
            getUsername() >> { "user" }
            getRoles() >> { ["admin"] as Set }
        }
        service.logFileStorageService = Mock(LogFileStorageService) {
            getFileForExecutionFiletype(_, _, _, _) >> { File.createTempFile("import", "import") }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){

        }
        ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
            getProject() >> 'importtest'
            getImportConfig() >> true
            getImportACL() >> true
            getImportScm() >> true
            getImportComponents() >> [webhooks: true]
            getImportOpts() >> [webhooks: [some: 'thing']]
        }

        def tempfile2 = File.createTempFile("test-archive", ".jar")
        tempfile2.deleteOnExit()

        if(jarContent) {
            def writer = new PrintWriter(tempfile2.newOutputStream())
            writer.println(jarContent)
            writer.close()
        }

        component.getImportFilePatterns() >> ['webhooks.yaml']
        component.getName() >> 'webhooks'

        when:
        def result = tempfile2.withInputStream { service.importToProject(project, framework, authCtx, it, rq) }

        then:
        def e = thrown(IOException)
        e.message == errmsg
        0 * project.storeFileResource('readme.md', _) >> 1234L
        0 * component.doImport(_, _, { it.containsKey('webhooks.yaml') }, [some: 'thing']) >> []

        cleanup:
        tempfile2.delete()

        where:
        jarContent                                                                      | errmsg
        null                                                                            | "Empty or corrupted archive file"
        "###!!!!!?????Some invalid content to write to a zip file.00010100100101!!!!!!" | "Empty or corrupted archive file"
    }

    def "import project archive throws error when zip file is valid but nothing is imported"() {
        setup:
        ProjectComponent component = Mock(ProjectComponent){
            isComponentEnabled() >> true
        }
        service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
            Map<String, ProjectComponent> beans = [test1: component]
        }

        def project = Mock(IRundeckProject) {
            getName() >> 'importtest'
        }
        def framework = Mock(Framework) {
            getFrameworkProjectsBaseDir() >> { File.createTempDir() }
        }
        def authCtx = Mock(UserAndRolesAuthContext) {
            getUsername() >> { "user" }
            getRoles() >> { ["admin"] as Set }
        }
        service.logFileStorageService = Mock(LogFileStorageService) {
            getFileForExecutionFiletype(_, _, _, _) >> { File.createTempFile("import", "import") }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){

        }
        ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
            getProject() >> 'importtest'
            getImportConfig() >> true
            getImportACL() >> true
            getImportScm() >> true
            getImportComponents() >> [webhooks: true]
            getImportOpts() >> [webhooks: [some: 'thing']]
        }

        def tempfile2 = File.createTempFile("test-archive", ".jar")
        tempfile2.deleteOnExit()
        def jarStream = new JarOutputStream(tempfile2.newOutputStream())
        ZipBuilder builder = new ZipBuilder(jarStream)
        builder.dir('unrelated-dir/') {
            builder.file('virus.exe') { Writer writer ->
                writer << 'test-bin-content-very-malicious-and-evil-ñacañacañññáááááááééééé'.getBytes(StandardCharsets.US_ASCII)
            }
        }
        builder.file('malicious_image.png') { Writer writer ->
            writer << 'test-bin-content-very-malicious-and-evil-ñacañacañññáááááááééééé'.getBytes(StandardCharsets.US_ASCII)
        }
        builder.file('malicious_text.txt') { Writer writer ->
            writer << 'test-content-very-malicious-and-evil-ñacañacañññáááááááééééé'
        }
        jarStream.close()
        component.getImportFilePatterns() >> ['webhooks.yaml']
        component.getName() >> 'webhooks'

        when:
        def result = tempfile2.withInputStream { service.importToProject(project, framework, authCtx, it, rq) }

        then:
        def e = thrown(ProjectServiceException)
        e.message == "Nothing to import found in archive"
        0 * project.storeFileResource('readme.md', _) >> 1234L
        0 * component.doImport(_, _, { it.containsKey('webhooks.yaml') }, [some: 'thing']) >> []

        cleanup:
        tempfile2.delete()

    }


    def "basic export project to stream"() {
        given:

            File temp = File.createTempFile("test", "zip")
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>(){
                Map<String, ProjectComponent> beans=[:]
            }


            def project = Mock(IRundeckProject)
            def framework = Mock(IFramework)
            def output = new ZipOutputStream(temp.newOutputStream())
            def options = Mock(ProjectArchiveExportRequest)
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener){

            }
            service.rundeckAuthContextEvaluator = Mock(BaseAuthContextEvaluator)
        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            true
            1 * listener.total('export', 0)
            0 * listener.inc('export', _)
            1 * listener.done()
        cleanup:
            temp.delete()


    }
    def "basic export executions only to stream"() {
        given:

            File temp = File.createTempFile("test", "zip")
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>(){
                Map<String, ProjectComponent> beans=[:]
            }

            ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'testproj', uuid: 'new-job-uuid')
            assertNotNull se.save()
            Execution exec = new Execution(
                argString: "-test args",
                user: "testuser",
                project: "testproj",
                loglevel: 'WARN',
                doNodedispatch: true,
                dateStarted: new Date(0),
                dateCompleted: new Date(3600000),
                nodeInclude: 'test1',
                nodeExcludeTags: 'monkey',
                status: 'true',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'exec command')]),
                scheduledExecution: se
            )
            assertNotNull exec.save()
            ExecReport er = ExecReport.fromExec(exec).save()
            assert null!=er

            def project = Mock(IRundeckProject){
                getName()>>'testproj'
            }
            def framework = Mock(IFramework)
            List<String> entries=[]
            def output = new ZipOutputStream(temp.newOutputStream()){
                @Override
                void putNextEntry(@NotNull final ZipEntry e) throws IOException {
                    entries<<e.name
                    super.putNextEntry(e)
                }
            }
            def options = Mock(ProjectArchiveExportRequest){
                isAll()>>false
                isExecutionsOnly()>>true
                getExecutionIds()>>[
                    exec.id.toString()
                ]
            }
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener){

            }

            service.logFileStorageService = Mock(LogFileStorageService)
            service.rundeckAuthContextEvaluator = Mock(BaseAuthContextEvaluator)
            service.loggingService = Mock(LoggingService)
            service.workflowService = Mock(WorkflowService)



        
            service.executionUtilService=Mock(ExecutionUtilService){
                1 * exportExecutionXml(_, _, _)>>{
                    it[1].write('test\n')
                }
            }
        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            true
            1 * listener.total('export', 4)
            _ * listener.inc('export', _)
            1 * listener.done()
            entries.contains("rundeck-testproj/executions/execution-${exec.id}.xml".toString())
            entries.contains("rundeck-testproj/reports/report-${exec.id}.xml".toString()    )
        cleanup:
            temp.delete()


    }
    def "component export project to stream"() {
        given:
            ProjectComponent component = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            component.getName() >> 'test1'
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [test1: component]
            }

            File temp = File.createTempFile("test", "zip")

            def project = Mock(IRundeckProject){
                getName()>>'aProject'
            }
            def framework = Mock(IFramework)
            def output = new ZipOutputStream(temp.newOutputStream())
            def options = Mock(ProjectArchiveExportRequest){
                getExportOpts() >> [test1: [a: 'b']]
            }
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener)
            service.rundeckAuthContextEvaluator = Mock(BaseAuthContextEvaluator)
        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            true
            1 * listener.total('export', 1)
            1 * listener.inc('export', 1)
            1 * listener.done()
            1 * component.export('aProject', _, [a: 'b'])
        cleanup:
            temp.delete()
    }

    @Unroll
    def "export project with components ordered"() {
        given:
            ProjectComponent component = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            ProjectComponent component2 = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }

            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [test1: component,test2:component2]
            }

            File temp = File.createTempFile("test", "zip")

            def project = Mock(IRundeckProject){
                getName()>>'aProject'
            }
            def framework = Mock(IFramework)
            def output = new ZipOutputStream(temp.newOutputStream())
            def options = Mock(ProjectArchiveExportRequest){

            }
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener)
            service.rundeckAuthContextEvaluator = Mock(BaseAuthContextEvaluator)

            component.getName() >> name1
            component2.getName() >> name2
            component.getExportMustRunAfter()>>comp1After
            component.getExportMustRunBefore()>>comp1Before
            component2.getExportMustRunAfter()>>comp2After
            component2.getExportMustRunBefore()>>comp2Before
            def compOrder = []

        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            1 * listener.total('export', 2)
            2 * listener.inc('export', 1)
            1 * listener.done()
            1 * component.export('aProject', _, _)>>{
                compOrder<<name1
            }
            1 * component2.export('aProject', _, _)>>{
                compOrder<<name2
            }
            compOrder == expectOrder
        cleanup:
            temp.delete()

        where:
            name1   | name2   | comp1After | comp1Before | comp2After     | comp2Before || expectOrder
            'test1' | 'test2' | null       | null        | null           | null        || ['test1', 'test2']
            'testX' | 'testA' | null       | null        | null           | null        || ['testA', 'testX']
            'test1' | 'test2' | ['test2']  | null        | null           | null        || ['test2', 'test1']
            'test1' | 'test2' | null       | ['test2']   | null           | null        || ['test1', 'test2']
            'test1' | 'test2' | null       | null        | ['test1']      | null        || ['test1', 'test2']
            'test1' | 'test2' | null       | null        | null           | ['test1']   || ['test2', 'test1']
            'test1' | 'test2' | [BuiltinExportComponents.jobs.name()] | null | null | [BuiltinExportComponents.jobs.name()] || ['test2', 'test1']
            'test1' | 'test2' | null       | [BuiltinExportComponents.jobs.name()]    | [BuiltinExportComponents.jobs.name()]       | null        || ['test1', 'test2']
            'test1' | 'test2' | null       | [BuiltinExportComponents.jobs.name()]    | [BuiltinExportComponents.executions.name()] | null        || ['test1', 'test2']
    }
    @Unroll
    def "export project with components ordered cyclic"() {
        given:
            ProjectComponent component = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            ProjectComponent component2 = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            ProjectComponent component3 = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }

            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [test1: component, test2: component2, test3: component3]
            }

            File temp = File.createTempFile("test", "zip")

            def project = Mock(IRundeckProject){
                getName()>>'aProject'
            }
            def framework = Mock(IFramework)
            def output = new ZipOutputStream(temp.newOutputStream())
            def options = Mock(ProjectArchiveExportRequest){

            }
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener)
            service.rundeckAuthContextEvaluator = Mock(BaseAuthContextEvaluator)

            component.getName() >> name1
            component2.getName() >> name2
            component3.getName() >> name3
            component.getExportMustRunAfter()>>comp1After
            component.getExportMustRunBefore()>>comp1Before
            component2.getExportMustRunAfter()>>comp2After
            component2.getExportMustRunBefore()>>comp2Before
            component3.getExportMustRunAfter()>>comp3After
            component3.getExportMustRunBefore()>>comp3Before
            def compOrder = []

        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            1 * listener.total('export', 3)
            3 * listener.inc('export', 1)
            1 * listener.done()
            1 * component.export('aProject', _, _)>>{
                compOrder<<name1
            }
            1 * component2.export('aProject', _, _)>>{
                compOrder<<name2
            }
            1 * component3.export('aProject', _, _)>>{
                compOrder<<name3
            }
            compOrder == expectOrder
        cleanup:
            temp.delete()

        where:
            name1   | name2   |name3   | comp1After | comp1Before | comp2After | comp2Before | comp3After | comp3Before || expectOrder
            'test1' | 'test2' |'test3' | ['test2']  | null        | ['test3']  | null        | ['test1']  | null        || ['test1', 'test2','test3']
    }

    def "export project to stream optional component no components specified"() {
        given:
            ProjectComponent component = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            component.getName() >> 'test1'
            component.isExportOptional() >> true
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [test1: component]
            }

            File temp = File.createTempFile("test", "zip")

            def project = Mock(IRundeckProject){
                getName()>>'aProject'
            }
            def framework = Mock(IFramework)
            def output = new ZipOutputStream(temp.newOutputStream())
            def options = Mock(ProjectArchiveExportRequest){
                isJobs() >> true
            }
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener)
            service.rundeckAuthContextEvaluator = Mock(BaseAuthContextEvaluator)
        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            true
            1 * listener.total('export', 0)
            1 * listener.done()
        cleanup:
            temp.delete()
    }

    @Unroll
    def "component export project to stream when authorized #authorized"() {
        given:
            ProjectComponent component = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            component.getName() >> 'test1'
            component.getExportAuthRequiredActions() >> ['a', 'b']
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [test1: component]
            }

            File temp = File.createTempFile("test", "zip")


            def project = Mock(IRundeckProject){
                getName()>>'aProject'
            }
            def framework = Mock(IFramework)
            def output = new ZipOutputStream(temp.newOutputStream())
            def options = Mock(ProjectArchiveExportRequest){
                getExportOpts() >> [test1: [a: 'b']]
            }
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener)
            service.rundeckAuthContextEvaluator = Mock(BaseAuthContextEvaluator)
        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            true
            1 * listener.total('export', count)
            (count) * listener.inc('export', 1)
            1 * listener.done()
            _ * service.rundeckAuthContextEvaluator.authResourceForProject('aProject') >> [test: 'resource']
            1 * service.
                rundeckAuthContextEvaluator.
                authorizeApplicationResourceAny(auth, [test: 'resource'], ['a', 'b']) >> authorized
            (count) * component.export('aProject', _, [a: 'b'])
        cleanup:
            temp.delete()
        where:
            authorized | count
            true       | 1
            false      | 0
    }

    @Unroll
    def "export project to stream all options"() {
        given:
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [:]
            }

            File temp = File.createTempFile("test", "zip")
            def projectProps = new Properties()
            projectProps.setProperty('project.name', 'aProject')

            def project = Mock(IRundeckProject){
                getName()>>'aProject'
                getProjectProperties()>>projectProps
            }
            def framework = Mock(IFramework)
            def output = new ZipOutputStream(temp.newOutputStream())
            def options = Mock(ProjectArchiveExportRequest){
                isAll()>>true
            }
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener)
            service.rundeckAuthContextEvaluator = Mock(BaseAuthContextEvaluator)
        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            true
            1 * listener.total('export', 2)
            2 * listener.inc('export', 1)
            1 * listener.done()
            _ * service.rundeckAuthContextEvaluator.authResourceForProject('aProject') >> [test: 'resource']
            0 * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, [test: 'resource'], ['a', 'b'])

        cleanup:
            temp.delete()
    }
    @Unroll
    def "component export project to stream optional"() {
        given:
            ProjectComponent component = Mock(ProjectComponent){
                isComponentEnabled() >> true
            }
            component.getName() >> 'test1'
            component.isExportOptional() >> exportOptional
            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [test1: component]
            }

            File temp = File.createTempFile("test", "zip")


            def projectProps = new Properties()
            projectProps.setProperty('project.name', 'aProject')
            def project = Mock(IRundeckProject){
                getName()>>'aProject'
                getProjectProperties()>>projectProps
            }
            def framework = Mock(IFramework)
            def output = new ZipOutputStream(temp.newOutputStream())
            def options = Mock(ProjectArchiveExportRequest){
                getExportOpts() >> [test1: [a: 'b']]
                getExportComponents() >> [test1: isComp]
                isAll() >> isAllOpt
            }
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener)
            service.rundeckAuthContextEvaluator = Mock(BaseAuthContextEvaluator)
        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            true
            1 * listener.total('export', count)
            (count) * listener.inc('export', 1)
            1 * listener.done()
            _ * service.rundeckAuthContextEvaluator.authResourceForProject('aProject') >> [test: 'resource']
            0 * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, [test: 'resource'], ['a', 'b'])
            (compCount) * component.export('aProject', _, [a: 'b'])
        cleanup:
            temp.delete()
        where:
            isComp | isAllOpt | exportOptional | count | compCount
            true   | false    | true           | 1     | 1
            true   | true     | true           | 3     | 1
            false  | true     | true           | 3     | 1
            false  | false    | true           | 0     | 0
            false  | false    | false          | 1     | 1
    }
    @Unroll
    def "export project to stream scm authorized #scmAuth"() {
        given:

            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [:]
            }

            File temp = File.createTempFile("test", "zip")


            def projectProps = new Properties()
            projectProps.setProperty('project.name', 'aProject')
            def project = Mock(IRundeckProject){
                getName()>>'aProject'
                getProjectProperties()>>projectProps
            }
            def framework = Mock(IFramework)
            def output = new ZipOutputStream(temp.newOutputStream())
            def options = Mock(ProjectArchiveExportRequest){
                isAll() >> false
                isScm()>>true
            }
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener)
            service.rundeckAuthContextEvaluator = Mock(BaseAuthContextEvaluator)
            service.scmService = Mock(ScmService)
        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            true
            1 * listener.total('export', count)
            (count) * listener.inc('export', 1)
            1 * listener.done()
            _ * service.rundeckAuthContextEvaluator.authResourceForProject('aProject') >> [test: 'resource']
            1 * service.rundeckAuthContextEvaluator.authorizeProjectConfigure(auth, 'aProject')>>scmAuth
            (count)*service.scmService.loadScmConfig('aProject','export')>>Mock(ScmPluginConfigData)
            (count)*service.scmService.loadScmConfig('aProject','import')>>Mock(ScmPluginConfigData)

        cleanup:
            temp.delete()
        where:
            scmAuth | count
            true    | 1
            false   | 0
    }


    @Unroll
    def "export project to stream acl authorized #aclAuth"() {
        given:

            service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
                Map<String, ProjectComponent> beans = [:]
            }

            File temp = File.createTempFile("test", "zip")


            def projectProps = new Properties()
            projectProps.setProperty('project.name', 'aProject')
            def project = Mock(IRundeckProject){
                getName()>>'aProject'
                getProjectProperties()>>projectProps
                0 * listDirPaths(_)
                0 * loadFileResource(*_)
            }
            def framework = Mock(IFramework)
            def output = new ZipOutputStream(temp.newOutputStream())
            def options = Mock(ProjectArchiveExportRequest){
                isAll() >> false
                isAcls()>>true
            }
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener)
            service.rundeckAuthContextEvaluator = Mock(BaseAuthContextEvaluator)
            service.scmService = Mock(ScmService)
            service.aclFileManagerService=Mock(AclFileManagerService)
        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            true
            1 * listener.total('export', count)
            (count) * listener.inc('export', 1)
            1 * listener.done()
            (count) * service.aclFileManagerService.forContext(AppACLContext.project('aProject')) >>
            Mock(ACLFileManager) {
                (count) * listStoredPolicyFiles() >> ['test.aclpolicy']
                (count) * loadPolicyFileContents('test.aclpolicy', _) >> {
                    it[1] << 'acl data'
                    'acl data'.bytes.length
                }
            }
            _ * service.rundeckAuthContextEvaluator.authResourceForProject('aProject') >> [test: 'resource']
            1 * service.rundeckAuthContextEvaluator.authResourceForProjectAcl('aProject') >> [test2: 'resource']
            1 * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, [test2: 'resource'], [AuthConstants.ACTION_READ,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN])>> aclAuth

        cleanup:
            temp.delete()
        where:
            aclAuth | count
            true    | 1
            false   | 0
    }

    def "should result success and no error msgs when export to another instance is success"() {
        given:
        ProjectArchiveParams exportArchiveParams = new ProjectArchiveParams([
                targetproject:'project-target',
                project : 'project-target',
                preserveuuid : 'preserve',
                exportExecutions : true,
                exportConfigs : true,
                exportAcls : true,
                exportScm : true,
                exportComponents : [
                        'webhooks' : true,
                        'node-wizard' : true
                ],
                exportOpts : [
                        'webhooks' : ['inludeAuthTokens' : 'true', 'regenUuid':'true']
                ],
        ])
        File archive = new File("testfile")
        MockRundeckApi mockRundeckApi = (MockRundeckApi) Spy(MockRundeckApi, constructorArgs: [true, 200])

        when:
        ProjectImportStatus response = ProjectService.importArchiveToInstance(archive, exportArchiveParams, mockRundeckApi.getRundeckClient())

        then:
        assertTrue(response.getResultSuccess())
        0 * ProjectService.projectLogger.error(_)
    }

    def "should log acl error when fails to export acls to another instance"() {
        given:
        ProjectService.projectLogger = Mock(Logger)

        ProjectArchiveParams exportArchiveParams = new ProjectArchiveParams([
                project : 'project-target',
                targetproject:'project-target',
                preserveuuid : 'preserve',
                exportExecutions : true,
                exportConfigs : true,
                exportAcls : true,
                exportScm : true,
                exportComponents : [
                        'webhooks' : true,
                        'node-wizard' : true
                ],
                exportOpts : [
                        'webhooks' : ['inludeAuthTokens' : 'true', 'regenUuid':'true']
                ],
        ])
        File archive = new File("testfile")
        MockRundeckApi mockRundeckApi = (MockRundeckApi) Spy(MockRundeckApi, constructorArgs: [false, 200])

        when:
        ProjectImportStatus response = ProjectService.importArchiveToInstance(archive, exportArchiveParams, mockRundeckApi.getRundeckClient())

        then:
        assertFalse(response.getResultSuccess())
        1 * ProjectService.projectLogger.error("Error on import acls to new project: 1")
    }

    def "should throw couldn't create resource runtime exception when exporting to another instance request has 409 status code"() {
        given:
        ProjectArchiveParams exportArchiveParams = new ProjectArchiveParams([
                project : 'TARGETprojectName',
                preserveuuid : 'preserve',
                exportExecutions : true,
                exportConfigs : true,
                exportAcls : true,
                exportScm : true,
                exportComponents : [
                        'webhooks' : true,
                        'node-wizard' : true
                ],
                exportOpts : [
                        'webhooks' : ['inludeAuthTokens' : 'true', 'regenUuid':'true']
                ],
        ])
        File archive = new File("testfile")
        MockRundeckApi mockRundeckApi = (MockRundeckApi) Spy(MockRundeckApi, constructorArgs: [false, errorStatusCode])

        when:
        ProjectService.importArchiveToInstance(archive, exportArchiveParams, mockRundeckApi.getRundeckClient())

        then:
        def e = thrown(RuntimeException)
        e.message.contains(expectedThrownMessage)

        where:
        errorStatusCode | expectedThrownMessage
        409             | "Could not create resource"
    }

    def "should return a Map<string,string> with the same given keys but with an added desiredPrefix and the same value"() {
        given:
        ProjectArchiveParams exportArchiveParams = new ProjectArchiveParams([
                exportComponents : [
                        'webhooks' : true,
                        'node-wizard' : true,
                        'component-X' : true,
                        'component-Y' : false
                ]
        ])

        String desiredPrefix = 'importComponents'

        when:
        Map<String,String> transformedMap = ProjectService.prependStringToKeysInMap(desiredPrefix, exportArchiveParams.exportComponents)

        then:
        exportArchiveParams.exportComponents.each {originalKey, originalValue ->
            transformedMap[desiredPrefix + '.' + originalKey] == originalValue.toString()
        }
    }

    static String EXEC_UUID = "9f0509eb-f89c-4fb3-a520-edf5a34eb52f"
    static String EXECS_START='<executions>'
    static String EXECS_END= '</executions>'
    static String EXEC_XML_TEST1_DEF_START= '''
  <execution id='1'>
    <dateStarted>1970-01-01T00:00:00Z</dateStarted>
    <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
    <status>true</status>'''
    static String EXEC_XML_TEST1_DEF_END= '''
    <failedNodeList />
    <succeededNodeList />
    <abortedby />
    <cancelled>false</cancelled>
    <argString>-test args</argString>
    <loglevel>WARN</loglevel>
    <uuid>9f0509eb-f89c-4fb3-a520-edf5a34eb52f</uuid>
    <doNodedispatch>true</doNodedispatch>
    <nodefilters>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>false</keepgoing>
        <excludePrecedence>true</excludePrecedence>
        <rankOrder>ascending</rankOrder>
      </dispatch>
      <filter>hostname: test1 !tags: monkey</filter>
    </nodefilters>
    <project>testproj</project>
    <user>testuser</user>
    <workflow keepgoing='false' strategy='node-first'>
      <command>
        <exec>exec command</exec>
      </command>
    </workflow>
  </execution>
'''

    static String EXEC_XML_TEST1_START = EXECS_START+EXEC_XML_TEST1_DEF_START
    static String EXEC_XML_TEST1_REST = EXEC_XML_TEST1_DEF_END+EXECS_END
    static String EXEC_XML_TEST1 = EXEC_XML_TEST1_START+ '''
    <outputfilepath />
    <execIdForLogStore>1</execIdForLogStore>''' + EXEC_XML_TEST1_REST

    /**
     * Execution xml output with an output file path
     */
    static String EXEC_XML_TEST2 = EXEC_XML_TEST1_START+ '''
    <outputfilepath>output-1.rdlog</outputfilepath>
    <execIdForLogStore>1</execIdForLogStore>''' + EXEC_XML_TEST1_REST

    /**
     * Execution xml with associated job ID
     */
    static String EXEC_XML_TEST3 = EXEC_XML_TEST1_START + '''
    <outputfilepath />''' + '''
    <jobId>jobid1</jobId>''' + EXEC_XML_TEST1_REST
    /**
     * Execution xml with associated job ID
     */
    static String EXEC_XML_TEST4 = EXEC_XML_TEST1_START + '''
    <outputfilepath>output-1.rdlog</outputfilepath>''' + '''
    <failedNodeList />
    <succeededNodeList />
    <abortedby />
    <cancelled>false</cancelled>
    <argString>-test args</argString>
    <loglevel>WARN</loglevel>
    <doNodedispatch>true</doNodedispatch>
    <nodefilters>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>false</keepgoing>
        <excludePrecedence>true</excludePrecedence>
        <rankOrder>ascending</rankOrder>
      </dispatch>
      <filter>hostname: test1 !tags: monkey</filter>
    </nodefilters>
    <project>testproj</project>
    <user>testuser</user>
    <workflow keepgoing='false' strategy='node-first'>
      <command>
        <jobref name='echo' nodeStep='true'>
          <arg line='-name ${node.name}' />
        </jobref>
        <description>echo on node</description>
      </command>
    </workflow>
  </execution>
</executions>''' /**
     * Execution xml with orchestrator
     */
    static String EXEC_XML_TEST5 = EXEC_XML_TEST1_START + '''
    <outputfilepath>output-1.rdlog</outputfilepath>''' + '''
    <failedNodeList />
    <succeededNodeList />
    <abortedby />
    <cancelled>false</cancelled>
    <argString>-test args</argString>
    <loglevel>WARN</loglevel>
    <doNodedispatch>true</doNodedispatch>
    <nodefilters>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>false</keepgoing>
        <excludePrecedence>true</excludePrecedence>
        <rankOrder>ascending</rankOrder>
      </dispatch>
      <filter>hostname: test1 !tags: monkey</filter>
    </nodefilters>
    <project>testproj</project>
    <user>testuser</user>
    <workflow keepgoing='false' strategy='node-first'>
      <command>
        <jobref name='echo' nodeStep='true'>
          <arg line='-name ${node.name}' />
        </jobref>
        <description>echo on node</description>
      </command>
    </workflow>

    <orchestrator>
      <type>subset</type>
      <configuration>
        <count>1</count>
      </configuration>
    </orchestrator>
  </execution>
</executions>'''

    static String EXEC_XML_TEST6 ='''<executions>
  <execution id='1'>
    <jobId>1</jobId>
    <dateStarted>1970-01-01T00:00:00Z</dateStarted>
    <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
    <status>true</status>
    <outputfilepath />
    <execIdForLogStore>1</execIdForLogStore>
    <failedNodeList />
    <succeededNodeList />
    <abortedby />
    <cancelled>false</cancelled>
    <argString>-test args</argString>
    <loglevel>WARN</loglevel>
    <uuid>9f0509eb-f89c-4fb3-a520-edf5a34eb52f</uuid>
    <doNodedispatch>true</doNodedispatch>
    <nodefilters>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>false</keepgoing>
        <excludePrecedence>true</excludePrecedence>
        <rankOrder>ascending</rankOrder>
      </dispatch>
      <filter>hostname: test1 !tags: monkey</filter>
    </nodefilters>
    <project>testproj</project>
    <user>testuser</user>
    <workflow keepgoing='false' strategy='node-first'>
      <command>
        <exec>exec command</exec>
      </command>
    </workflow>
  </execution>
</executions>'''

    static String EXEC_XML_TEST7 ='''<executions>
  <execution id='1'>
    <jobId>1</jobId>
    <dateStarted>1970-01-01T00:00:00Z</dateStarted>
    <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
    <status>true</status>
    <outputfilepath />
    <execIdForLogStore>1</execIdForLogStore>
    <failedNodeList />
    <succeededNodeList />
    <abortedby />
    <cancelled>false</cancelled>
    <argString>-test args</argString>
    <loglevel>WARN</loglevel>
    <uuid>9f0509eb-f89c-4fb3-a520-edf5a34eb52f</uuid>
    <doNodedispatch>true</doNodedispatch>
    <nodefilters>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>false</keepgoing>
        <excludePrecedence>true</excludePrecedence>
        <rankOrder>ascending</rankOrder>
      </dispatch>
      <filter>hostname: test1 !tags: monkey</filter>
    </nodefilters>
    <project>testproj</project>
    <user>testuser</user>
    <workflow keepgoing='false' strategy='node-first'>
      <command>
        <exec>exec command</exec>
      </command>
    </workflow>
    <fullJob>
      <scheduleEnabled>true</scheduleEnabled>
      <executionEnabled>true</executionEnabled>
      <sequence keepgoing='true' strategy='node-first'>
        <command>
          <exec>exec command</exec>
        </command>
      </sequence>
      <loglevel>WARN</loglevel>
      <name>blue</name>
      <nodeFilterEditable>false</nodeFilterEditable>
      <description>a job</description>
      <id>1</id>
      <retry>1</retry>
      <group>some/where</group>
    </fullJob>
  </execution>
</executions>'''
    def testExportExecution(){
        given:
        def outfilename = "blahfile.xml"

        def zipmock=new MockFor(ZipBuilder)
        def outwriter = new StringWriter()
        zipmock.demand.file(1..1){name,Closure withwriter->
            assertEquals(outfilename,name.toString())
            withwriter.call(outwriter)
            outwriter.flush()
        }
//        zipmock.demand.file(1..1){name,File outfile-> }
        def zip = zipmock.proxyInstance()
        Execution exec = new Execution(
            uuid: EXEC_UUID,
            argString: "-test args",
            user: "testuser",
            project: "testproj",
            loglevel: 'WARN',
            doNodedispatch: true,
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            nodeInclude: 'test1',
            nodeExcludeTags: 'monkey',
            status: 'true',
            workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'exec command')])
        )
        assertNotNull exec.save()
        def logmock = new MockFor(LoggingService)
        logmock.demand.getLogFileForExecution(1..1){Execution e->
            assert exec==e
            new File(outfilename)
        }
        service.loggingService=logmock.proxyInstance()
        def workflowmock = new MockFor(WorkflowService)
        workflowmock.demand.getStateFileForExecution(1..1){Execution e->
            assert exec==e
            null
        }
        service.workflowService= workflowmock.proxyInstance()

        service.executionUtilService = new ExecutionUtilService()
        service.executionUtilService.configurationService=Mock(ConfigurationService){
            getBoolean('execution.logs.fileStorage.generateExecutionXml',_)>>true
        }
        when:
        service.exportExecution(zip,exec,outfilename)
        def str=outwriter.toString()
//        println str
        then:
        assertEquals EXEC_XML_TEST1, str
    }
    def  testExportExecutionOutputFile(){
        given:
        def outfilename = "blahfile.xml"
        File tempoutfile = File.createTempFile("tempout",".txt")
        tempoutfile.deleteOnExit()

        def zipmock=new MockFor(ZipBuilder)
        def outwriter = new StringWriter()

        Execution exec = new Execution(
            uuid: EXEC_UUID,
            argString: "-test args",
            user: "testuser",
            project: "testproj",
            loglevel: 'WARN',
            doNodedispatch: true,
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            nodeInclude: 'test1',
            nodeExcludeTags: 'monkey',
            status: 'true',
            outputfilepath: tempoutfile.absolutePath,
            workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'exec command')])
        )
        assertNotNull exec.save()

        zipmock.demand.file(1..1) {name, File out ->
            assertEquals('output-'+exec.id+'.rdlog', name)
            assertEquals(tempoutfile,out)
        }
        zipmock.demand.file(1..1){name,Closure withwriter->
            assertEquals(outfilename,name.toString())
            withwriter.call(outwriter)
            outwriter.flush()
        }

        def zip = zipmock.proxyInstance()

        def logmock = new MockFor(LoggingService)
        logmock.demand.getLogFileForExecution(1..1) { Execution e ->
            assert exec == e
            tempoutfile
        }
        service.loggingService = logmock.proxyInstance()
        service.executionUtilService = new ExecutionUtilService()
        service.executionUtilService.configurationService=Mock(ConfigurationService){
            getBoolean('execution.logs.fileStorage.generateExecutionXml',_)>>true
        }
        def workflowmock = new MockFor(WorkflowService)
        workflowmock.demand.getStateFileForExecution(1..1) { Execution e ->
            assert exec == e
            null
        }

        service.workflowService = workflowmock.proxyInstance()
        when:
        service.exportExecution(zip,exec,outfilename)
        def str=outwriter.toString()
//        println str
        then:
        assertEquals EXEC_XML_TEST2, str
    }
    def testExportExecutionStateFile(){
        given:

        def outfilename = "blahfile.xml"
        File tempoutfile = File.createTempFile("tempout",".txt")
        tempoutfile.deleteOnExit()
        File tempoutfile2 = File.createTempFile("tempout",".state.json")
        tempoutfile2.deleteOnExit()

        def zipmock=new MockFor(ZipBuilder)
        def outwriter = new StringWriter()

        Execution exec = new Execution(
            uuid: EXEC_UUID,
            argString: "-test args",
            user: "testuser",
            project: "testproj",
            loglevel: 'WARN',
            doNodedispatch: true,
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            nodeInclude: 'test1',
            nodeExcludeTags: 'monkey',
            status: 'true',
            outputfilepath: tempoutfile.absolutePath,
            workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'exec command')])
        )
        assertNotNull exec.save()
        int filecalled=0
        Closure validateZipCall = { name, File out ->
            filecalled++
            if(filecalled==1){
                assertEquals('output-'+exec.id+'.rdlog', name.toString())
                assertEquals(tempoutfile,out)
            }else{
                assertEquals('state-' + exec.id + '.state.json', name.toString())
                assertEquals(tempoutfile2, out)
            }
        }
        zipmock.demand.file(1..1, validateZipCall)
        zipmock.demand.file(1..1){name,Closure withwriter->
            assertEquals(outfilename,name.toString())
            withwriter.call(outwriter)
            outwriter.flush()
        }
        zipmock.demand.file(1..1, validateZipCall)

        def zip = zipmock.proxyInstance()

        def logmock = new MockFor(LoggingService)
        logmock.demand.getLogFileForExecution(1..1) { Execution e ->
            assert exec == e
            tempoutfile
        }
        service.loggingService = logmock.proxyInstance()
        def workflowmock = new MockFor(WorkflowService)
        workflowmock.demand.getStateFileForExecution(1..1) { Execution e ->
            assert exec == e
            tempoutfile2
        }
        service.workflowService = workflowmock.proxyInstance()
        service.executionUtilService = new ExecutionUtilService()
        service.executionUtilService.configurationService=Mock(ConfigurationService){
            getBoolean('execution.logs.fileStorage.generateExecutionXml',_)>>true
        }

        when:
        service.exportExecution(zip,exec,outfilename)
        def str=outwriter.toString()
//        println str
        then:
        assertEquals(2, filecalled)
        assertEquals EXEC_XML_TEST2, str
    }
    def testImportExecution(){
        when:
        def result = service.loadExecutions(EXEC_XML_TEST1,'AProject')
        then:
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertEquals 1,result.executions.size()
        def Execution e = result.executions[0]
        def expected = [
            argString: '-test args',
            user: 'testuser',
            project: 'testproj',
            loglevel: 'WARN',
            doNodedispatch: true,
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            filter: 'hostname: test1 !tags: monkey',
            status: 'true',
        ]
        assertPropertiesEquals expected,e
        assertEquals e,e
        assertEquals 1,result.execidmap.size()
        assertEquals e,result.execidmap.keySet().first()
        assertEquals 1,result.execidmap.values().first()
        assertEquals( [(e):1],result.execidmap)
        WorkflowData workflowData = e.getWorkflowData()
        assertNotNull workflowData
        assertNotNull workflowData.commands
        assertEquals 1,workflowData.commands.size()
        assertPropertiesEquals( [adhocRemoteString: 'exec command'],workflowData.commands[0].configuration)
    }
    def testLoadExecutionsWorkflow(){
        when:
        def result = service.loadExecutions(EXEC_XML_TEST4,'AProject')
        then:
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertEquals 1,result.executions.size()
        def Execution e = result.executions[0]
        WorkflowData workflowData = e.getWorkflowData()
        assertNotNull workflowData
        assertNotNull workflowData.commands
        assertEquals 1,workflowData.commands.size()
        assertPropertiesEquals( [jobName: 'echo', nodeStep:true,argString: '-name ${node.name}',
                                 description: 'echo on node'],
                                workflowData.commands[0])
    }
    /**
     * load execution xml with orchestrator definition
     */
    def testLoadExecutionsOrchestrator(){

        when:

        def result = service.loadExecutions(EXEC_XML_TEST5,'AProject')
        then:
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertEquals 1,result.executions.size()
        def Execution e = result.executions[0]

        assertNotNull e.orchestrator
        assertEquals  'subset',e.orchestrator.type
        assertEquals( [count:"1"],e.orchestrator.configuration)
    }
    /**
     * Imported execution where jobId should be skipped, should not be loaded
     */
    def testImportExecutionSkipJob(){
        when:
        def result = service.loadExecutions(EXEC_XML_TEST3,'AProject',null,['jobid1'])
        then:
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertEquals 0,result.executions.size()
        assertEquals 0,result.execidmap.size()
    }
    def testImportExecutionRemappedJob(){
        given:
        def testJobId='test-id1'

        def newJobId = 'test-id2'
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                                                       uuid: newJobId,
                                                       adhocFilepath: '/this/is/a/path', groupPath: 'some/where',
                                                       description: 'a job', argString: '-a b -c d',
                                                       workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                                                       )
        assertNotNull se.save()
        def idMap=[(testJobId):newJobId]


        def semock = new MockFor(ScheduledExecutionService)
        semock.demand.getByIDorUUID(1..1){id->
            assertEquals(newJobId,id)
            se
        }

        service.scheduledExecutionService=semock.proxyInstance()

        when:
        def result = service.loadExecutions(EXEC_XML_TEST1_START+"<outputfilepath/><jobId>${testJobId}</jobId>"+EXEC_XML_TEST1_REST,'AProject',idMap)
        then:
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertEquals 1,result.executions.size()

        def Execution e = result.executions[0]
        def expected = [
            argString: '-test args',
            user: 'testuser',
            project: 'testproj',
            loglevel: 'WARN',
            doNodedispatch: true,
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            filter: 'hostname: test1 !tags: monkey',
            status: 'true',
        ]
        assertPropertiesEquals expected,e
        assertNotNull(e.scheduledExecution)
        assertEquals(se,e.scheduledExecution)
        assertEquals( [(e):1],result.execidmap)
        WorkflowData workflowData = e.getWorkflowData()
        assertNotNull workflowData
        assertNotNull workflowData.commands
        assertEquals 1,workflowData.commands.size()
        assertPropertiesEquals( [adhocRemoteString: 'exec command'],workflowData.commands[0].configuration)
    }
    /**
     * using job id that already exists will attach to that job
     */
    def testImportExecutionRetainJob(){
        def newJobId = 'test-id2'
        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            uuid: newJobId,
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(
                keepgoing: true,
                commands: [
                    new CommandExec(
                        adhocRemoteString: 'test buddy',
                        argString: '-delay 12 -monkey cheese -particle'
                    )
                ]
            )
        )
        assertNotNull se.save()
        def idMap = [:]


        def semock = new MockFor(ScheduledExecutionService)
        semock.demand.getByIDorUUID(1..1){id->
            assertEquals(newJobId,id)
            se
        }

        service.scheduledExecutionService=semock.proxyInstance()

        when:
        def result = service.loadExecutions(EXEC_XML_TEST1_START+"<outputfilepath/><jobId>${newJobId}</jobId>"+EXEC_XML_TEST1_REST,'AProject',idMap)
        then:
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertEquals 1,result.executions.size()

        def Execution e = result.executions[0]
        def expected = [
            argString: '-test args',
            user: 'testuser',
            project: 'testproj',
            loglevel: 'WARN',
            doNodedispatch: true,
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            filter: 'hostname: test1 !tags: monkey',
            status: 'true',
        ]
        assertPropertiesEquals expected,e
        assertNotNull(e.scheduledExecution)
        assertEquals(se,e.scheduledExecution)
        assertEquals( [(e):1],result.execidmap)
        WorkflowData workflowData = e.getWorkflowData()
        assertNotNull workflowData
        assertNotNull workflowData.commands
        assertEquals 1,workflowData.commands.size()
        assertPropertiesEquals( [adhocRemoteString: 'exec command'],workflowData.commands[0].configuration)
    }
    def testloadExecutionsRetryExecId(){
        def remapExecId='12'
        def idMap=[:]


        def semock = new MockFor(ScheduledExecutionService)
        semock.demand.getByIDorUUID(1..1){id->
            assertEquals(newJobId,id)
            se
        }


        service.scheduledExecutionService=semock.proxyInstance()
        when:
        def result = service.loadExecutions(
            EXECS_START
                + EXEC_XML_TEST1_DEF_START
                + '''<retryExecutionId>12</retryExecutionId> <outputfilepath />'''
                + EXEC_XML_TEST1_DEF_END
                + '''
  <execution id='12'>
    <dateStarted>1970-01-01T00:00:00Z</dateStarted>
    <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
    <status>true</status>'''
                + ''' <outputfilepath />'''
                + EXEC_XML_TEST1_DEF_END
                + EXECS_END,
            'AProject',
            idMap)
        then:
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertNotNull result.retryidmap
        assertEquals 1,result.retryidmap.size()
        assertEquals 12,result.retryidmap.values().first()
        assertEquals 2,result.executions.size()

    }
    public void  assertPropertiesEquals(Map data, Object obj){
        data.each{k,v->
            def test=obj[k]
            if(null==test){
                fail("key:'${k}' Expected value '${v}' of type ${v.class}, but value was null")
            }
            if(!(v.class.isAssignableFrom(test.class))){
                fail("key:'${k}' Expected value of type ${v.class}, but value was ${test.class}")
            }
            assert v==test, "unexpected value ${test} for key ${k}"
        }
    }

    static String REPORT_XML_TEST1='''<report>
  <node>1/0/0</node>
  <title>blah</title>
  <status>succeed</status>
  <actionType>succeed</actionType>
  <project>testproj1</project>
  <reportId>test/job</reportId>
  <tags>a,b,c</tags>
  <author>admin</author>
  <message>Report message</message>
  <dateStarted>1970-01-01T00:00:00Z</dateStarted>
  <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
  <executionId>123</executionId>
  <jobId>1</jobId>
  <adhocExecution />
  <adhocScript />
  <abortedByUser />
  <succeededNodeList />
  <failedNodeList />
  <filterApplied />
  <jobUuid>test-job-uuid</jobUuid>
  <executionUuid>uuid</executionUuid>
</report>'''
    /**
     * uses deprecated jcExecId
     */
    static String REPORT_XML_TEST1_DEPRECATED='''<report>
  <node>1/0/0</node>
  <title>blah</title>
  <status>succeed</status>
  <actionType>succeed</actionType>
  <ctxProject>testproj1</ctxProject>
  <reportId>test/job</reportId>
  <tags>a,b,c</tags>
  <author>admin</author>
  <message>Report message</message>
  <dateStarted>1970-01-01T00:00:00Z</dateStarted>
  <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
  <jcExecId>123</jcExecId>
  <jcJobId>test-job-uuid</jcJobId>
  <executionUuid>uuid</executionUuid>
  <adhocExecution />
  <adhocScript />
  <abortedByUser />
  <succeededNodeList />
  <failedNodeList />
  <filterApplied />
</report>'''
    def testExportReport() {

        def newJobId = 'test-job-uuid'
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                                                       uuid: newJobId,
                                                       adhocFilepath: '/this/is/a/path', groupPath: 'some/where', description: 'a job', argString: '-a b -c d',
                                                       workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                                                       )
        assertNotNull se.save()
        def oldJobId=se.id

        def outfilename = "reportout.xml"

        def zipmock = new MockFor(ZipBuilder)
        def outwriter = new StringWriter()
        zipmock.demand.file(1..1) {name, Closure withwriter ->
            assertEquals(outfilename, name.toString())
            withwriter.call(outwriter)
            outwriter.flush()
        }
        def zip = zipmock.proxyInstance()
        ExecReport exec = new ExecReport(
            executionId:123L,
            jobId: oldJobId.toString(),
            node:'1/0/0',
            title: 'blah',
            status: 'succeed',
            actionType: 'succeed',
            project: 'testproj1',
            reportId: 'test/job',
            tags: 'a,b,c',
            author: 'admin',
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            message: 'Report message',
            jobUuid: se.uuid,
            executionUuid: 'uuid'
            )
        assertNotNull exec.save()

        when:
        service.exportHistoryReport(zip, exec, outfilename)
        then:
        def str = outwriter.toString()
        println str
        assertEquals REPORT_XML_TEST1, str
    }

    def testLoadReport() {

        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                                                       uuid: 'new-job-uuid',
                                                       adhocFilepath: '/this/is/a/path', groupPath: 'some/where', description: 'a job', argString: '-a b -c d',
                                                       workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                                                       )
        assertNotNull se.save()
        def newJobId = se.id
        def oldUuid= 'test-job-uuid'

        when:
        def SaveReportRequestImpl result = service.loadHistoryReport(rptxml,[(123):456],[(oldUuid):se],'test')
        then:
        result!=null
        def expected = [
            executionId: 456L,
            jobId: newJobId.toString(),
            node: '1/0/0',
            title: 'blah',
            status: 'succeed',
            project: 'testproj1',
            reportId: 'test/job',
            tags: 'a,b,c',
            author: 'admin',
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            message: 'Report message',
        ]
        assertPropertiesEquals expected, result
        where:
            rptxml<<[
                REPORT_XML_TEST1,
                REPORT_XML_TEST1_DEPRECATED
            ]
    }
    def testLoadReportSkippedExecution() {

        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                                                       uuid: 'new-job-uuid',
                                                       adhocFilepath: '/this/is/a/path', groupPath: 'some/where', description: 'a job', argString: '-a b -c d',
                                                       workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                                                       )
        assertNotNull se.save()
        def newJobId = se.id
        def oldUuid= 'test-job-uuid'

        when:
        def ExecReport result = service.loadHistoryReport(rptxml,[:],[(oldUuid):se],'test')
        then:
        assertNull result
        where:
            rptxml<<[
                REPORT_XML_TEST1,
                REPORT_XML_TEST1_DEPRECATED
            ]
    }
    def testReportRoundtrip() {
        given:
        def outfilename = "reportout.xml"

        def zipmock = new MockFor(ZipBuilder)
        def outwriter = new StringWriter()
        zipmock.demand.file(1..1) {name, Closure withwriter ->
            assertEquals(outfilename, name.toString())
            withwriter.call(outwriter)
            outwriter.flush()
        }
        def zip = zipmock.proxyInstance()
        ExecReport exec = new ExecReport(
            ctxController: 'ct',
            executionId: 123,
            jobId: '321',
            node: '1/0/0',
            title: 'blah',
            status: 'succeed',
            actionType: 'succeed',
            project: 'testproj1',
            reportId: 'test/job',
            tags: 'a,b,c',
            author: 'admin',
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            message: 'Report message',
            executionUuid: 'uuid'
            )
        assertNotNull exec.save()


        service.exportHistoryReport(zip, exec, outfilename)
        def str = outwriter.toString()

        when:
        def SaveReportRequestImpl result = service.loadHistoryReport(str,[(123):123],null,'test')
        then:
        assertNotNull result
        def keys = [
            executionId: 456,
            jcJobId: 321,
            node: '1/0/0',
            title: 'blah',
            status: 'succeed',
            ctxProject: 'testproj1',
            reportId: 'test/job',
            tags: 'a,b,c',
            author: 'admin',
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            message: 'Report message',
        ].keySet()
        assertPropertiesEquals exec.properties.subMap(keys), result
    }

    def testReportNotUpdatedAfterExport() {
        def newJobId = 'test-job-uuid'
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                uuid: newJobId,
                adhocFilepath: '/this/is/a/path', groupPath: 'some/where', description: 'a job', argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        assertNotNull se.save()
        def oldJobId=se.id

        def outfilename = "reportout.xml"

        def zipmock = new MockFor(ZipBuilder)
        def outwriter = new StringWriter()
        zipmock.demand.file(1..1) {name, Closure withwriter ->
            assertEquals(outfilename, name.toString())
            withwriter.call(outwriter)
            outwriter.flush()
        }
        def zip = zipmock.proxyInstance()

        ExecReport exec = new ExecReport(
                executionId:123L,
                jobId: oldJobId.toString(),
                node:'1/0/0',
                title: 'blah',
                status: 'succeed',
                actionType: 'succeed',
                project: 'testproj1',
                reportId: 'test/job',
                tags: 'a,b,c',
                author: 'admin',
                dateStarted: new Date(0),
                dateCompleted: new Date(3600000),
                message: 'Report message',
                jobUuid: se.uuid,
                executionUuid: 'uuid'
        )
        assertNotNull exec.save(flush: true)
        when:
        service.exportHistoryReport(zip, exec, outfilename)
        then:
        def report = ExecReport.get(exec.id)
        assertNotNull report
        assertEquals report.jobUuid, exec.jobUuid
        assertEquals report.executionUuid, exec.executionUuid
        assertEquals report.jobId, exec.jobId

    }

    /**
     * empty archive progress meter
     */
    def testArchiveRequestProgressEmpty(){
        when:
        ArchiveRequestProgress svc = new ArchiveRequestProgress()
        then:
        assertEquals(0,svc.percent())
    }

    /**
     *  archive progress meter with 0 total for a key
     */
    def testArchiveRequestProgressZerocount(){
        given:
        ArchiveRequestProgress svc = new ArchiveRequestProgress()
        expect:
        svc.percent()==0
        when:
        svc.total("a",0)
        then:
        svc.percent()==100
        when:
        svc.inc("a",0)
        then:
        svc.percent()==100
        when:
        svc.inc("a",10)
        then:
        svc.percent()==100

    }

    /**
     * basic archive progress meter with single key
     */
    def testArchiveRequestProgressSingle(){
        given:
        ArchiveRequestProgress svc = new ArchiveRequestProgress()
        when:
        svc.total("a",10)
        then:
        svc.percent()==0
        when:
        svc.inc("a",5)
        then:
        svc.percent()==50
        when:
        svc.inc("a",5)
        then:
        svc.percent()==100
    }

    /**
     * archive progress meter with multiple keys
     */
    def testArchiveRequestProgressMulti(){
        given:
        ArchiveRequestProgress svc = new ArchiveRequestProgress()
        when:
        svc.total("a",10)
        svc.total("b",10)
        then:
        svc.percent()==0
        when:
        svc.inc("a",5)
        then:
        svc.percent()==25
        when:
        svc.inc("a",5)
        then:
        svc.percent()==50
        when:
        svc.inc("b",5)
        then:
        svc.percent()==75
        when:
        svc.inc("b",5)
        then:
        svc.percent()==100
    }
    /**
     * archive progress meter with multiple keys, some zero
     */
    def testArchiveRequestProgressMultiAndZero(){
        given:
        ArchiveRequestProgress svc = new ArchiveRequestProgress()
        when:
        svc.total("a",0)
        svc.total("b",10)
        then:
        svc.percent()==50
        when:
        svc.inc("a",5)
        then:
        svc.percent()==50
        when:
        svc.inc("a",5)
        then:
        svc.percent()==50
        when:
        svc.inc("b",5)
        then:
        svc.percent()==75
        when:
        svc.inc("b",5)
        then:
        svc.percent()==100
    }


    def testExportExecutionWithScheduledExecution(){
        given:
        def outfilename = "blahfile.xml"

        def zipmock=new MockFor(ZipBuilder)
        def outwriter = new StringWriter()
        zipmock.demand.file(1..1){name,Closure withwriter->
            assertEquals(outfilename,name.toString())
            withwriter.call(outwriter)
            outwriter.flush()
        }

        def zip = zipmock.proxyInstance()
        ScheduledExecution job = new ScheduledExecution(
            jobName: 'blue',
            project: 'testproj',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec(adhocRemoteString: 'exec command')]
            ),
            retry: '1'
        )
        assertNotNull job.save()

        Execution exec = new Execution(
            uuid: EXEC_UUID,
            argString: "-test args",
            user: "testuser",
            project: "testproj",
            loglevel: 'WARN',
            doNodedispatch: true,
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            nodeInclude: 'test1',
            nodeExcludeTags: 'monkey',
            status: 'true',
            workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'exec command')]),
            scheduledExecution: job
        )
        assertNotNull exec.save()
        def logmock = new MockFor(LoggingService)
        logmock.demand.getLogFileForExecution(1..1){Execution e->
            assert exec==e
            new File(outfilename)
        }
        service.loggingService=logmock.proxyInstance()
        def workflowmock = new MockFor(WorkflowService)
        workflowmock.demand.getStateFileForExecution(1..1){Execution e->
            assert exec==e
            null
        }
        service.workflowService= workflowmock.proxyInstance()

        service.executionUtilService = new ExecutionUtilService()
        service.executionUtilService.configurationService=Mock(ConfigurationService){
            getBoolean('execution.logs.fileStorage.generateExecutionXml',_)>>false
        }
        when:
        service.exportExecution(zip,exec,outfilename)
        def str=outwriter.toString()
        then:
        assertEquals EXEC_XML_TEST6, str
    }

    def "Handle API import with exception"(){
        given:
        def framework = Mock(IFramework)
        def auth = Mock(UserAndRolesAuthContext)
        def project = Mock(IRundeckProject){
            getName() >> "test"
        }
        def inputStream = Mock(FileInputStream)
        def params = Mock(ProjectArchiveParams){
            asyncImport >> true
        }
        service.asyncImportService = Mock(AsyncImportService){
            createStatusFile(project.name) >> false
        }

        when:
        def result = service.handleApiImport(
                framework,
                auth,
                project,
                inputStream,
                params
        )

        then:
        result.importerErrors != null
    }

    def "Handle API import with exception at import"(){
        given:
        def framework = Mock(IFramework)
        def auth = Mock(UserAndRolesAuthContext)
        def project = Mock(IRundeckProject){
            getName() >> "test"
        }
        def inputStream = Mock(FileInputStream)
        def params = Mock(ProjectArchiveParams){
            asyncImport >> true
        }
        service.asyncImportService = Mock(AsyncImportService){
            createStatusFile(project.name) >> true
            1 * startAsyncImport(
                    project.name,
                    _,
                    project,
                    _,
                    params
            ) >> { throw new AsyncImportException("A message") }
        }

        when:
        def result = service.handleApiImport(
                framework,
                auth,
                project,
                inputStream,
                params
        )

        then:
        result.importerErrors != null
    }

    def "restart async import calls to service for status file removal"(){
        given:
        def projectName = "test"
        service.asyncImportService = Mock(AsyncImportService){
            removeAsyncImportStatusFile(projectName) >> whenMethodReturns
        }

        when:
        def result = service.restartAsyncImport(projectName)
        flagWillBe = result

        then:
        1 * service.asyncImportService.removeAsyncImportStatusFile(projectName)

        where:
        whenMethodReturns | flagWillBe
        false             | false
        true              | true
    }

    def testExportExecutionWithScheduledExecutionBackupJobEnabled(){
        given:

        def outfilename = "blahfile.xml"

        def zipmock=new MockFor(ZipBuilder)
        def outwriter = new StringWriter()
        zipmock.demand.file(1..1){name,Closure withwriter->
            assertEquals(outfilename,name.toString())
            withwriter.call(outwriter)
            outwriter.flush()
        }

        def zip = zipmock.proxyInstance()
        ScheduledExecution job = new ScheduledExecution(
            jobName: 'blue',
            project: 'testproj',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec(adhocRemoteString: 'exec command')]
            ),
            retry: '1'
        )
        assertNotNull job.save()

        Execution exec = new Execution(
            uuid: EXEC_UUID,
            argString: "-test args",
            user: "testuser",
            project: "testproj",
            loglevel: 'WARN',
            doNodedispatch: true,
            dateStarted: new Date(0),
            dateCompleted: new Date(3600000),
            nodeInclude: 'test1',
            nodeExcludeTags: 'monkey',
            status: 'true',
            workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'exec command')]),
            scheduledExecution: job
        )
        assertNotNull exec.save()
        def logmock = new MockFor(LoggingService)
        logmock.demand.getLogFileForExecution(1..1){Execution e->
            assert exec==e
            new File(outfilename)
        }
        service.loggingService=logmock.proxyInstance()
        def workflowmock = new MockFor(WorkflowService)
        workflowmock.demand.getStateFileForExecution(1..1){Execution e->
            assert exec==e
            null
        }
        service.workflowService= workflowmock.proxyInstance()

        service.executionUtilService = new ExecutionUtilService()
        service.executionUtilService.configurationService=Mock(ConfigurationService){
            getBoolean('execution.logs.fileStorage.generateExecutionXml',_)>>true
        }
        service.executionUtilService.rundeckJobDefinitionManager = new RundeckJobDefinitionManager()

        when:
        service.exportExecution(zip,exec,outfilename)
        then:
        def str=outwriter.toString()
        assertEquals EXEC_XML_TEST7, str
    }

    def testProduceStorageFileForExecution(){
        given:
        Execution e = new Execution(argString: "-test args",
                                    user: "testuser", project: "p1", loglevel: 'WARN',
                                    doNodedispatch: false)

        assertNotNull(e.save())

        ProjectService svc = new ProjectService()
        File localFile = File.createTempFile("${e.id}.execution", ".xml")

        def logFileStorageServiceMock = new MockFor(LogFileStorageService)
        logFileStorageServiceMock.demand.getFileForExecutionFiletype(1..1){
            Execution e2, String filetype, boolean stored ->
                assertEquals(1, e2.id)
                assertEquals(ProjectService.EXECUTION_XML_LOG_FILETYPE, filetype)
                assertEquals(false, stored)
                return localFile
        }

        svc.logFileStorageService = logFileStorageServiceMock.proxyInstance()

        when:
        ProducedExecutionFile executionFile = svc.produceStorageFileForExecution(e.asReference())

        then:
        assertEquals(localFile, executionFile.localFile)
        assertEquals(ExecutionFile.DeletePolicy.ALWAYS, executionFile.fileDeletePolicy)
    }

    def "Async import events calls"(){
        given:
        def projectName = "test"
        def auth = Mock(UserAndRolesAuthContext)
        def project = Mock(IRundeckProject)
        def eventBusMock = Mock(EventBus)
        service.setTargetEventBus(eventBusMock)

        when: "we invoke the milestones events"
        service.beginAsyncImportMilestone(projectName, auth, project, AsyncImportMilestone.M2_DISTRIBUTION.milestoneNumber)

        then: "the event bus notifies"
        1 * eventBusMock.notify(*_)
    }

    def "Async import is called with exception"(){
        given:
        def projectName = "test"

        when:
        service.isIncompleteAsyncImportForProject(projectName)

        then:
        thrown AsyncImportException
    }

    def "Async import is called twice"(){
        given:
        def projectName = "test"
        def mockDto = new AsyncImportStatusDTO(projectName, AsyncImportMilestone.M2_DISTRIBUTION.milestoneNumber)
        service.asyncImportService = Mock(AsyncImportService){
            statusFileExists(projectName) >> true
            getAsyncImportStatusForProject(projectName) >> mockDto
        }

        when:
        def isIncomplete = service.isIncompleteAsyncImportForProject(projectName)

        then:
        isIncomplete
    }

    def "Async import is called when operation is complete"(){
        given:
        def projectName = "test"
        def mockDto = new AsyncImportStatusDTO(projectName, AsyncImportMilestone.ASYNC_IMPORT_COMPLETED.milestoneNumber)
        service.asyncImportService = Mock(AsyncImportService){
            statusFileExists(projectName) >> true
            getAsyncImportStatusForProject(projectName) >> mockDto
        }

        when:
        def isIncomplete = service.isIncompleteAsyncImportForProject(projectName)

        then:
        !isIncomplete
    }

    def "Async import events calls with a invalid milestone number"(){
        given:
        def projectName = "test"
        def auth = Mock(UserAndRolesAuthContext)
        def project = Mock(IRundeckProject)
        def eventBusMock = Mock(EventBus)
        service.setTargetEventBus(eventBusMock)

        when: "we invoke the milestones events"
        service.beginAsyncImportMilestone(projectName, auth, project, 999)

        then: "the event bus notifies"
        0 * eventBusMock.notify(*_)
        thrown AsyncImportException
    }

    def "Get async status file for project"(){
        given:
        def projectName = "test"
        def mockedStatus = new AsyncImportStatusDTO(projectName, AsyncImportMilestone.M1_CREATED.milestoneNumber).with {
            it.lastUpdate = "Its a mock!"
            return it
        }
        service.asyncImportService = Mock(AsyncImportService){
            it.getAsyncImportStatusForProject(projectName) >> mockedStatus
            it.statusFileExists(projectName) >> true
        }

        when:
        def status = service.getAsyncImportStatusFileForProject(projectName)

        then:
        status != null
    }

    def "Failed async status file for project retrieval"(){
        given:
        def projectName = "test"
        def mockedStatus = new AsyncImportStatusDTO(projectName, AsyncImportMilestone.M1_CREATED.milestoneNumber).with {
            it.lastUpdate = "Its a mock!"
            return it
        }
        service.asyncImportService = Mock(AsyncImportService){
            it.statusFileExists(projectName) >> true
            1 * it.getAsyncImportStatusForProject(projectName) >> { throw new AsyncImportException("Some error") }
        }

        when:
        def status = service.getAsyncImportStatusFileForProject(projectName)

        then:
        thrown AsyncImportException
    }

    def "Create async status file for project"(){
        given:
        def projectName = "test"
        service.asyncImportService = Mock(AsyncImportService){
            it.createStatusFile(projectName) >> true
        }

        when:
        def created = service.createAsyncImportStatusFile(projectName)

        then:
        created

    }

    def "call afterProjectCreate of projectComponents"() {
        given:
        ProjectComponent component1 = Mock(ProjectComponent){
            getName()>>'comp1'
            isComponentEnabled() >> true
        }
        ProjectComponent component2 = Mock(ProjectComponent){
            getName()>>'comp2'
            isComponentEnabled() >> true
        }
        service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
            Map<String, ProjectComponent> beans = [comp1: component1,comp2:component2]
        }

        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)

        def prjMgr = Mock(ProjectManager) {
            removeFrameworkProject(_) >> {}
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> { prjMgr }
        }

        when:
        service.afterCreationProjectComponents("myproject")

        then:
        1 * component1.afterProjectCreate('myproject')
        1 * component2.afterProjectCreate('myproject')
    }


    def "don't load project componets marked as disabled"() {
        given:
        ProjectComponent component1 = Mock(ProjectComponent){
            getName()>>'comp1'
            isComponentEnabled() >> true
        }
        ProjectComponent component2 = Mock(ProjectComponent){
            getName()>>'comp2'
            isComponentEnabled() >> false
        }
        service.componentBeanProvider=new ProjectService.BeanProvider<ProjectComponent>() {
            Map<String, ProjectComponent> beans = [comp1: component1,comp2:component2]
        }

        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.targetEventBus = Mock(EventBus)

        def prjMgr = Mock(ProjectManager) {
            removeFrameworkProject(_) >> {}
        }
        def fwk = Mock(Framework) {
            getFrameworkProjectMgr() >> { prjMgr }
        }

        when:
        service.afterCreationProjectComponents("myproject")

        then:
        1 * component1.afterProjectCreate('myproject')
        0 * component2.afterProjectCreate('myproject')
    }
}

abstract class MockRundeckApi implements RundeckApi{
    private boolean importSuccess
    private int responseStatusCode
    private Retrofit retrofit
    private MockRetrofit mockRetrofit

    private static final errorResponseJsonBody = ResponseBody.create(MediaType.parse("application/json") ,"{\"result\": {\"error\": {\"message\": \"error\"}}}")

    final BehaviorDelegate<RundeckApi> delegate;

    MockRundeckApi(Boolean importSuccess, Integer responseStatusCode){
        this.responseStatusCode = responseStatusCode
        this.importSuccess = importSuccess
        setupRetrofit()
        this.delegate = mockRetrofit.create(RundeckApi.class)
    }


    RundeckClient getRundeckClient(){
        return new RundeckClient(this, retrofit)
    }

    private void setupRetrofit(){
        this.retrofit = new Retrofit.Builder()
                .baseUrl("http://mock.url")
                .client(new OkHttpClient.Builder().build())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        NetworkBehavior behavior =  NetworkBehavior.create()
        behavior.setFailurePercent(0);
        mockRetrofit = new MockRetrofit.Builder(retrofit)
                .networkBehavior(behavior)
                .build();
    }

    Call<ProjectImportStatus> importProjectArchive(
            String project,
            String jobUuidOption,
            Boolean importExecutions,
            Boolean importConfig,
            Boolean importACL,
            Boolean importScm,
            Boolean importWebhooks,
            Boolean whkRegenAuthTokens,
            Boolean whkRegenUuid,
            Boolean importNodesSources,
            Map<String,String> params,
            RequestBody body
    ){
        Response response
        if(responseStatusCode == 200){
            ProjectImportStatus respBody = new ProjectImportStatus();
            respBody.successful = this.importSuccess;
            if(!importSuccess){
                respBody.aclErrors = new ArrayList<>();
                respBody.aclErrors.add("Error");
            }
            response = Response.success(respBody)
        }else {
            response = Response.error(responseStatusCode, errorResponseJsonBody)
        }

        return delegate.returning(Calls.response(response)).importProjectArchive(project,jobUuidOption, importExecutions, importConfig, importACL, importScm, importWebhooks, whkRegenAuthTokens,whkRegenUuid, importNodesSources, params, body)
    }
}
