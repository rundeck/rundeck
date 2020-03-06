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
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.util.ZipBuilder
import grails.events.bus.EventBus
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import org.rundeck.app.authorization.RundeckAuthContextEvaluator
import org.rundeck.app.components.project.BuiltinExportComponents
import org.rundeck.app.components.project.BuiltinImportComponents
import org.rundeck.app.components.project.ProjectComponent
import org.rundeck.app.components.RundeckJobDefinitionManager
import rundeck.BaseReport
import rundeck.CommandExec
import rundeck.ExecReport
import rundeck.Execution
import rundeck.Project
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.codecs.JobsXMLCodec
import rundeck.services.authorization.PoliciesValidation
import rundeck.services.scm.ScmPluginConfigData
import spock.lang.Specification
import spock.lang.Unroll

import java.util.jar.JarOutputStream
import java.util.zip.ZipOutputStream

/**
 * Created by greg on 8/5/15.
 */
class ProjectServiceSpec extends Specification implements ServiceUnitTest<ProjectService>, GrailsWebUnitTest, DataTest {

    def setup() {
        mockDomain Project
        mockDomain BaseReport
        mockDomain ExecReport
        mockDomain ScheduledExecution
        mockDomain Execution
        mockDomain CommandExec
        mockCodec JobsXMLCodec
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
            getName()>>'myproject'
        }
        service.authorizationService=Mock(AuthorizationService){
            1 * validateYamlPolicy('myproject','files/acls/test.aclpolicy',_) >> Mock(PoliciesValidation){
                isValid()>>true
            }
            1 * validateYamlPolicy('myproject','files/acls/test2.aclpolicy',_) >> Mock(PoliciesValidation){
                isValid()>>true
            }
            0 * _(*_)
        }

        when:
        def result=service.importProjectACLPolicies(policyfiles,project)

        then:
        result==[]
        1 * project.storeFileResource('acls/test.aclpolicy',{it.text=='file1'})
        1 * project.storeFileResource('acls/test2.aclpolicy',{it.text=='file2'})
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
        service.authorizationService=Mock(AuthorizationService){
            1 * validateYamlPolicy('myproject','files/acls/test.aclpolicy',_) >> Mock(PoliciesValidation){
                isValid()>>false
                getErrors()>>['blah':['blah']]
                toString()>>'test validation failure'
            }
            1 * validateYamlPolicy('myproject','files/acls/test2.aclpolicy',_) >> Mock(PoliciesValidation){
                isValid()>>true
            }
            0 * _(*_)
        }

        when:
        def result=service.importProjectACLPolicies(policyfiles,project)

        then:
        0 * project.storeFileResource('acls/test.aclpolicy',{it.text=='file1'})
        1 * project.storeFileResource('acls/test2.aclpolicy',{it.text=='file2'})
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
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)

        def fwk = Mock(Framework)

        when:
        def result = service.deleteProject(project, fwk, null, null)


        then:
        1 * service.scmService.removeAllPluginConfiguration('myproject')
        1 * service.executionService.deleteBulkExecutionIds(*_)
        1 * fwk.getFrameworkProjectMgr() >> Mock(ProjectManager) {
            1 * removeFrameworkProject('myproject')
        }
        1 * service.fileUploadService.deleteRecordsForProject('myproject')
        result.success

    }

    def "delete project notifies event bus success"() {
        given:
            def project = Mock(IRundeckProject) {
                getName() >> 'myproject'
            }
            service.scmService = Mock(ScmService)
            service.executionService = Mock(ExecutionService)
            service.fileUploadService = Mock(FileUploadService)
            service.targetEventBus = Mock(EventBus)
            def fwk = Mock(Framework)

        when:
            def result = service.deleteProject(project, fwk, null, null)

        then:
            1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
            1 * service.eventBus.notify('projectWasDeleted', ['myproject'])
            1 * fwk.getFrameworkProjectMgr() >> Mock(ProjectManager) {
                1 * removeFrameworkProject('myproject')
            }
            result.success
    }
    def "delete project notifies event bus failure"() {
        given:
            def project = Mock(IRundeckProject) {
                getName() >> 'myproject'
            }
            service.scmService = Mock(ScmService)
            service.executionService = Mock(ExecutionService)
            service.fileUploadService = Mock(FileUploadService){
                deleteRecordsForProject(_)>>{throw new Exception("test exception")}
            }
            service.targetEventBus = Mock(EventBus)
            def fwk = Mock(Framework)

        when:
            def result = service.deleteProject(project, fwk, null, null)

        then:
            1 * service.eventBus.notify('projectWillBeDeleted', ['myproject'])
            1 * service.eventBus.notify('projectDeleteFailed', ['myproject'])
            0 * fwk.getFrameworkProjectMgr()
            !result.success
    }
    def "delete project calls component projectDelete"() {
        given:
        ProjectComponent component1 = Mock(ProjectComponent){
            getName()>>'comp1'
        }
        ProjectComponent component2 = Mock(ProjectComponent){
            getName()>>'comp2'
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

    def "import project archive does not fail when webhooks are enabled but project archive has no webhook defs"() {
        setup:

        ProjectComponent component = Mock(ProjectComponent){
            getName()>>'webhooks'
            getImportFilePatterns()>>['webhooks.yaml']
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
        service.rundeckAuthContextEvaluator=Mock(AuthContextEvaluator){

        }

        ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest){
            getProject()>>'importtest'
            getImportConfig()>>true
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
            service.rundeckAuthContextEvaluator=Mock(AuthContextEvaluator){

            }
            ProjectArchiveImportRequest rq = Mock(ProjectArchiveImportRequest) {
                getProject() >> 'importtest'
                getImportConfig() >> true
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
            ProjectComponent component = Mock(ProjectComponent)
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
            service.rundeckAuthContextEvaluator=Mock(AuthContextEvaluator){

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

            ProjectComponent component = Mock(ProjectComponent)
            ProjectComponent component2 = Mock(ProjectComponent)
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
            service.rundeckAuthContextEvaluator=Mock(AuthContextEvaluator){

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
            ProjectComponent component = Mock(ProjectComponent)
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
            service.rundeckAuthContextEvaluator=Mock(AuthContextEvaluator){

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
            }
            jarStream.close()
            component.getImportFilePatterns() >> ['webhooks.yaml']
            component.getName() >> 'webhooks'

        when:
            def result = tempfile2.withInputStream { service.importToProject(project, framework, authCtx, it, rq) }

        then:
            result

            0 * component.doImport(_, _, { it.containsKey('webhooks.yaml') }, [some: 'thing']) >> []

        cleanup:
            tempfile2.delete()
    }
    def "import project archive with component unauthorized"() {
        setup:
            ProjectComponent component = Mock(ProjectComponent)
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
            service.rundeckAuthContextEvaluator=Mock(AuthContextEvaluator){
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
            }
            jarStream.close()
            component.getImportFilePatterns() >> ['webhooks.yaml']
            component.getName() >> 'webhooks'

        when:
            def result = tempfile2.withInputStream { service.importToProject(project, framework, authCtx, it, rq) }

        then:
            result

            0 * component.doImport(_, _, { it.containsKey('webhooks.yaml') }, [some: 'thing']) >> []

        cleanup:
            tempfile2.delete()
    }

    @Unroll
    def "import project archive with component with matching pattern #pattern"() {
        setup:
            ProjectComponent component = Mock(ProjectComponent)
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
            service.rundeckAuthContextEvaluator=Mock(AuthContextEvaluator){

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
            service.rundeckAuthContextEvaluator = Mock(RundeckAuthContextEvaluator)
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
    def "component export project to stream"() {
        given:
            ProjectComponent component = Mock(ProjectComponent)
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
            service.rundeckAuthContextEvaluator = Mock(RundeckAuthContextEvaluator)
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
            ProjectComponent component = Mock(ProjectComponent)
            ProjectComponent component2 = Mock(ProjectComponent)

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
            service.rundeckAuthContextEvaluator = Mock(RundeckAuthContextEvaluator)

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
            ProjectComponent component = Mock(ProjectComponent)
            ProjectComponent component2 = Mock(ProjectComponent)
            ProjectComponent component3 = Mock(ProjectComponent)

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
            service.rundeckAuthContextEvaluator = Mock(RundeckAuthContextEvaluator)

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
            ProjectComponent component = Mock(ProjectComponent)
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
            service.rundeckAuthContextEvaluator = Mock(RundeckAuthContextEvaluator)
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
            ProjectComponent component = Mock(ProjectComponent)
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
            service.rundeckAuthContextEvaluator = Mock(RundeckAuthContextEvaluator)
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
            service.rundeckAuthContextEvaluator = Mock(RundeckAuthContextEvaluator)
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
            ProjectComponent component = Mock(ProjectComponent)
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
            service.rundeckAuthContextEvaluator = Mock(RundeckAuthContextEvaluator)
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
            service.rundeckAuthContextEvaluator = Mock(RundeckAuthContextEvaluator)
            service.scmService = Mock(ScmService)
        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            true
            1 * listener.total('export', count)
            (count) * listener.inc('export', 1)
            1 * listener.done()
            _ * service.rundeckAuthContextEvaluator.authResourceForProject('aProject') >> [test: 'resource']
            1 * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAll(auth, [test: 'resource'], ['configure','admin'])>>scmAuth
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
                (count) * listDirPaths('acls/')>>['acls/test.aclpolicy']
                (count) * loadFileResource('acls/test.aclpolicy',_)>>{
                    it[1]<<'acl data'
                    'acl data'.bytes.length
                }
            }
            def framework = Mock(IFramework)
            def output = new ZipOutputStream(temp.newOutputStream())
            def options = Mock(ProjectArchiveExportRequest){
                isAll() >> false
                isAcls()>>true
            }
            def auth = Mock(AuthContext)
            def listener = Mock(ProgressListener)
            service.rundeckAuthContextEvaluator = Mock(RundeckAuthContextEvaluator)
            service.scmService = Mock(ScmService)
        when:
            service.exportProjectToStream(project, framework, output, listener, options, auth)
        then:
            true
            1 * listener.total('export', count)
            (count) * listener.inc('export', 1)
            1 * listener.done()
            _ * service.rundeckAuthContextEvaluator.authResourceForProject('aProject') >> [test: 'resource']
            1 * service.rundeckAuthContextEvaluator.authResourceForProjectAcl('aProject') >> [test2: 'resource']
            1 * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, [test2: 'resource'], ['read','admin'])>>aclAuth

        cleanup:
            temp.delete()
        where:
            aclAuth | count
            true    | 1
            false   | 0
    }
}
