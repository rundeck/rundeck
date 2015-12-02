package rundeck.services

import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProjectMgr
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.mock.interceptor.MockFor
import rundeck.BaseReport
import rundeck.ExecReport
import rundeck.Execution
import rundeck.Project
import rundeck.ScheduledExecution
import spock.lang.Specification

/**
 * Created by greg on 8/5/15.
 */
@TestFor(ProjectService)
@Mock([Project,BaseReport,ExecReport,ScheduledExecution,Execution])
class ProjectServiceSpec extends Specification {
    def "importProjectConfig"(){
        given:
        def tempfile = File.createTempFile("test-importProjectConfig",".properties")
        def origprops = [a:'b',x:'%PROJECT_BASEDIR%/sub/path/file.txt'] as Properties
        tempfile.withOutputStream {
            origprops.store(it,'test')
        }
        tempfile.deleteOnExit()
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
            1 * validateYamlPolicy('myproject','files/acls/test.aclpolicy',_) >> Mock(Validation){
                isValid()>>true
            }
            1 * validateYamlPolicy('myproject','files/acls/test2.aclpolicy',_) >> Mock(Validation){
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
            1 * validateYamlPolicy('myproject','files/acls/test.aclpolicy',_) >> Mock(Validation){
                isValid()>>false
                getErrors()>>['blah':['blah']]
                toString()>>'test validation failure'
            }
            1 * validateYamlPolicy('myproject','files/acls/test2.aclpolicy',_) >> Mock(Validation){
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
        def result = service.replacePlaceholderForProjectProperties(project, fwk, props, placeholder)

        then:
        result!=null
        result.x==after

        where:
        placeholder         | dir      | before                                | after
        '%PROJECT_BASEDIR%' | '/a/dir' | '/sub/path/file.txt'                  | '/sub/path/file.txt'
        '%PROJECT_BASEDIR%' | '/a/dir' | '%PROJECT_BASEDIR%/sub/path/file.txt' | '/a/dir/myproject/sub/path/file.txt'
        '%PROJECT_BASEDIR%' | '/a/dir' | '/sub/path/%PROJECT_BASEDIR%file.txt' | '/sub/path/%PROJECT_BASEDIR%file.txt'
        '%PROJECT_BASEDIR%' | '/a/dir' | '/sub/path/file.txt%PROJECT_BASEDIR%' | '/sub/path/file.txt%PROJECT_BASEDIR%'
    }

    def "replaceRelativePathsForProjectProperties"(){
        given:
        def project = Mock(IRundeckProject){
            getName()>>'myproject'
        }
        def fwk = Mock(Framework){
            1 * getFrameworkProjectsBaseDir() >> new File(dir)
        }

        def props = [x: before]

        when:
        def result = service.replaceRelativePathsForProjectProperties(project, fwk, props, placeholder)

        then:
        result!=null
        result.x==after

        where:
        placeholder         | dir      | before                               | after
        '%PROJECT_BASEDIR%' | '/a/dir' | '/sub/path/file.txt'                 | '/sub/path/file.txt'
        '%PROJECT_BASEDIR%' | '/a/dir' | '/a/dir/myproject/sub/path/file.txt' | '%PROJECT_BASEDIR%/sub/path/file.txt'
        '%PROJECT_BASEDIR%' | '/a/dir' | '/b/a/dir/sub/path/file.txt'         | '/b/a/dir/sub/path/file.txt'
    }

    def "delete project disables scm plugins"() {
        given:
        def project = Mock(IRundeckProject) {
            getName() >> 'myproject'
        }
        service.scmService = Mock(ScmService)
        service.executionService = Mock(ExecutionService)
        def fwk = Mock(Framework)

        when:
        def result = service.deleteProject(project, fwk, null, null)


        then:
        1 * service.scmService.removeAllPluginConfiguration('myproject')
        1 * service.executionService.deleteBulkExecutionIds(*_)
        1 * fwk.getFrameworkProjectMgr() >> Mock(ProjectManager) {
            1 * removeFrameworkProject('myproject')
        }
        result.success

    }
}
