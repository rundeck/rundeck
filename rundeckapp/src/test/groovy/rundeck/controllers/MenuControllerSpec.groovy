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
import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
import com.dtolabs.rundeck.app.support.SysAclFile
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.authorization.ValidationSet
import com.dtolabs.rundeck.core.authorization.providers.Policy
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import rundeck.AuthToken
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Project
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.User
import rundeck.Workflow
import rundeck.services.ApiService
import rundeck.services.AuthorizationService
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService
import rundeck.services.ScmService
import rundeck.services.UserService
import rundeck.services.authorization.PoliciesValidation
import rundeck.services.scm.ScmPluginConfig
import rundeck.services.scm.ScmPluginConfigData
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

/**
 * Created by greg on 3/15/16.
 */
@TestFor(MenuController)
@Mock([ScheduledExecution, CommandExec, Workflow, Project, Execution, User, AuthToken, ScheduledExecutionStats])
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
        1 * controller.frameworkService.authorizeProjectJobAny(_, job1, ['read','view'], 'AProject') >> true
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
        1 * controller.frameworkService.authorizeProjectJobAny(_, job1, ['read','view'], 'AProject') >> true
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
        1 * controller.frameworkService.authorizeProjectJobAny(_,job1,['read','view'],'AProject')>>true
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
        1 * controller.frameworkService.authorizeProjectJobAny(_,job2,['read','view'],'AProject')>>true
        1 * controller.frameworkService.isClusterModeEnabled()>>true
        1 * controller.apiService.renderSuccessXml(_,_,_)

    }

    def "api jobsAjax without daysAhead param"() {
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
        request.addHeader('x-rundeck-ajax', 'true')

        when:
        params.id=testUUID
        params.project='AProject'
        response.format='json'
        def result = controller.jobsAjax()

        then:
        1 * controller.frameworkService.authResourceForJob(_) >>
        [authorized: true, action: AuthConstants.ACTION_READ, resource: job1]
        1 * controller.frameworkService.authorizeProjectResource(_, _, _, _) >> true
        1 * controller.frameworkService.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    : AuthConstants.ACTION_READ,
                                                                                   resource  : [group: job1.groupPath, name: job1.jobName]]]
        1 * controller.frameworkService.existsFrameworkProject('AProject') >> true
        1 * controller.apiService.requireExists(_, true, ['project', 'AProject']) >> true
        1 * controller.scheduledExecutionService.listWorkflows(_) >> [schedlist: [job1]]
        1 * controller.scheduledExecutionService.finishquery(_, _, _) >> [max           : 20,
                                                                          offset        : 0,
                                                                          paginateParams: [:],
                                                                          displayParams : [:]]
        1 * controller.scheduledExecutionService.nextExecutionTimes(_) >> [(job1.id): new Date()]
        response.json != null
        response.json.count == 1
        response.json.jobs
        response.json.jobs[0].nextScheduledExecution
        !response.json.jobs[0].futureScheduledExecutions
    }

    def "api jobsAjax with invalid daysAhead param"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid: testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime = 200 * 1000
        job1.execCount = 100
        job1.save()
        request.addHeader('x-rundeck-ajax', 'true')

        when:
        params.id = testUUID
        params.project = 'AProject'
        params.daysAhead = daysAhead
        response.format = 'json'
        def result = controller.jobsAjax()

        then:
        1 * controller.frameworkService.authResourceForJob(_) >> [authorized:true, action:AuthConstants.ACTION_READ,resource:job1]
        1 * controller.frameworkService.authorizeProjectResource(_,_,_,_) >> true
        1 * controller.frameworkService.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    :AuthConstants.ACTION_READ,
                                                                                   resource  :[group:job1.groupPath,name:job1.jobName]] ]
        1 * controller.frameworkService.existsFrameworkProject('AProject') >> true
        1 * controller.apiService.requireExists(_,true,['project','AProject']) >> true
        1 * controller.scheduledExecutionService.listWorkflows(_) >> [schedlist : [job1]]
        1 * controller.scheduledExecutionService.finishquery(_,_,_) >> [max: 20,
                                                                        offset:0,
                                                                        paginateParams:[:],
                                                                        displayParams:[:]]
        1 * controller.scheduledExecutionService.nextExecutionTimes(_) >> [ (job1.id) :new Date()]
        response.json != null
        response.json.count == 1
        response.json.jobs
        response.json.jobs[0].nextScheduledExecution
        !response.json.jobs[0].futureScheduledExecutions

        where:
        daysAhead | _
        '0'       | _
        '-1'      | _
    }

    def "api jobsAjax with daysAhead param"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            nextExecutions(_,_) >> [new Date()]
        }
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime=200*1000
        job1.execCount=100
        job1.save()
        request.addHeader('x-rundeck-ajax', 'true')

        when:
        params.id=testUUID
        params.project='AProject'
        params.daysAhead='2'
        response.format='json'
        def result = controller.jobsAjax()

        then:
        1 * controller.frameworkService.authResourceForJob(_) >> [authorized:true, action:AuthConstants.ACTION_READ,resource:job1]
        1 * controller.frameworkService.authorizeProjectResource(_,_,_,_) >> true
        1 * controller.frameworkService.authorizeProjectResources(_,_,_,_) >> [ [authorized:true, 
                                    action:AuthConstants.ACTION_READ,
                                    resource:[group:job1.groupPath,name:job1.jobName]] ]
        1 * controller.frameworkService.existsFrameworkProject('AProject') >> true
        1 * controller.apiService.requireExists(_,true,['project','AProject']) >> true
        1 * controller.scheduledExecutionService.listWorkflows(_) >> [schedlist : [job1]]
        1 * controller.scheduledExecutionService.finishquery(_,_,_) >> [max: 20,
                                                                        offset:0,
                                                                        paginateParams:[:],
                                                                        displayParams:[:]]
        1 * controller.scheduledExecutionService.nextExecutionTimes(_) >> [ (job1.id) :new Date()]
        response.json != null
        response.json.count == 1
        response.json.jobs
        response.json.jobs[0].nextScheduledExecution
        response.json.jobs[0].futureScheduledExecutions
    }

    @Unroll
    def "api jobsAjax with future param"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            nextExecutions(_,_) >> [new Date()]
        }
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid: testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime = 200 * 1000
        job1.execCount = 100
        job1.save()
        request.addHeader('x-rundeck-ajax', 'true')

        when:
        params.id = testUUID
        params.project = 'AProject'
        params.future = futureParam
        response.format = 'json'
        def result = controller.jobsAjax()

        then:
        1 * controller.frameworkService.authResourceForJob(_) >>
        [authorized: true, action: AuthConstants.ACTION_READ, resource: job1]
        1 * controller.frameworkService.authorizeProjectResource(_, _, _, _) >> true
        1 * controller.frameworkService.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    : AuthConstants.ACTION_READ,
                                                                                   resource  : [group: job1.groupPath, name: job1.jobName]]]
        1 * controller.frameworkService.existsFrameworkProject('AProject') >> true
        1 * controller.apiService.requireExists(_, true, ['project', 'AProject']) >> true
        1 * controller.scheduledExecutionService.listWorkflows(_) >> [schedlist: [job1]]
        1 * controller.scheduledExecutionService.finishquery(_, _, _) >> [max           : 20,
                                                                          offset        : 0,
                                                                          paginateParams: [:],
                                                                          displayParams : [:]]
        1 * controller.scheduledExecutionService.nextExecutionTimes(_) >> [(job1.id): new Date()]
        response.json != null
        response.json.count == 1
        response.json.jobs
        response.json.jobs[0].nextScheduledExecution
        response.json.jobs[0].futureScheduledExecutions

        where:
        futureParam | _
        '1h'        | _
        '2d'        | _
        '3w'        | _
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

    def "save project policy"() {
        given:
        def id = 'test.aclpolicy'
        def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)

        when:
        request.method = 'POST'
        setupFormTokens(params)
        params.project = project
        def result = controller.saveProjectAclFile(input)
        then:
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, _) >> true
        1 * controller.frameworkService.getFrameworkProject(project) >> Mock(IRundeckProject) {
            1 * storeFileResource(_, { it.getText('UTF-8') == fileText }) >> {
                fileText.length()
            }
            existsFileResource('acls/' + id) >> exists
            getName() >> project
        }
        1 * controller.authorizationService.validateYamlPolicy(project, 'acls/' + id, fileText) >>
                new PoliciesValidation(validation: new ValidationSet(valid: true))

        response.redirectedUrl == "/project/$project/admin/acls"
        where:
        fileText    | create | exists | project
        'test-data' | true   | false  | 'testproj'
        'test-data' | false  | true   | 'testproj'
    }
    @Unroll
    def "delete project policy"() {
        given:
        def id = 'test.aclpolicy'
        def input = new ProjAclFile(id: id)
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)

        when:
        request.method = 'POST'
        setupFormTokens(params)
        params.project = project
        def result = controller.deleteProjectAclFile(input)
        then:
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authResourceForProjectAcl(project)
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, _) >> true
        1 * controller.frameworkService.getFrameworkProject(project) >> Mock(IRundeckProject) {
            1 * deleteFileResource('acls/' + id) >> true
            existsFileResource('acls/' + id) >> exists
            getName() >> project
        }
        0 * controller.frameworkService._(*_)
        0 * controller.authorizationService._(*_)

        flash.message=~/was deleted/
        response.redirectedUrl == "/project/$project/admin/acls"
        where:
        fileText    |  exists | project
        'test-data' |  true  | 'testproj'
    }

    @Unroll
    def "delete project policy missing id"() {
        given:
        def id = null
        def input = new ProjAclFile(id: id)
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)

        when:
        request.method = 'POST'
        setupFormTokens(params)
        params.project = project
        def result = controller.deleteProjectAclFile(input)
        then:

        0 * controller.frameworkService._(*_)
        0 * controller.authorizationService._(*_)

        flash.message == null
        view == '/common/error'
        where:
        fileText    | exists | project
        'test-data' | true   | 'testproj'
    }

    def "save sys fs policy disabled"() {
        given:
        def id = 'test.aclpolicy'
        def input = new SaveSysAclFile(id: id, fileText: fileText, create: create, fileType: fileType)
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)
        controller.configurationService = Mock(ConfigurationService)
        when:
        request.method = 'POST'
        setupFormTokens(params)
        def result = controller.saveSystemAclFile(input)
        then:
        1 * controller.frameworkService.isClusterModeEnabled() >> true
        1 * controller.configurationService.getBoolean('clusterMode.acls.localfiles.modify.disabled', true) >> true
        view == '/common/error'
        request.errorMessage == 'clusterMode.acls.localfiles.modify.disabled.warning.message'
        where:
        fileType | fileText    | create | exists
        'fs'     | 'test-data' | true   | false
    }

    @Unroll
    def "save sys policy enabled type #fileType #create #exists #overwrite"() {
        given:
        def id = 'test.aclpolicy'
        def input = new SaveSysAclFile(
                id: id,
                fileText: fileText,
                create: create,
                fileType: fileType,
                overwrite: overwrite
        )
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)
        controller.configurationService = Mock(ConfigurationService)
        when:
        request.method = 'POST'
        setupFormTokens(params)
        def result = controller.saveSystemAclFile(input)
        then:
        if (fileType == 'fs') {
            1 * controller.frameworkService.isClusterModeEnabled() >> true
            1 * controller.configurationService.getBoolean('clusterMode.acls.localfiles.modify.disabled', true) >> false
        }
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, _) >> true
        if (fileType == 'fs') {
            1 * controller.frameworkService.existsFrameworkConfigFile(id) >> exists
            1 * controller.frameworkService.writeFrameworkConfigFile(id, fileText) >> fileText.bytes.length
        } else {
            1 * controller.authorizationService.existsPolicyFile(id) >> exists
            1 * controller.authorizationService.storePolicyFileContents(id, fileText) >> fileText.bytes.length
        }

        1 * controller.authorizationService.validateYamlPolicy(id, fileText) >>
                new PoliciesValidation(validation: new ValidationSet(valid: true))
        0 * controller.frameworkService._(*_)
        0 * controller.configurationService._(*_)
        0 * controller.authorizationService._(*_)
        response.status == 302
        response.redirectedUrl == '/menu/acls'
        flash.storedSize == fileText.bytes.length
        flash.storedFile == 'test'
        flash.storedType == fileType
        where:
        fileType  | fileText    | create | exists | overwrite
        'fs'      | 'test-data' | true   | false  | false
        'fs'      | 'test-data' | false  | true   | false
        'storage' | 'test-data' | true   | false  | false
        'storage' | 'test-data' | false  | true   | false
    }

    @Unroll
    def "save sys policy conflict"() {
        given:
        def id = 'test.aclpolicy'
        def input = new SaveSysAclFile(id: id, fileText: fileText, create: create, fileType: fileType, overwrite: false)
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)
        controller.configurationService = Mock(ConfigurationService)
        when:
        request.method = 'POST'
        setupFormTokens(params)
        def result = controller.saveSystemAclFile(input)
        then:
        if (fileType == 'fs') {
            1 * controller.frameworkService.isClusterModeEnabled() >> true
            1 * controller.configurationService.getBoolean('clusterMode.acls.localfiles.modify.disabled', true) >> false
        }
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, _) >> true
        if (fileType == 'fs') {
            1 * controller.frameworkService.existsFrameworkConfigFile(id) >> exists
            0 * controller.frameworkService.writeFrameworkConfigFile(id, fileText) >> fileText.bytes.length
        } else {
            1 * controller.authorizationService.existsPolicyFile(id) >> exists
            0 * controller.authorizationService.storePolicyFileContents(id, fileText) >> fileText.bytes.length
        }

        0 * controller.authorizationService.validateYamlPolicy(id, fileText) >>
                new PoliciesValidation(validation: new ValidationSet(valid: true))
        0 * controller.frameworkService._(*_)
        0 * controller.configurationService._(*_)
        0 * controller.authorizationService._(*_)
        response.status == (exists ? 200 : 404)
        if(exists) {
            view ==~ '/menu/(create|update)SystemAclFile'
            model.id == id
            model.name == 'test'
        }else{
            
        }
        where:
        fileType  | fileText    | create | exists
        'fs'      | 'test-data' | true   | true
        'fs'      | 'test-data' | false  | false
        'storage' | 'test-data' | true   | true
        'storage' | 'test-data' | false  | false
    }


    @Unroll
    def "save/delete acl policy action #action require POST"() {
        when:
        params.putAll(inputParams)
        input.id = 'test.aclpolicy'
        def result = controller."$action"(input)
        then:
        response.status == 405

        where:
        action                 | input                                                | inputParams
        'saveProjectAclFile'   | new SaveProjAclFile(fileText: 'asdf')                | [project: 'aproject']
        'deleteProjectAclFile' | new ProjAclFile()                                    | [project: 'aproject']
        'saveSystemAclFile'    | new SaveSysAclFile(fileType: 'fs', fileText: 'asdf') | [:]
        'deleteSystemAclFile'  | new SysAclFile(fileType: 'fs')                       | [:]
    }

    @Unroll
    def "save/delete acl policy action #action require request Token"() {
        when:
        params.putAll(inputParams)
        request.method = 'POST'
        input.id = 'test.aclpolicy'
        def result = controller."$action"(input)
        then:
        response.status == 200
        request.errorCode == 'request.error.invalidtoken.message'
        view == '/common/error'

        where:
        action                 | input                                                | inputParams
        'saveProjectAclFile'   | new SaveProjAclFile(fileText: 'asdf')                | [project: 'aproject']
        'deleteProjectAclFile' | new ProjAclFile()                                    | [project: 'aproject']
        'saveSystemAclFile'    | new SaveSysAclFile(fileType: 'fs', fileText: 'asdf') | [:]
        'deleteSystemAclFile'  | new SysAclFile(fileType: 'fs')                       | [:]
    }

    @Unroll
    def "delete system policy missing id"() {
        given:
        def input = new SysAclFile(id: id, fileType: fileType)
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)

        when:
        request.method = 'POST'
        setupFormTokens(params)
        def result = controller.deleteSystemAclFile(input)
        then:
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, ['delete','admin']) >> true
        0 * controller.frameworkService._(*_)
        0 * controller.authorizationService._(*_)

        flash.message == null
        view == '/common/error'
        where:
        id   | fileType
        null | 'fs'
        1    | null
    }


    def "meta on policy"() {
        given:
        def id = 'test.aclpolicy'
        // def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)
        def project = 'test'
        def description = 'description'
        PoliciesValidation policy =  new PoliciesValidation(validation: new ValidationSet(valid: true))
        def policyM = Mock(Policy){
            getDescription() >> description
        }


        PolicyCollection policies = Mock(PolicyCollection){
            getPolicies() >> [policyM]
            countPolicies() >> 1
        }
        policy.setPolicies(policies)

        when:
        request.method = 'POST'
        setupFormTokens(params)
        params.project = project
        def result = controller.projectAcls()
        then:
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, _) >> true
        1 * controller.frameworkService.getFrameworkProject(project) >> Mock(IRundeckProject) {
            1 * listDirPaths('acls/') >> {
                ['test.aclpolicy']
            }
            //existsFileResource('acls/' + id) >> exists
            getName() >> project
        }
        1 * controller.authorizationService.validateYamlPolicy(project, id, _) >>
                policy
        result
        result.acllist
        result.acllist.size == 1
        result.acllist[0].meta
        result.acllist[0].meta.policies
        result.acllist[0].meta.policies.size == 1
        result.acllist[0].meta.policies[0].description == description
    }

    def "homeAjax get description field on project table"() {
        given:

        controller.frameworkService = Mock(FrameworkService)
        def description = 'desc'
        new Project(name: 'proj', description: description).save(flush: true)
        def iproj = Mock(IRundeckProject) {
            getName() >> 'proj'
        }
        def projects = [iproj]
        controller.configurationService = Mock(ConfigurationService)
        controller.menuService = Mock(MenuService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)

        request.addHeader('x-rundeck-ajax', 'true')

        when:
        controller.homeAjax()

        then:
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.projectNames(_) >> []
        1 * controller.frameworkService.projects(_) >> projects
        0 * iproj.hasProperty('project.description')
        description == response.json.projects[0].description
    }

    def "homeAjax dont fail on project not created yet"() {
        given:

        controller.frameworkService = Mock(FrameworkService)
        def description = 'desc'
        //new Project(name: 'proj', description: description).save(flush: true)
        def iproj = Mock(IRundeckProject) {
            getName() >> 'proj'
        }
        def projects = [iproj]
        controller.configurationService = Mock(ConfigurationService)
        controller.menuService = Mock(MenuService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)

        request.addHeader('x-rundeck-ajax', 'true')

        when:
        controller.homeAjax()

        then:
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.projectNames(_) >> []
        1 * controller.frameworkService.projects(_) >> projects
        1 * iproj.hasProperty('project.description') >> true
        1 * iproj.getProperty('project.description') >> description
        description == response.json.projects[0].description
    }

    def "homeAjax get description field on properties when is null on table"() {
        given:
        def description = 'desc'
        controller.frameworkService = Mock(FrameworkService)
        new Project(name: 'proj').save(flush: true)
        def iproj = Mock(IRundeckProject) {
            getName() >> 'proj'
        }
        def projects = [iproj]
        controller.configurationService = Mock(ConfigurationService)
        controller.menuService = Mock(MenuService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)

        request.addHeader('x-rundeck-ajax', 'true')

        when:
        controller.homeAjax()

        then:
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.projectNames(_) >> []
        1 * controller.frameworkService.projects(_) >> projects
        1 * iproj.hasProperty('project.description') >> true
        1 * iproj.getProperty('project.description') >> description
        description == response.json.projects[0].description
    }
    
    def "list Export"() {
        given:
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        def project = 'test'
        def scmConfig = Mock(ScmPluginConfigData){
            getEnabled() >> true
        }

        when:
        request.method = 'POST'
        request.JSON = []
        request.format = 'json'
        params.project = project
        controller.listExport()
        then:

        1 * controller.scheduledExecutionService.listWorkflows(_) >> [schedlist : []]
        1 * controller.scheduledExecutionService.finishquery(_,_,_) >> [max: 20,
                                                                        offset:0,
                                                                        paginateParams:[:],
                                                                        displayParams:[:]]
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN,
                                                                               AuthConstants.ACTION_EXPORT,
                                                                                AuthConstants.SCM_EXPORT]) >> true
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN,
                                                                               AuthConstants.ACTION_IMPORT,
                                                                               AuthConstants.SCM_IMPORT]) >> true
        1 * controller.scmService.projectHasConfiguredExportPlugin(project) >> true
        1 * controller.scmService.projectHasConfiguredImportPlugin(project) >> false
        1 * controller.scmService.loadScmConfig(project,'export') >> scmConfig

        response.json
        response.json.scmExportEnabled
        !response.json.scmImportEnabled
    }

    def "initialize scm on ajax call if its cluster"() {
        given:
        controller.frameworkService = Mock(FrameworkService){
            isClusterModeEnabled() >> true
        }
        controller.authorizationService = Mock(AuthorizationService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        def project = 'test'
        def scmConfig = Mock(ScmPluginConfigData){
            getEnabled() >> true
        }

        when:
        request.method = 'POST'
        request.JSON = []
        request.format = 'json'
        params.project = project
        controller.listExport()
        then:

        1 * controller.scheduledExecutionService.listWorkflows(_) >> [schedlist : []]
        1 * controller.scheduledExecutionService.finishquery(_,_,_) >> [max: 20,
                                                                        offset:0,
                                                                        paginateParams:[:],
                                                                        displayParams:[:]]
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN,
                                                                               AuthConstants.ACTION_EXPORT,
                                                                               AuthConstants.SCM_EXPORT]) >> true
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN,
                                                                               AuthConstants.ACTION_IMPORT,
                                                                                AuthConstants.SCM_IMPORT]) >> true
        1 * controller.scmService.projectHasConfiguredExportPlugin(project) >> true
        1 * controller.scmService.projectHasConfiguredImportPlugin(project) >> false
        1 * controller.scmService.loadScmConfig(project,'export') >> scmConfig
        1 * controller.scmService.initProject(project,'export')
        1 * controller.scmService.initProject(project,'import')

        response.json
        response.json.scmExportEnabled
        !response.json.scmImportEnabled
    }

    def "project Toggle SCM off"(){
        given:
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        def project = 'test'

        def type = 'git'
        def econfig = new ScmPluginConfig(new Properties(), 'prefix')
        econfig.setType(type)
        econfig.setEnabled(true)

        def descPlugin = Mock(DescribedPlugin)
        descPlugin.name >> 'git-export'

        when:
        request.method = 'POST'
        params.project = project
        controller.projectToggleSCM()
        then:
        1 * controller.frameworkService.authorizeApplicationResourceAll(_, _, [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]) >> true
        1 * controller.scmService.loadScmConfig(project, 'export') >> econfig
        1 * controller.scmService.loadScmConfig(project, 'import')

        1* controller.scmService.projectHasConfiguredPlugin('export', project) >> true

        1 * controller.scmService.getPluginDescriptor('export', type) >> descPlugin
        1 * controller.scmService.disablePlugin('export', project, 'git-export')


        response.status == 302
    }

    def "project Toggle SCM on"(){
        given:
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        def project = 'test'

        def type = 'git'
        def econfig = new ScmPluginConfig(new Properties(), 'prefix')
        econfig.setType(type)
        econfig.setEnabled(false)

        def descPlugin = Mock(DescribedPlugin)
        descPlugin.name >> 'git-export'

        when:
        request.method = 'POST'
        params.project = project
        controller.projectToggleSCM()
        then:
        1 * controller.frameworkService.authorizeApplicationResourceAll(_, _, [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]) >> true
        1 * controller.scmService.loadScmConfig(project, 'export') >> econfig
        1 * controller.scmService.loadScmConfig(project, 'import')

        1 * controller.scmService.getPluginDescriptor('export', type) >> descPlugin
        0 * controller.scmService.disablePlugin(_, project, _)
        1 * controller.scmService.enablePlugin(_, 'export', project, 'git-export')


        response.status == 302
    }

    def "project Toggle SCM do nothing without configured plugins"(){
        given:
        controller.frameworkService = Mock(FrameworkService)
        controller.authorizationService = Mock(AuthorizationService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        def project = 'test'

        def type = 'git'
        def econfig = new ScmPluginConfig(new Properties(), 'prefix')
        econfig.setType(type)
        econfig.setEnabled(false)

        def descPlugin = Mock(DescribedPlugin)
        descPlugin.name >> 'git-export'

        when:
        request.method = 'POST'
        params.project = project
        controller.projectToggleSCM()
        then:
        1 * controller.frameworkService.authorizeApplicationResourceAll(_, _, [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]) >> true
        1 * controller.scmService.loadScmConfig(project, 'export')
        1 * controller.scmService.loadScmConfig(project, 'import')

        0 * controller.scmService.disablePlugin(_, project, _)
        0 * controller.scmService.enablePlugin(_, _, project, _)

        response.status == 302
    }

    def "homeAjax get project label"() {
        given:

        controller.frameworkService = Mock(FrameworkService)
        new Project(name: 'proj',description: 'desc').save(flush: true)
        def iproj = Mock(IRundeckProject) {
            getName() >> 'proj'
        }
        def projects = [iproj]
        controller.configurationService = Mock(ConfigurationService)
        controller.menuService = Mock(MenuService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)

        request.addHeader('x-rundeck-ajax', 'true')

        when:
        controller.homeAjax()

        then:
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.projectNames(_) >> []
        1 * controller.frameworkService.projects(_) >> projects
        1 * iproj.hasProperty('project.label') >> true
        1 * iproj.getProperty('project.label') >> 'label'
        'label' == response.json.projects[0].label
    }


    def "jobs nextSchedListIds"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager)
            }
        }
        controller.authorizationService = Mock(AuthorizationService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        controller.userService = Mock(UserService)
        def query = new ScheduledExecutionQuery()
        params.project='test'
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))

        when:
        def model = controller.jobs(query)
        then:
        1 * controller.frameworkService.authResourceForJob(_) >>
                [authorized: true, action: AuthConstants.ACTION_READ, resource: job1]
        1 * controller.frameworkService.authorizeProjectResource(_, _, _, _) >> true
        1 * controller.frameworkService.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    : AuthConstants.ACTION_READ,
                                                                                   resource  : [group: job1.groupPath, name: job1.jobName]]]
        1 * controller.scheduledExecutionService.listWorkflows(_) >> [schedlist: [job1]]
        1 * controller.scheduledExecutionService.finishquery(_, _, _) >> [max           : 20,
                                                                          offset        : 0,
                                                                          paginateParams: [:],
                                                                          displayParams : [:]]
        model.nextSchedListIds.size() == model.nextScheduled.size()
        model.nextSchedListIds.get(0) == model.nextScheduled.get(0).extid
    }


    def "job list with scm active"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        ScmPluginConfig data = Mock(ScmPluginConfig)
        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager)
            }
            authorizeApplicationResourceAny(_,_,_)>>true
        }
        controller.authorizationService = Mock(AuthorizationService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        controller.userService = Mock(UserService)
        def query = new ScheduledExecutionQuery()
        params.project='test'
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))

        when:
        def model = controller.jobsFragment(query)
        then:
        1 * controller.frameworkService.authResourceForJob(_) >>
                [authorized: true, action: AuthConstants.ACTION_READ, resource: job1]
        1 * controller.frameworkService.authorizeProjectResource(_, _, _, _) >> true
        1 * controller.frameworkService.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    : AuthConstants.ACTION_READ,
                                                                                   resource  : [group: job1.groupPath, name: job1.jobName]]]
        1 * controller.scheduledExecutionService.listWorkflows(_) >> [schedlist: [job1]]
        1 * controller.scheduledExecutionService.finishquery(_, _, _) >> [max           : 20,
                                                                          offset        : 0,
                                                                          paginateParams: [:],
                                                                          displayParams : [:]]
        1 * controller.scmService.projectHasConfiguredExportPlugin(_)>>true
        controller.scmService.loadScmConfig(_,'export')>>Mock(ScmPluginConfig){
            getEnabled()>>true
        }
        1 * controller.scmService.exportStatusForJobs(_,_)
        1 * controller.scmService.exportPluginActions(_,_)
    }

    def "user summary"() {
        given:
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext)
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_)>>auth
            1 * authorizeApplicationResourceType(_,_,_) >> true
        }
        def userToSearch = 'admin'
        def email = 'test@test.com'
        def text = '{email:\''+email+'\',firstName:\'The\', lastName:\'Admin\'}'
        User u = new User(login: userToSearch)
        u.save()

        when:
        def model = controller.userSummary()

        then:
        model
        model.users
        model.users.admin
        model.users.admin.login=='admin'

    }

    def "user summary with last exec"() {
        given:
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext)
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_)>>auth
            1 * authorizeApplicationResourceType(_,_,_) >> true
        }
        def userToSearch = 'admin'
        def email = 'test@test.com'
        def text = '{email:\''+email+'\',firstName:\'The\', lastName:\'Admin\'}'
        User u = new User(login: userToSearch)
        u.save()

        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job.save()
        def exec = new Execution(
                scheduledExecution: job,
                dateStarted: new Date(),
                dateCompleted: null,
                user: userToSearch,
                project: 'AProject'
        ).save()

        when:
        def model = controller.userSummary()

        then:
        model
        model.users
        model.users.admin
        model.users.admin.lastJob

    }

    def "api job forecast xml"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            createTrigger(_) >> org.quartz.TriggerBuilder.newTrigger().build();
        }
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime=200*1000
        job1.execCount=100
        job1.scheduled=true
        job1.save()

        when:
        params.id=testUUID
        def result = controller.apiJobForecast()

        then:
        1 * controller.apiService.requireVersion(_, _, 31) >> true
        1 * controller.apiService.requireParameters(_, _, ['id']) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(testUUID) >> job1
        1 * controller.apiService.requireExists(_, job1, _) >> true
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, 'AProject') >>
                Mock(UserAndRolesAuthContext)
        1 * controller.frameworkService.authorizeProjectJobAny(_, job1, ['read','view'], 'AProject') >> true
        1 * controller.apiService.apiHrefForJob(job1) >> 'api/href'
        1 * controller.apiService.guiHrefForJob(job1) >> 'gui/href'

        response.xml != null
        response.xml.id  == testUUID
        response.xml.description  == 'a job'
        response.xml.name  == 'job1'
        response.xml.group  == 'some/where'
        response.xml.href  == 'api/href'
        response.xml.permalink  == 'gui/href'
        response.xml.futureScheduledExecutions != null
        response.xml.futureScheduledExecutions.size() == 1

    }
    def "api job forecast json"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            createTrigger(_) >> org.quartz.TriggerBuilder.newTrigger().build();
        }
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime=200*1000
        job1.execCount=100
        job1.scheduled=true
        job1.save()

        when:
        params.id=testUUID
        response.format='json'
        def result = controller.apiJobForecast()

        then:
        1 * controller.apiService.requireVersion(_, _, 31) >> true
        1 * controller.apiService.requireParameters(_, _, ['id']) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(testUUID) >> job1
        1 * controller.apiService.requireExists(_, job1, _) >> true
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, 'AProject') >>
                Mock(UserAndRolesAuthContext)
        1 * controller.frameworkService.authorizeProjectJobAny(_, job1, ['read','view'], 'AProject') >> true
        1 * controller.apiService.apiHrefForJob(job1) >> 'api/href'
        1 * controller.apiService.guiHrefForJob(job1) >> 'gui/href'
        1 * controller.scheduledExecutionService.nextExecutions(_,_,false) >> [new Date()]

        response.json != null
        response.json.id  == testUUID
        response.json.description  == 'a job'
        response.json.name  == 'job1'
        response.json.group  == 'some/where'
        response.json.href  == 'api/href'
        response.json.permalink  == 'gui/href'
        response.json.futureScheduledExecutions != null
        response.json.futureScheduledExecutions.size() == 1
    }


    def "api job forecast json past mode"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            createTrigger(_) >> org.quartz.TriggerBuilder.newTrigger().build();
        }
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime=200*1000
        job1.execCount=100
        job1.scheduled=true
        job1.save()

        when:
        request.api_version = 32
        params.id=testUUID
        params.past='true'
        response.format='json'
        def result = controller.apiJobForecast()

        then:
        1 * controller.apiService.requireVersion(_, _, 31) >> true
        1 * controller.apiService.requireParameters(_, _, ['id']) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(testUUID) >> job1
        1 * controller.apiService.requireExists(_, job1, _) >> true
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, 'AProject') >>
                Mock(UserAndRolesAuthContext)
        1 * controller.frameworkService.authorizeProjectJobAny(_, job1, ['read','view'], 'AProject') >> true
        1 * controller.apiService.apiHrefForJob(job1) >> 'api/href'
        1 * controller.apiService.guiHrefForJob(job1) >> 'gui/href'
        1 * controller.scheduledExecutionService.nextExecutions(_,_,true) >> [new Date()]

        response.json != null
        response.json.id  == testUUID
        response.json.description  == 'a job'
        response.json.name  == 'job1'
        response.json.group  == 'some/where'
        response.json.href  == 'api/href'
        response.json.permalink  == 'gui/href'
        response.json.futureScheduledExecutions != null
        response.json.futureScheduledExecutions.size() == 1
    }
}
