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

package rundeck.controllers

import com.dtolabs.rundeck.app.support.ProjAclFile
import com.dtolabs.rundeck.app.support.SaveProjAclFile
import com.dtolabs.rundeck.app.support.SaveSysAclFile
import com.dtolabs.rundeck.app.support.SysAclFile
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.authorization.providers.Policies
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ApiService
import rundeck.services.AuthorizationService
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService
import rundeck.services.authorization.PoliciesValidation
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

/**
 * Created by greg on 3/15/16.
 */
@TestFor(MenuController)
@Mock([ScheduledExecution, CommandExec, Workflow])
class MenuControllerSpec extends Specification {
    def "api job detail xml"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime=200*1000
        job1.execCount=100
        job1.save()

        when:
        params.id=testUUID
        def result = controller.apiJobDetail()

        then:
        1 * controller.apiService.requireVersion(_, _, 18) >> true
        1 * controller.apiService.requireParameters(_, _, ['id']) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(testUUID) >> job1
        1 * controller.apiService.requireExists(_, job1, _) >> true
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, 'AProject') >>
                Mock(UserAndRolesAuthContext)
        1 * controller.frameworkService.authorizeProjectJobAll(_, job1, ['read'], 'AProject') >> true
        1 * controller.apiService.apiHrefForJob(job1) >> 'api/href'
        1 * controller.apiService.guiHrefForJob(job1) >> 'gui/href'

        response.xml != null
        response.xml.id  == testUUID
        response.xml.description  == 'a job'
        response.xml.name  == 'job1'
        response.xml.group  == 'some/where'
        response.xml.href  == 'api/href'
        response.xml.permalink  == 'gui/href'
        response.xml.averageDuration  == '2000'
    }
    def "api job detail json"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime=200*1000
        job1.execCount=100
        job1.save()

        when:
        params.id=testUUID
        response.format='json'
        def result = controller.apiJobDetail()

        then:
        1 * controller.apiService.requireVersion(_, _, 18) >> true
        1 * controller.apiService.requireParameters(_, _, ['id']) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(testUUID) >> job1
        1 * controller.apiService.requireExists(_, job1, _) >> true
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, 'AProject') >>
                Mock(UserAndRolesAuthContext)
        1 * controller.frameworkService.authorizeProjectJobAll(_, job1, ['read'], 'AProject') >> true
        1 * controller.apiService.apiHrefForJob(job1) >> 'api/href'
        1 * controller.apiService.guiHrefForJob(job1) >> 'gui/href'

        response.json != null
        response.json.id  == testUUID
        response.json.description  == 'a job'
        response.json.name  == 'job1'
        response.json.group  == 'some/where'
        response.json.href  == 'api/href'
        response.json.permalink  == 'gui/href'
        response.json.averageDuration  == 2000
    }

    def "scheduler list jobs invalid uuid"() {
        given:
        def paramUUID = "not a uuid"
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)

        when:
        def result = controller.apiSchedulerListJobs(paramUUID, false)

        then:
        1 * controller.apiService.requireVersion(_, _, 17) >> true
        1 * controller.apiService.renderErrorFormat(_,{map->
            map.status==400 && map.code=='api.error.parameter.error'
        })

    }

    private Map createJobParams(Map overrides=[:]){
        [
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy'])]),
                serverNodeUUID: null,
                scheduled: true
        ]+overrides
    }
    def "scheduler list this servers jobs"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName:'job1'))
        job1.scheduled=true
        job1.serverNodeUUID=testUUID
        job1.save()

        ScheduledExecution job2 = new ScheduledExecution(createJobParams(jobName:'job2'))
        job2.scheduled=true
        job2.serverNodeUUID=UUID.randomUUID().toString()
        job2.save()

        when:
        def result = controller.apiSchedulerListJobs(null, true)

        then:
        1 * controller.apiService.requireVersion(_, _, 17) >> true
        _ * controller.frameworkService.getServerUUID() >> testUUID
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_,'AProject') >> Mock(UserAndRolesAuthContext)
        1 * controller.frameworkService.authorizeProjectJobAll(_,job1,['read'],'AProject')>>true
        1 * controller.frameworkService.isClusterModeEnabled()>>true
        1 * controller.apiService.renderSuccessXml(_,_,_)

    }
    def "scheduler list other server jobs"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def uuid2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName:'job1'))
        job1.scheduled=true
        job1.serverNodeUUID=testUUID
        job1.save()

        ScheduledExecution job2 = new ScheduledExecution(createJobParams(jobName:'job2'))
        job2.scheduled=true
        job2.serverNodeUUID=uuid2
        job2.save()

        when:
        def result = controller.apiSchedulerListJobs(uuid2, false)

        then:
        1 * controller.apiService.requireVersion(_, _, 17) >> true
        _ * controller.frameworkService.getServerUUID() >> testUUID
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_,'AProject') >> Mock(UserAndRolesAuthContext)
        1 * controller.frameworkService.authorizeProjectJobAll(_,job2,['read'],'AProject')>>true
        1 * controller.frameworkService.isClusterModeEnabled()>>true
        1 * controller.apiService.renderSuccessXml(_,_,_)

    }

    protected void setupFormTokens(params) {
        def token = SynchronizerTokensHolder.store(session)
        params[SynchronizerTokensHolder.TOKEN_KEY] = token.generateToken('/test')
        params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
    }

    @Unroll
    def "required auth for acl action #action"() {
        given:
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)
        when:
        if (paramvals) {
            params.putAll(paramvals)
        }
        if (method) {
            request.method = method
            setupFormTokens(params)
        }
        def result = arg ? controller."$action"(arg) : controller."$action"()
        then:

        response.status == HttpServletResponse.SC_FORBIDDEN
        result == null
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, resource, actions) >> false
        if (paramvals?.project) {
            1 * controller.frameworkService.authResourceForProjectAcl(paramvals.project) >>
                    AuthorizationUtil.resource(AuthConstants.TYPE_PROJECT_ACL, [name: paramvals.project])
        }
        controller.authorizationService.existsPolicyFile(_) >> storageExists
        controller.frameworkService.getFrameworkProject(_) >> Mock(IRundeckProject) {
            existsFileResource(_) >> storageExists
        }
        where:
        method | action                 |
                actions             |
                arg                                                                                         |
                paramvals             |
                resource                               |
                storageExists
        null   | 'acls'                 |
                ['read', 'admin']   |
                null                                                                                        |
                null                  |
                [kind: 'system_acl', type: 'resource']     |
                false
        null   | 'editSystemAclFile'    |
                ['update', 'admin'] |
                new SysAclFile(id: 'test.aclpolicy', fileType: 'fs')                                        |
                null                  |
                [kind: 'system_acl', type: 'resource'] |
                false
        'POST' | 'saveSystemAclFile'    |
                ['update', 'admin'] |
                new SaveSysAclFile(id: 'test.aclpolicy', fileType: 'storage', create: false, fileText: 'z') |
                null                  |
                [kind: 'system_acl', type: 'resource'] |
                false
        'POST' | 'saveSystemAclFile'    |
                ['create', 'admin'] |
                new SaveSysAclFile(id: 'test.aclpolicy', fileType: 'storage', create: true, fileText: 'z')  |
                null                  |
                [kind: 'system_acl', type: 'resource'] |
                true
        'POST' | 'deleteSystemAclFile'  |
                ['delete', 'admin'] |
                new SysAclFile(id: 'test.aclpolicy', fileType: 'storage')                                   |
                null                  |
                [kind: 'system_acl', type: 'resource'] |
                true
        'POST' | 'deleteSystemAclFile'  |
                ['delete', 'admin'] |
                new SysAclFile(id: 'test.aclpolicy', fileType: 'fs')                                        |
                null                  |
                [kind: 'system_acl', type: 'resource'] |
                true
        null   | 'createSystemAclFile'  |
                ['create', 'admin'] |
                null                                                                                        |
                [fileType: 'storage'] |
                [kind: 'system_acl', type: 'resource'] |
                true
        null   | 'createSystemAclFile'  |
                ['create', 'admin'] |
                null                                                                                        |
                [fileType: 'fs']      |
                [kind: 'system_acl', type: 'resource'] |
                true
        null   | 'projectAcls'          |
                ['read', 'admin']   |
                null                                                                                        |
                [project: 'a']        |
                [name: 'a', type: 'project_acl']       |
                false
        null   | 'editProjectAclFile'   |
                ['update', 'admin'] |
                new ProjAclFile(id: 'test.aclpolicy')                                                       |
                [project: 'a']        |
                [name: 'a', type: 'project_acl']       |
                false
        'POST' | 'saveProjectAclFile'   |
                ['update', 'admin'] |
                new SaveProjAclFile(id: 'test.aclpolicy', create: false, fileText: 'x')                     |
                [project: 'a']        |
                [name: 'a', type: 'project_acl']       |
                false
        'POST' | 'saveProjectAclFile'   |
                ['create', 'admin'] |
                new SaveProjAclFile(id: 'test.aclpolicy', create: true, fileText: 'x')                      |
                [project: 'a']        |
                [name: 'a', type: 'project_acl']       |
                true
        null   | 'createProjectAclFile' |
                ['create', 'admin'] |
                null                                                                                        |
                [project: 'a']        |
                [name: 'a', type: 'project_acl']       |
                false
        'POST' | 'deleteProjectAclFile' |
                ['delete', 'admin'] |
                new ProjAclFile(id: 'test.aclpolicy')                                                       |
                [project: 'a']        |
                [name: 'a', type: 'project_acl']       |
                false

    }

    def "load project policy"() {
        given:
        def project = Mock(IRundeckProject) {
            getName() >> 'aproject'
        }
        def ident = 'a.aclpolicy'
        def validation = new PoliciesValidation()
        controller.authorizationService = Mock(AuthorizationService)
        when:
        def result = controller.loadProjectPolicyValidation(project, ident)

        then:
        1 * project.loadFileResource('acls/' + ident, _) >> { args ->
            args[1].write('data'.bytes)
            4L
        }
        1 * controller.authorizationService.validateYamlPolicy('aproject', ident, 'data') >> validation
        result == validation

    }
}
