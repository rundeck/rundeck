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

import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.app.gui.GroupedJobListLinkHandler
import com.dtolabs.rundeck.app.gui.JobListLinkHandlerRegistry
import com.dtolabs.rundeck.app.support.ProjAclFile
import com.dtolabs.rundeck.app.support.SaveProjAclFile
import com.dtolabs.rundeck.app.support.SaveSysAclFile
import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
import com.dtolabs.rundeck.app.support.SysAclFile
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.RuleSetValidation
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.authorization.ValidationSet
import com.dtolabs.rundeck.core.authorization.providers.BaseValidator
import com.dtolabs.rundeck.core.authorization.providers.PoliciesValidation
import com.dtolabs.rundeck.core.authorization.providers.Policy
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection
import com.dtolabs.rundeck.core.authorization.providers.Validator
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.server.AuthContextEvaluatorCacheManager
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import grails.web.Action
import org.rundeck.app.acl.ACLFileManager
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.auth.CoreTypedRequestAuthorizer
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.authorization.domain.AppAuthorizer
import org.rundeck.app.gui.JobListLinkHandler
import org.rundeck.core.auth.AuthConstants
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.core.auth.app.NamedAuthRequestUtil
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.app.type.AuthorizingSystem
import org.rundeck.core.auth.web.RdAuthorizeApplicationType
import org.rundeck.core.auth.web.RdAuthorizeSystem
import rundeck.AuthToken
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Project
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.User
import rundeck.Workflow
import rundeck.services.ApiService
import rundeck.services.AclFileManagerService
import rundeck.services.ConfigurationService
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.JobSchedulesService
import rundeck.services.LocalJobSchedulesManager
import rundeck.services.ScheduledExecutionService
import rundeck.services.ScmService
import rundeck.services.UserService
import rundeck.services.feature.FeatureService
import rundeck.services.scm.ScmPluginConfig
import rundeck.services.scm.ScmPluginConfigData
import spock.lang.Ignore
import spock.lang.Unroll
import testhelper.RundeckHibernateSpec

import javax.security.auth.Subject
import javax.servlet.http.HttpServletResponse
import java.lang.annotation.Annotation
import java.nio.file.Files

/**
 * Created by greg on 3/15/16.
 */
class MenuControllerSpec extends RundeckHibernateSpec implements ControllerUnitTest<MenuController> {

    List<Class> getDomainClasses() { [ScheduledExecution, CommandExec, Workflow, Project, Execution, User, AuthToken, ScheduledExecutionStats, UserService] }

    def setup(){
        session.subject=new Subject()

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'

        defineBeans {
            configurationService(ConfigurationService) {
                grailsApplication = grailsApplication
            }
        }
    }
    def "home without sidebar feature"(){
        given:
            controller.configurationService=Mock(ConfigurationService)
            controller.frameworkService=Mock(FrameworkService){
                getRundeckFramework()>>Mock(IFramework)
            }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                        controller.featureService=Mock(FeatureService)
        when:
            def result = controller.home()
        then:
            model!=null
            model.projectNames==null
    }
    def "api job detail xml"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime=200*1000
        job1.execCount=100
        job1.save()
        def jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> job1.shouldScheduleExecution()
        }
        controller.jobSchedulesService = jobSchedulesService
        controller.scheduledExecutionService.jobSchedulesService = jobSchedulesService

        when:
        params.id=testUUID
        controller.response.format = "xml"
        def result = controller.apiJobDetail()

        then:
        1 * controller.apiService.requireApi(_, _, 18) >> true
        1 * controller.apiService.requireParameters(_, _, ['id']) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(testUUID) >> job1
        1 * controller.apiService.requireExists(_, job1, _) >> true
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, 'AProject') >>
                Mock(UserAndRolesAuthContext)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_, job1, ['read','view'], 'AProject') >> true
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
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime=200*1000
        job1.execCount=100
        job1.save()
        def jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> job1.shouldScheduleExecution()
        }
        controller.jobSchedulesService = jobSchedulesService
        controller.scheduledExecutionService.jobSchedulesService = jobSchedulesService

        when:
        params.id=testUUID
        response.format='json'
        def result = controller.apiJobDetail()

        then:
        1 * controller.apiService.requireApi(_, _, 18) >> true
        1 * controller.apiService.requireParameters(_, _, ['id']) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(testUUID) >> job1
        1 * controller.apiService.requireExists(_, job1, _) >> true
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, 'AProject') >>
                Mock(UserAndRolesAuthContext)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_, job1, ['read','view'], 'AProject') >> true
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
        1 * controller.apiService.requireApi(_, _, 17) >> true
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
        controller.jobSchedulesService = new JobSchedulesService()
        controller.jobSchedulesService.rundeckJobSchedulesManager = new LocalJobSchedulesManager()

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName:'job1'))
        job1.scheduled=true
        job1.serverNodeUUID=testUUID
        job1.save()

        ScheduledExecution job2 = new ScheduledExecution(createJobParams(jobName:'job2'))
        job2.scheduled=true
        job2.serverNodeUUID=UUID.randomUUID().toString()
        job2.save()

        ScheduledExecution unscheduledJob = new ScheduledExecution(createJobParams(jobName:'unscheduled'))
        unscheduledJob.scheduled=false
        unscheduledJob.serverNodeUUID=testUUID
        unscheduledJob.save()

        when:
        def result = controller.apiSchedulerListJobs(null, true)

        then:
        1 * controller.apiService.requireApi(_, _, 17) >> true
        _ * controller.frameworkService.getServerUUID() >> testUUID
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_,'AProject') >> Mock(UserAndRolesAuthContext)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_,job1,['read','view'],'AProject')>>true
        0 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_,unscheduledJob,['read','view'],'AProject')
        1 * controller.frameworkService.isClusterModeEnabled()>>true
        1 * controller.apiService.renderSuccessXml(_,_,_)

    }
    def "scheduler list other server jobs"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def uuid2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.jobSchedulesService = new JobSchedulesService()
        controller.jobSchedulesService.rundeckJobSchedulesManager = new LocalJobSchedulesManager()
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName:'job1'))
        job1.scheduled=true
        job1.serverNodeUUID=testUUID
        job1.save()

        ScheduledExecution unscheduledJob = new ScheduledExecution(createJobParams(jobName:'unscheduled'))
        unscheduledJob.scheduled=false
        unscheduledJob.serverNodeUUID=uuid2
        unscheduledJob.save()

        ScheduledExecution job2 = new ScheduledExecution(createJobParams(jobName:'job2'))
        job2.scheduled=true
        job2.serverNodeUUID=uuid2
        job2.save()

        when:
        def result = controller.apiSchedulerListJobs(uuid2, false)

        then:
        1 * controller.apiService.requireApi(_, _, 17) >> true
        _ * controller.frameworkService.getServerUUID() >> testUUID
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_,'AProject') >> Mock(UserAndRolesAuthContext)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_,job2,['read','view'],'AProject')>>true
        0 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_,unscheduledJob,['read','view'],'AProject')
        1 * controller.frameworkService.isClusterModeEnabled()>>true
        1 * controller.apiService.renderSuccessXml(_,_,_)

    }

    def "api jobsAjax without daysAhead param"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.configurationService = Mock(ConfigurationService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.jobSchedulesService = Mock(JobSchedulesService)
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
        1 * controller.rundeckAuthContextProcessor.authResourceForJob(_) >>
        [authorized: true, action: AuthConstants.ACTION_READ, resource: job1]
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_, _, _, _) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    : AuthConstants.ACTION_READ,
                                                                                   resource  : [group: job1.groupPath, name: job1.jobName]]]
        1 * controller.frameworkService.existsFrameworkProject('AProject') >> true
        1 * controller.apiService.requireExists(_, true, ['project', 'AProject']) >> true
        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist: [job1]]
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
        controller.configurationService = Mock(ConfigurationService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.jobSchedulesService = Mock(JobSchedulesService)
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
        1 * controller.rundeckAuthContextProcessor.authResourceForJob(_) >> [authorized:true, action:AuthConstants.ACTION_READ,resource:job1]
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_,_,_,_) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    :AuthConstants.ACTION_READ,
                                                                                   resource  :[group:job1.groupPath,name:job1.jobName]] ]
        1 * controller.frameworkService.existsFrameworkProject('AProject') >> true
        1 * controller.apiService.requireExists(_,true,['project','AProject']) >> true
        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist : [job1]]
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
        controller.configurationService = Mock(ConfigurationService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            nextExecutions(_,_) >> [new Date()]
        }
        controller.jobSchedulesService = Mock(JobSchedulesService)
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
        1 * controller.rundeckAuthContextProcessor.authResourceForJob(_) >> [authorized:true, action:AuthConstants.ACTION_READ,resource:job1]
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_,_,_,_) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResources(_,_,_,_) >> [ [authorized:true,
                                    action:AuthConstants.ACTION_READ,
                                    resource:[group:job1.groupPath,name:job1.jobName]] ]
        1 * controller.frameworkService.existsFrameworkProject('AProject') >> true
        1 * controller.apiService.requireExists(_,true,['project','AProject']) >> true
        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist : [job1]]
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
        controller.configurationService = Mock(ConfigurationService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            nextExecutions(_,_) >> [new Date()]
        }
        controller.jobSchedulesService = Mock(JobSchedulesService)
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
        1 * controller.rundeckAuthContextProcessor.authResourceForJob(_) >>
        [authorized: true, action: AuthConstants.ACTION_READ, resource: job1]
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_, _, _, _) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    : AuthConstants.ACTION_READ,
                                                                                   resource  : [group: job1.groupPath, name: job1.jobName]]]
        1 * controller.frameworkService.existsFrameworkProject('AProject') >> true
        1 * controller.apiService.requireExists(_, true, ['project', 'AProject']) >> true
        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist: [job1]]
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)
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
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, resource, actions) >> false
        if (paramvals?.project) {
            1 * controller.rundeckAuthContextProcessor.authResourceForProjectAcl(paramvals.project) >>
                    AuthorizationUtil.resource(AuthConstants.TYPE_PROJECT_ACL, [name: paramvals.project])
        }
        controller.aclFileManagerService.existsPolicyFile(AppACLContext.system(), _) >> storageExists
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
        null   | 'editSystemAclFile'    |
                ['update', 'admin', 'app_admin'] |
                new SysAclFile(id: 'test.aclpolicy', fileType: 'fs')                                        |
                null                  |
                [kind: 'system_acl', type: 'resource'] |
                false
        'POST' | 'saveSystemAclFile'    |
                ['update', 'admin', 'app_admin'] |
                new SaveSysAclFile(id: 'test.aclpolicy', fileType: 'storage', create: false, fileText: 'z') |
                null                  |
                [kind: 'system_acl', type: 'resource'] |
                false
        'POST' | 'saveSystemAclFile'    |
                ['create', 'admin', 'app_admin'] |
                new SaveSysAclFile(id: 'test.aclpolicy', fileType: 'storage', create: true, fileText: 'z')  |
                null                  |
                [kind: 'system_acl', type: 'resource'] |
                true
        'POST' | 'deleteSystemAclFile'  |
                ['delete', 'admin', 'app_admin'] |
                new SysAclFile(id: 'test.aclpolicy', fileType: 'storage')                                   |
                null                  |
                [kind: 'system_acl', type: 'resource'] |
                true
        'POST' | 'deleteSystemAclFile'  |
                ['delete', 'admin', 'app_admin'] |
                new SysAclFile(id: 'test.aclpolicy', fileType: 'fs')                                        |
                null                  |
                [kind: 'system_acl', type: 'resource'] |
                true
        null   | 'createSystemAclFile'  |
                ['create', 'admin', 'app_admin'] |
                null                                                                                        |
                [fileType: 'storage'] |
                [kind: 'system_acl', type: 'resource'] |
                true
        null   | 'createSystemAclFile'  |
                ['create', 'admin', 'app_admin'] |
                null                                                                                        |
                [fileType: 'fs']      |
                [kind: 'system_acl', type: 'resource'] |
                true
        null   | 'projectAcls'          |
                ['read', 'admin', 'app_admin']   |
                null                                                                                        |
                [project: 'a']        |
                [name: 'a', type: 'project_acl']       |
                false
        null   | 'editProjectAclFile'   |
                ['update', 'admin', 'app_admin'] |
                new ProjAclFile(id: 'test.aclpolicy')                                                       |
                [project: 'a']        |
                [name: 'a', type: 'project_acl']       |
                false
        'POST' | 'saveProjectAclFile'   |
                ['update', 'admin', 'app_admin'] |
                new SaveProjAclFile(id: 'test.aclpolicy', create: false, fileText: 'x')                     |
                [project: 'a']        |
                [name: 'a', type: 'project_acl']       |
                false
        'POST' | 'saveProjectAclFile'   |
                ['create', 'admin', 'app_admin'] |
                new SaveProjAclFile(id: 'test.aclpolicy', create: true, fileText: 'x')                      |
                [project: 'a']        |
                [name: 'a', type: 'project_acl']       |
                true
        null   | 'createProjectAclFile' |
                ['create', 'admin', 'app_admin'] |
                null                                                                                        |
                [project: 'a']        |
                [name: 'a', type: 'project_acl']       |
                false
        'POST' | 'deleteProjectAclFile' |
                ['delete', 'admin', 'app_admin'] |
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
        def ctx=AppACLContext.project('aproject')
        def ident = 'a.aclpolicy'
        def validation = new PoliciesValidation(null,null)
        controller.aclFileManagerService = Mock(AclFileManagerService){
            1 * existsPolicyFile(ctx, ident) >> true
            1 * getPolicyFileContents(ctx, ident) >> 'data'
            1 * validateYamlPolicy(ctx, ident, 'data') >> validation
        }
        when:
        def result = controller.loadProjectPolicyValidation(project.name, ident)

        then:
        0 * project.existsFileResource('acls/' + ident)
        0 * project.loadFileResource('acls/' + ident, _)

        result == validation

    }

    @Unroll
    def "save project policy"() {
        given:
        def id = 'test.aclpolicy'
        def ctx=AppACLContext.project(project)
        def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                        def validator = Mock(Validator)
        controller.aclFileManagerService = Mock(AclFileManagerService){
            1 * existsPolicyFile(ctx, id) >> exists
            1 * storePolicyFileContents(ctx, id, fileText) >> 4L
            1 * validateYamlPolicy(ctx,  id, fileText) >>
            new PoliciesValidation( new ValidationSet(valid: true),null)
        }
        controller.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()

        when:
        request.method = 'POST'
        setupFormTokens(params)
        params.project = project
        def result = controller.saveProjectAclFile(input)
        then:
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, _) >> true
        0 * controller.frameworkService.getFrameworkProject(project)

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

            def ctx=AppACLContext.project(project)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)
        controller.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()

        when:
        request.method = 'POST'
        setupFormTokens(params)
        params.project = project
        def result = controller.deleteProjectAclFile(input)
        then:
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.rundeckAuthContextProcessor.authResourceForProjectAcl(project)
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, _) >> true
        1 * controller.frameworkService.existsFrameworkProject(project) >> true
        1 * controller.aclFileManagerService.existsPolicyFile(ctx,id)>>exists
        (exists ? 1 : 0) * controller.aclFileManagerService.deletePolicyFile(ctx, id) >> success
        0 * controller.frameworkService._(*_)
        0 * controller.aclFileManagerService._(*_)

        if(flashMatch) {
            flash.message =~ flashMatch
        }
        if(errCode){
            request.errorCode=~errCode
        }
        response.redirectedUrl == redir
        where:
            fileText    | exists | success | project    | flashMatch    | errCode           | redir
            'test-data' | true   | true    | 'testproj' | 'was deleted' | null              | "/project/$project/admin/acls"
            'test-data' | true   | false   | 'testproj' | null          | 'error'           | "/project/$project/admin/acls"
            'test-data' | false  | false   | 'testproj' | null          | 'was NOT deleted' | null
    }

    @Unroll
    def "delete project policy missing id"() {
        given:
        def id = null
        def input = new ProjAclFile(id: id)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)

        when:
        request.method = 'POST'
        setupFormTokens(params)
        params.project = project
        def result = controller.deleteProjectAclFile(input)
        then:

        0 * controller.frameworkService._(*_)
        0 * controller.aclFileManagerService._(*_)

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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

        controller.aclFileManagerService = Mock(AclFileManagerService)
        controller.configurationService = Mock(ConfigurationService)
        controller.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
        when:
        request.method = 'POST'
        setupFormTokens(params)
        def result = controller.saveSystemAclFile(input)
        then:
        if (fileType == 'fs') {
            1 * controller.frameworkService.isClusterModeEnabled() >> true
            1 * controller.configurationService.getBoolean('clusterMode.acls.localfiles.modify.disabled', true) >> false
        }
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, _) >> true
        if (fileType == 'fs') {
            1 * controller.frameworkService.existsFrameworkConfigFile(id) >> exists
            1 * controller.frameworkService.writeFrameworkConfigFile(id, fileText) >> fileText.bytes.length
        } else {
            1 * controller.aclFileManagerService.existsPolicyFile(AppACLContext.system(),id) >> exists
            1 * controller.aclFileManagerService.storePolicyFileContents(AppACLContext.system(),id, fileText) >> fileText.bytes.length
        }

        1 * controller.aclFileManagerService.validateYamlPolicy(AppACLContext.system(), id, fileText) >>
                new PoliciesValidation( new ValidationSet(valid: true),null)
        0 * controller.frameworkService._(*_)
        0 * controller.configurationService._(*_)
        0 * controller.aclFileManagerService._(*_)
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)
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
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, _) >> true
        if (fileType == 'fs') {
            1 * controller.frameworkService.existsFrameworkConfigFile(id) >> exists
            0 * controller.frameworkService.writeFrameworkConfigFile(id, fileText) >> fileText.bytes.length
        } else {
            1 * controller.aclFileManagerService.existsPolicyFile(AppACLContext.system(),id) >> exists
            0 * controller.aclFileManagerService.storePolicyFileContents(AppACLContext.system(),id, fileText) >> fileText.bytes.length
        }

        0 * controller.aclFileManagerService.validatePolicyFile(AppACLContext.system(),id, fileText) >>
                new PoliciesValidation( new ValidationSet(valid: true),null)
        0 * controller.frameworkService._(*_)
        0 * controller.configurationService._(*_)
        0 * controller.aclFileManagerService._(*_)
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)

        when:
        request.method = 'POST'
        setupFormTokens(params)
        def result = controller.deleteSystemAclFile(input)
        then:
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, ['delete','admin', 'app_admin']) >> true
        0 * controller.frameworkService._(*_)
        0 * controller.aclFileManagerService._(*_)

        flash.message == null
        view == '/common/error'
        where:
        id   | fileType
        null | 'fs'
        1    | null
    }


    def "projectAcls does not load metadata for policies"() {
        given:
        def id = 'test.aclpolicy'
        def project = 'test'
        def ctx=AppACLContext.project(project)
        // def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)

        when:
        params.project = project
        def result = controller.projectAcls()
        then:
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, _) >> true
        0 * controller.frameworkService.getFrameworkProject(project)
        1 * controller.aclFileManagerService.listStoredPolicyFiles(ctx)>>['test.aclpolicy']
        0 * controller.aclFileManagerService.validatePolicyFile(*_)
        result
        result.acllist
        result.acllist.size == 1
        result.acllist[0].id==id
        result.acllist[0].name=='test'
        result.acllist[0].valid
    }
    def "projectAcls are sorted by name"() {
        given:
        def id = 'atest.aclpolicy'
        def id2 = 'ztest.aclpolicy'
        def project = 'test'
        def ctx=AppACLContext.project(project)
        // def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)

        when:
        params.project = project
        def result = controller.projectAcls()
        then:
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, _) >> true
        0 * controller.frameworkService.getFrameworkProject(project)
        1 * controller.aclFileManagerService.listStoredPolicyFiles(ctx)>>[id2,id]
        0 * controller.aclFileManagerService.validatePolicyFile(*_)
        result
        result.acllist
        result.acllist.size == 2
        result.acllist[0].id==id
        result.acllist[0].name=='atest'
        result.acllist[0].valid
        result.acllist[1].id==id2
        result.acllist[1].name=='ztest'
        result.acllist[1].valid
    }

    def "ajaxProjectAclMeta loads metadata for policies"() {
        given:
            def id = 'test.aclpolicy'
            def project = 'test'
            def ctx=AppACLContext.project(project)
            // def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
            controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                        def description = 'description'
            def policyM = Mock(Policy) {
                getDescription() >> description
            }
            PolicyCollection policies = Mock(PolicyCollection) {
                getPolicies() >> [policyM]
                countPolicies() >> 1
            }
            PoliciesValidation policy = new PoliciesValidation( new ValidationSet(valid: true),policies)
            controller.aclFileManagerService = Mock(AclFileManagerService){

                1 * existsPolicyFile(ctx,id)>>exists
                1 * validateYamlPolicy(ctx, id, _) >> policy
            }

        when:
            request.method = 'POST'
            request.addHeader('x-rundeck-ajax', 'true')
            request.json=[files:[id]]
            params.project = project
            def result = controller.ajaxProjectAclMeta()
        then:
            1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, _) >> true
            0 * controller.frameworkService.getFrameworkProject(project)
            response.json
            response.json
            response.json.size() == 1
            response.json[0].id==id
            response.json[0].name=='test'
            response.json[0].valid
            response.json[0].meta
            response.json[0].meta.policies
            response.json[0].meta.policies.size() == 1
            response.json[0].meta.policies[0].description == description
        where:
            exists = true
    }
    def "ajaxProjectAclMeta not found"() {
        given:
            def project = 'test'
            def id = 'test.aclpolicy'
            def ctx=AppACLContext.project(project)
            // def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
            controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                        controller.aclFileManagerService = Mock(AclFileManagerService)

        when:
            request.method = 'POST'
            request.addHeader('x-rundeck-ajax', 'true')
            request.json=[files:[id]]
            params.project = project
            def result = controller.ajaxProjectAclMeta()
        then:
            1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, _) >> true
            0 * controller.frameworkService.getFrameworkProject(project)
            1 * controller.aclFileManagerService.existsPolicyFile(ctx, id) >> exists
            0 * controller.aclFileManagerService.validatePolicyFile(project, id, _)
            response.json
            response.json
            response.json.size() == 1
            response.json[0].id==id
            response.json[0].name=='test'
            !response.json[0].valid
            response.json[0].validation==[(id):["Not found"]]
            !response.json[0].meta
        where:
            exists = false
    }

    @Unroll
    def "RdAuthorizeApplicationType required for endpoint #endpoint authorize type #type access #access"() {
        when:
            def result = getControllerMethodAnnotation(endpoint, RdAuthorizeApplicationType)
        then:
            result.type() == type
            result.access() == access
        where:
            endpoint | type                          | access
            'acls'   | AuthConstants.TYPE_SYSTEM_ACL | RundeckAccess.General.AUTH_ADMIN_OR_READ
    }

    def "acls does not load metadata for policies"() {
        given:
        def id = 'test.aclpolicy'
        File confdir= Files.createTempDirectory('menuControllerSpec').toFile()
        // def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
        controller.frameworkService = Mock(FrameworkService){
            getFrameworkConfigDir()>>confdir
        }
        controller.aclFileManagerService = Mock(AclFileManagerService){
            1 * listStoredPolicyFiles(AppACLContext.system())>>[id]
        }
        controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
            1 * system(_) >> Mock(AuthorizingSystem){
                isAuthorized(RundeckAccess.General.OPS_ADMIN) >> true
                isAuthorized(RundeckAccess.General.APP_ADMIN) >> true
            }
            0 * _(*_)
        }



        when:
        def result = controller.acls()
        then:
        0 * controller.aclFileManagerService.validatePolicyFile(*_)
        result
        result.fwkConfigDir==confdir
        result.aclFileList==[]
        result.aclStoredList
        result.aclStoredList.size() == 1
        result.aclStoredList[0].id==id
        result.aclStoredList[0].name=='test'
        result.aclStoredList[0].valid
    }

    def "acls are sorted by name"() {
        given:
        def id = 'test.aclpolicy'
        def id2 = 'btest.aclpolicy'
        File confdir= Files.createTempDirectory('menuControllerSpec').toFile()
        // def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
        controller.frameworkService = Mock(FrameworkService){
            getFrameworkConfigDir()>>confdir
        }
        controller.aclFileManagerService = Mock(AclFileManagerService){
            1 * listStoredPolicyFiles(AppACLContext.system())>>[id,id2]
        }

        controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
            1 * system(_) >> Mock(AuthorizingSystem){
                isAuthorized(RundeckAccess.General.OPS_ADMIN) >> true
                isAuthorized(RundeckAccess.General.APP_ADMIN) >> true
            }
            0 * _(*_)
        }


        when:
        def result = controller.acls()
        then:
        0 * controller.aclFileManagerService.validatePolicyFile(*_)
        result
        result.fwkConfigDir==confdir
        result.aclFileList==[]
        result.aclStoredList
        result.aclStoredList.size() == 2
        result.aclStoredList[0].id==id2
        result.aclStoredList[0].name=='btest'
        result.aclStoredList[0].valid
        result.aclStoredList[1].id==id
        result.aclStoredList[1].name=='test'
        result.aclStoredList[1].valid
    }

    def "acls are sorted by name, no ops admin"() {
        given:
        def id = 'test.aclpolicy'
        def id2 = 'btest.aclpolicy'
        File confdir= Files.createTempDirectory('menuControllerSpec').toFile()
        File test1 = Files.createTempFile(confdir.toPath(), "test1.aclpolicy", ".aclpolicy").toFile()
        File test2 = Files.createTempFile(confdir.toPath(), "test2.aclpolicy", ".aclpolicy").toFile()
        // def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
        controller.frameworkService = Mock(FrameworkService){
            getFrameworkConfigDir()>>confdir
            isClusterModeEnabled()>>true
            }
        controller.aclFileManagerService = Mock(AclFileManagerService){
            1 * listStoredPolicyFiles(AppACLContext.system())>>[id,id2]
            forContext(_)>>Mock(ACLFileManager){
                getValidator()>>Mock(Validator) {
                    validateYamlPolicy( _, _) >> Mock(RuleSetValidation) {
                        isValid() >> true
                    }
                }
            }
        }


        controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
            1 * system(_) >> Mock(AuthorizingSystem){
                isAuthorized(RundeckAccess.General.OPS_ADMIN) >> false
                isAuthorized(RundeckAccess.General.APP_ADMIN) >> true
            }
            0 * _(*_)
        }


        when:
        def result = controller.acls()
        then:
        0 * controller.aclFileManagerService.validatePolicyFile(*_)
        result
        result.fwkConfigDir==confdir
        result.aclFileList==[]
        result.aclStoredList
        result.aclStoredList.size() == 2
        result.aclStoredList[0].id==id2
        result.aclStoredList[0].name=='btest'
        result.aclStoredList[0].valid
        result.aclStoredList[1].id==id
        result.aclStoredList[1].name=='test'
        result.aclStoredList[1].valid
    }

    def "ajaxSystemAcls loads metadata for policies"() {
        given:
            def id = 'test.aclpolicy'
            File confdir= Files.createTempDirectory('menuControllerSpec').toFile()
            // def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
            controller.frameworkService = Mock(FrameworkService){
                getFrameworkConfigDir()>>confdir
            }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                        controller.aclFileManagerService = Mock(AclFileManagerService){
                0 * listStoredPolicyFiles()
            }
            def description = 'description'
            def policyM = Mock(Policy) {
                getDescription() >> description
            }


            PolicyCollection policies = Mock(PolicyCollection) {
                getPolicies() >> [policyM]
                countPolicies() >> 1
            }
            PoliciesValidation policy = new PoliciesValidation( new ValidationSet(valid: true),policies)

        when:
            request.method = 'POST'
            request.addHeader('x-rundeck-ajax', 'true')
            request.json=[files:[id]]
            def result = controller.ajaxSystemAclMeta()
        then:
            1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, _) >> true

            1 * controller.aclFileManagerService.validatePolicyFile(AppACLContext.system(),id) >> policy
            response.json
            response.json
            response.json.size() == 1
            response.json[0].id==id
            response.json[0].name=='test'
            response.json[0].valid
            response.json[0].meta
            response.json[0].meta.policies
            response.json[0].meta.policies.size() == 1
            response.json[0].meta.policies[0].description == description
        where:
            exists = true
    }
    def "ajaxSystemAcls not found"() {
        given:
            def id = 'test.aclpolicy'
            File confdir= Files.createTempDirectory('menuControllerSpec').toFile()
            // def input = new SaveProjAclFile(id: id, fileText: fileText, create: create)
            controller.frameworkService = Mock(FrameworkService){
                getFrameworkConfigDir()>>confdir
            }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                        controller.aclFileManagerService = Mock(AclFileManagerService){
                1 * validatePolicyFile(AppACLContext.system(),id)>>null
                0 * _(*_)
            }


        when:
            request.method = 'POST'
            request.addHeader('x-rundeck-ajax', 'true')
            request.json=[files:[id]]
            def result = controller.ajaxSystemAclMeta()
        then:
            1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, _) >> true
            response.json
            response.json.size() == 1
            response.json[0].id==id
            response.json[0].name=='test'
            !response.json[0].valid
            !response.json[0].meta
            response.json[0].validation == [(id):['Not found']]
        where:
            exists = true
    }

    def "homeAjax get description field on project table"() {
        given:

        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
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
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.frameworkService.projectNames(_) >> []
        1 * controller.frameworkService.projects(_) >> projects
        0 * iproj.hasProperty('project.description')
        description == response.json.projects[0].description
    }

    def "homeAjax dont fail on project not created yet"() {
        given:

        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
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
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
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
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.frameworkService.projectNames(_) >> []
        1 * controller.frameworkService.projects(_) >> projects
        1 * iproj.hasProperty('project.description') >> true
        1 * iproj.getProperty('project.description') >> description
        description == response.json.projects[0].description
    }

    def "homeAjax evaluate project auth"() {
        given:
        def description = 'desc'
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    new Project(name: 'proj').save(flush: true)
        def iproj = Mock(IRundeckProject) {
            getName() >> 'proj'
        }
        def projects = [iproj]
        controller.configurationService = Mock(ConfigurationService)
        controller.menuService = Mock(MenuService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)

        request.addHeader('x-rundeck-ajax', 'true')
        def systemAuth=Mock(UserAndRolesAuthContext)
        def projectAuth=Mock(UserAndRolesAuthContext)

        when:
        controller.homeAjax()

        then:
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)>>systemAuth
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, 'proj') >> projectAuth
            1 * controller.
                rundeckAuthContextProcessor.
                authorizeProjectResource(projectAuth, AuthConstants.RESOURCE_TYPE_EVENT, AuthConstants.ACTION_READ, 'proj')>>authEventRead
            1 * controller.
                rundeckAuthContextProcessor.authorizeProjectResource(
                projectAuth,
                AuthConstants.RESOURCE_TYPE_JOB,
                AuthConstants.ACTION_CREATE,
                'proj'
            )>>authJobCreate
            1 * controller.
                rundeckAuthContextProcessor.authResourceForProject('proj')
            1 * controller.
                rundeckAuthContextProcessor.
                authorizeApplicationResourceAny(
                    projectAuth,
                    _,
                    [
                        AuthConstants.ACTION_CONFIGURE,
                        AuthConstants.ACTION_ADMIN,
                        AuthConstants.ACTION_APP_ADMIN,
                        AuthConstants.ACTION_IMPORT,
                        AuthConstants.ACTION_EXPORT,
                        AuthConstants.ACTION_DELETE
                    ]
                )>> authAdmin
        0 * controller.rundeckAuthContextProcessor._(*_)
        1 * controller.frameworkService.projectNames(_) >> ['proj']
        1 * controller.frameworkService.projects(_) >> projects
        def json=response.json
        json.projects[0].auth.jobCreate==authJobCreate
        json.projects[0].auth.admin==authAdmin
        if(!authEventRead){
            json.projects[0].execCount== 0
            json.projects[0].failedCount== 0
            json.projects[0].userSummary== []
            json.projects[0].userCount==0
        }
        where:
            authAdmin | authJobCreate | authEventRead
            true      | true          | true
            false     | true          | true
            true      | false         | true
            true      | true          | false
    }

    def "list Export"() {
        given:
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)
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

        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist : []]
        1 * controller.scheduledExecutionService.finishquery(_,_,_) >> [max: 20,
                                                                        offset:0,
                                                                        paginateParams:[:],
                                                                        displayParams:[:]]
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN,AuthConstants.ACTION_APP_ADMIN,
                                                                               AuthConstants.ACTION_EXPORT,
                                                                                AuthConstants.ACTION_SCM_EXPORT]) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN,AuthConstants.ACTION_APP_ADMIN,
                                                                               AuthConstants.ACTION_IMPORT,
                                                                               AuthConstants.ACTION_SCM_IMPORT]) >> true
        1 * controller.scmService.projectHasConfiguredExportPlugin(project) >> true
        1 * controller.scmService.projectHasConfiguredImportPlugin(project) >> false
        1 * controller.scmService.loadScmConfig(project,'export') >> scmConfig

        response.json
        response.json.scmExportEnabled
        !response.json.scmImportEnabled
    }

    @Unroll
    def "list export calls exportStatusForJobs when export is enabled"() {
        given:
        controller.frameworkService = Mock(FrameworkService){
            isClusterModeEnabled() >> true
        }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        controller.aclFileManagerService = Mock(AclFileManagerService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        def project = 'test'
        def scmConfig = Mock(ScmPluginConfigData)

        when:
        request.method = 'POST'
        request.JSON = []
        request.format = 'json'
        params.project = project
        controller.listExport()
        then:

        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist : []]
        1 * controller.scheduledExecutionService.finishquery(_,_,_) >> [max: 20,
                                                                        offset:0,
                                                                        paginateParams:[:],
                                                                        displayParams:[:]]
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN,AuthConstants.ACTION_APP_ADMIN,
                                                                               AuthConstants.ACTION_EXPORT,
                                                                               AuthConstants.ACTION_SCM_EXPORT]) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN,AuthConstants.ACTION_APP_ADMIN,
                                                                               AuthConstants.ACTION_IMPORT,
                                                                                AuthConstants.ACTION_SCM_IMPORT]) >> true
        1 * controller.scmService.projectHasConfiguredExportPlugin(project) >> true
        1 * controller.scmService.loadScmConfig(project,'export') >> scmConfig
        1 * scmConfig.getEnabled() >> enabled
        (count) * controller.scmService.getJobsPluginMeta(project, true)
        (count) * controller.scmService.exportStatusForJobs(project,_, _, _, _)
        (count) * controller.scmService.exportPluginStatus(_,project)
        (count) * controller.scmService.exportPluginActions(_,project)
        (count) * controller.scmService.getRenamedJobPathsForProject(project)

        0 * controller.scmService.initProject(project,'export')
        0 * controller.scmService.initProject(project,'import')

        response.json
        response.json.scmExportEnabled==enabled
        !response.json.scmImportEnabled
        where:
            enabled | count
            true    | 1
            false   | 0
    }
    @Unroll
    def "list export calls importStatusForJobs when import is enabled"() {
        given:
        controller.frameworkService = Mock(FrameworkService){
            isClusterModeEnabled() >> true
        }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        controller.aclFileManagerService = Mock(AclFileManagerService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        def project = 'test'
        def scmConfig = Mock(ScmPluginConfigData)

        when:
        request.method = 'POST'
        request.JSON = []
        request.format = 'json'
        params.project = project
        controller.listExport()
        then:

        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist : []]
        1 * controller.scheduledExecutionService.finishquery(_,_,_) >> [max: 20,
                                                                        offset:0,
                                                                        paginateParams:[:],
                                                                        displayParams:[:]]
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN,
                                                                               AuthConstants.ACTION_EXPORT,
                                                                               AuthConstants.ACTION_SCM_EXPORT]) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN,
                                                                               AuthConstants.ACTION_IMPORT,
                                                                                AuthConstants.ACTION_SCM_IMPORT]) >> true
        1 * controller.scmService.projectHasConfiguredExportPlugin(project) >> false
        1 * controller.scmService.projectHasConfiguredImportPlugin(project) >> true
        1 * controller.scmService.loadScmConfig(project,'import') >> scmConfig
        1 * scmConfig.getEnabled() >> enabled
        (count) * controller.scmService.getJobsPluginMeta(project, false)
        (count) * controller.scmService.importStatusForJobs(project,_, _, _, _)
        (count) * controller.scmService.importPluginStatus(_,project)
        (count) * controller.scmService.importPluginActions(_,project,_)
        0 * controller.scmService.getRenamedJobPathsForProject(project)

        0 * controller.scmService.initProject(project,'export')
        0 * controller.scmService.initProject(project,'import')

        response.json
        !response.json.scmExportEnabled
        response.json.scmImportEnabled==enabled
        where:
            enabled | count
            true    | 1
            false   | 0
    }


    def "project Toggle SCM off"(){
        given:
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)
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
        1 * controller.scmService.loadScmConfig(project, 'export')
        1 * controller.scmService.loadScmConfig(project, 'import')

        0 * controller.scmService.disablePlugin(_, project, _)
        0 * controller.scmService.enablePlugin(_, _, project, _)

        response.status == 302
    }

    def "homeAjax get project label"() {
        given:

        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
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
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.frameworkService.projectNames(_) >> []
        1 * controller.frameworkService.projects(_) >> projects
        1 * iproj.hasProperty('project.label') >> true
        1 * iproj.getProperty('project.label') >> 'label'
        'label' == response.json.projects[0].label
    }


    def "jobs jobListIds"() {
        given:
        controller.configurationService = Mock(ConfigurationService)
        controller.jobListLinkHandlerRegistry = Mock(JobListLinkHandlerRegistry) {
            getJobListLinkHandlerForProject(_) >> new GroupedJobListLinkHandler()
        }
        def testUUID = UUID.randomUUID().toString()
        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager)
            }
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        controller.userService = Mock(UserService)
        controller.jobSchedulesService = Mock(JobSchedulesService)
        controller.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
        def query = new ScheduledExecutionQuery()
        params.project='test'
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))

        when:
        def model = controller.jobs(query)
        then:
        1 * controller.rundeckAuthContextProcessor.authResourceForJob(_) >>
                [authorized: true, action: AuthConstants.ACTION_READ, resource: job1]
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_, _, _, _) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    : AuthConstants.ACTION_READ,
                                                                                   resource  : [group: job1.groupPath, name: job1.jobName]]]
        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist: [job1]]
        1 * controller.scheduledExecutionService.finishquery(_, _, _) >> [max           : 20,
                                                                          offset        : 0,
                                                                          paginateParams: [:],
                                                                          displayParams : [:]]
        model.jobListIds.size() == model.nextScheduled.size()
        model.jobListIds.get(0) == model.nextScheduled.get(0).extid
    }

    @Unroll
    def "jobs scheduledJobListIds"() {
        given:
        controller.configurationService = Mock(ConfigurationService)
        controller.jobListLinkHandlerRegistry = Mock(JobListLinkHandlerRegistry) {
            getJobListLinkHandlerForProject(_) >> new GroupedJobListLinkHandler()
        }
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager)
            }
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        controller.userService = Mock(UserService)
        controller.jobSchedulesService = Mock(JobSchedulesService)
        controller.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()

        def query = new ScheduledExecutionQuery()
        params.project='test'
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        ScheduledExecution job2 = new ScheduledExecution(createJobParams(jobName: 'job2', uuid:testUUID2,scheduled:false))

        when:
        def model = controller.jobs(query)
        then:
        2 * controller.rundeckAuthContextProcessor.authResourceForJob(_) >>
                [authorized: true, action: AuthConstants.ACTION_READ]
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_, _, _, _) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    : AuthConstants.ACTION_READ,
                                                                                   resource  : [group: job1.groupPath, name: job1.jobName]],
                                                                                  [authorized: true,
                                                                                   action    : AuthConstants.ACTION_READ,
                                                                                   resource  : [group: job2.groupPath, name: job2.jobName]]]
        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist: [job1,job2]]
        1 * controller.scheduledExecutionService.finishquery(_, _, _) >> [max           : 20,
                                                                          offset        : 0,
                                                                          paginateParams: [:],
                                                                          displayParams : [:]]
        1 * controller.jobSchedulesService.isScheduled(testUUID) >> aScheduled
        1 * controller.jobSchedulesService.isScheduled(testUUID2) >> bScheduled
        model.jobListIds.size() == 2
        model.jobListIds.size() == model.nextScheduled.size()
        model.jobListIds == model.nextScheduled*.extid
        model.scheduledJobListIds.size() == count
        model.scheduledJobListIds.size() == model.scheduledJobs.size()
        model.scheduledJobListIds == model.scheduledJobs*.extid

        where:
            aScheduled | bScheduled | count
            true       | true       | 2
            true       | false      | 1
            false      | true       | 1
            false      | false      | 0
    }

    @Unroll
    def "next scheduled jobs pagination"() {
        given:

        params.project='test'
        def job1 = new ScheduledExecution(createJobParams(jobName:'another job1',groupPath: '', project: params.project)).save()
        def job2 = new ScheduledExecution(createJobParams(jobName:'another job2',groupPath:'', project: params.project)).save()
        def job3 = new ScheduledExecution(createJobParams(jobName:'another job3',groupPath:'', project: params.project)).save()
        def job4 = new ScheduledExecution(createJobParams(jobName:'another job4',groupPath:'', project: params.project)).save()
        def job5 = new ScheduledExecution(createJobParams(jobName:'another job5',groupPath:'', project: params.project)).save()
        def job6 = new ScheduledExecution(createJobParams(jobName:'another job6',groupPath:'', project: params.project)).save()
        def job7 = new ScheduledExecution(createJobParams(jobName:'another job7',groupPath:'', project: params.project)).save()

        controller.configurationService = new ConfigurationService()
        controller.aclFileManagerService = Mock(AclFileManagerService)
        controller.scheduledExecutionService = new ScheduledExecutionService()
        controller.scmService = Mock(ScmService)
        controller.userService = Mock(UserService)
        controller.jobSchedulesService = Mock(JobSchedulesService)
        controller.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
        controller.scheduledExecutionService.applicationContext = applicationContext

        controller.jobListLinkHandlerRegistry = Mock(JobListLinkHandlerRegistry) {
            getJobListLinkHandlerForProject(_) >> new GroupedJobListLinkHandler()
        }

        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager)
                isClusterModeEnabled() >> false
            }

        }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            getAuthContextForUserAndRolesAndProject(_,_,_)>>Mock(UserAndRolesAuthContext)
            authorizeProjectResource(_, _, _, _) >> true
            authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job1.groupPath, name: job1.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job2.groupPath, name: job2.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job3.groupPath, name: job3.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job4.groupPath, name: job4.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job5.groupPath, name: job5.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job6.groupPath, name: job6.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job7.groupPath, name: job7.jobName]]]
        }

        controller.scheduledExecutionService.frameworkService = controller.frameworkService


        def query = new ScheduledExecutionQuery()

        grailsApplication.config.rundeck.gui.paginatejobs.enabled = requirePagination
        grailsApplication.config.rundeck.gui.paginatejobs.max.per.page = maxPerPage
        controller.configurationService.setAppConfig(grailsApplication.config.rundeck)
        query.offset = offset

        when:
        def model = controller.jobs(query)

        then:
        model.nextScheduled.size() == count

        where:
        requirePagination | offset | maxPerPage | count
        true              | 5      | "3"        | 2
        false             | 3      | "3"        | 7
        true              | 0      | "3"        | 3
        true              | 7      | "3"        | 0
        false             | 5      | "5"        | 7
        true              | 4      | "3"        | 3
        true              | 6      | "3"        | 1

    }

    @Unroll
    def "api jobs list pagination"() {
        given:

        params.project='test'
        def job1 = new ScheduledExecution(createJobParams(jobName:'another job1',groupPath: '', project: params.project)).save()
        def job2 = new ScheduledExecution(createJobParams(jobName:'another job2',groupPath:'', project: params.project)).save()
        def job3 = new ScheduledExecution(createJobParams(jobName:'another job3',groupPath:'', project: params.project)).save()
        def job4 = new ScheduledExecution(createJobParams(jobName:'another job4',groupPath:'', project: params.project)).save()
        def job5 = new ScheduledExecution(createJobParams(jobName:'another job5',groupPath:'', project: params.project)).save()
        def job6 = new ScheduledExecution(createJobParams(jobName:'another job6',groupPath:'', project: params.project)).save()
        def job7 = new ScheduledExecution(createJobParams(jobName:'another job7',groupPath:'', project: params.project)).save()

        controller.configurationService = new ConfigurationService()
        controller.apiService = Mock(ApiService){
            requireApi(_, _) >> true
            requireExists(_,_,_) >> true
        }
        controller.scheduledExecutionService = new ScheduledExecutionService()
        controller.jobSchedulesService = Mock(JobSchedulesService)
        controller.scheduledExecutionService.applicationContext = applicationContext

        controller.jobListLinkHandlerRegistry = Mock(JobListLinkHandlerRegistry) {
            getJobListLinkHandlerForProject(_) >> new GroupedJobListLinkHandler()
        }

        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager)
                isClusterModeEnabled() >> false
            }

        }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            getAuthContextForUserAndRolesAndProject(_,_,_)>>Mock(UserAndRolesAuthContext)
            authorizeProjectResource(_, _, _, _) >> true
            authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job1.groupPath, name: job1.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job2.groupPath, name: job2.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job3.groupPath, name: job3.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job4.groupPath, name: job4.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job5.groupPath, name: job5.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job6.groupPath, name: job6.jobName]],
                                                      [authorized: true,
                                                       action    : AuthConstants.ACTION_READ,
                                                       resource  : [group: job7.groupPath, name: job7.jobName]]]
        }

        controller.scheduledExecutionService.frameworkService = controller.frameworkService


        def query = new ScheduledExecutionQuery()
        request.api_version = ApiVersions.V18

        grailsApplication.config.rundeck.gui.paginatejobs.enabled = guiPagEnabled
        grailsApplication.config.rundeck.gui.paginatejobs.max.per.page = 5
        if(apiPagDisabled) grailsApplication.config.rundeck.api.paginatejobs.enabled = false
        else grailsApplication.config.rundeck.api.paginatejobs = null
        controller.configurationService.setAppConfig(grailsApplication.config.rundeck)
        query.offset = offset
        query.max = apiPagMax

        when:
        def model = controller.apiJobsListv2(query)

        then:
        response.xml.count == count

        where:
        guiPagEnabled | apiPagDisabled| offset | apiPagMax  | count
        true          | false         | 5      | 3          | 2
        false         | true          | 3      | 3          | 7
        true          | false         | 0      | 3          | 3
        true          | false         | 7      | 3          | 0
        false         | true          | 5      | 5          | 7
        true          | false         | 4      | 3          | 3
        true          | false         | 6      | 3          | 1

    }

    def "jobs list next execution times"() {
        given:
        controller.configurationService = Mock(ConfigurationService)
        controller.jobListLinkHandlerRegistry = Mock(JobListLinkHandlerRegistry) {
            getJobListLinkHandlerForProject(_) >> new GroupedJobListLinkHandler()
        }
        def testUUID = UUID.randomUUID().toString()
        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager)
            }
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.aclFileManagerService = Mock(AclFileManagerService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        controller.userService = Mock(UserService)
        controller.jobSchedulesService = Mock(JobSchedulesService)
        controller.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
        def query = new ScheduledExecutionQuery()
        params.project='test'
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID)).save()

        when:
        def model = controller.jobs(query)
        then:
        1 * controller.rundeckAuthContextProcessor.authResourceForJob(_) >>
                [authorized: true, action: AuthConstants.ACTION_READ, resource: job1]
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_, _, _, _) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    : AuthConstants.ACTION_READ,
                                                                                   resource  : [group: job1.groupPath, name: job1.jobName]]]
        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist: [job1]]
        1 * controller.scheduledExecutionService.finishquery(_, _, _) >> [max           : 20,
                                                                          offset        : 0,
                                                                          paginateParams: [:],
                                                                          displayParams : [:]]
        1 * controller.jobSchedulesService.isScheduled(testUUID) >> true
        1 * controller.scheduledExecutionService.nextExecutionTimes([job1]) >> [(job1.id):new Date()]
        model.nextExecutions!=null
        model.nextExecutions[job1.id]!=null
    }


    @Unroll
    def "jobs Fragment with scm active"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        ScmPluginConfig data = Mock(ScmPluginConfig)
        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager)
            }
        }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            authorizeApplicationResourceAny(_,_,_)>>true
        }
        controller.aclFileManagerService = Mock(AclFileManagerService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        controller.userService = Mock(UserService)
        controller.jobSchedulesService = Mock(JobSchedulesService)
        def query = new ScheduledExecutionQuery()
        params.project='test'
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))

        when:
        def model = controller.jobsFragment(query, option)
        then:
        1 * controller.rundeckAuthContextProcessor.authResourceForJob(_) >>
                [authorized: true, action: AuthConstants.ACTION_READ, resource: job1]
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_, _, _, _) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                   action    : AuthConstants.ACTION_READ,
                                                                                   resource  : [group: job1.groupPath, name: job1.jobName]]]
        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist: [job1]]
        1 * controller.scheduledExecutionService.finishquery(_, _, _) >> [max           : 20,
                                                                          offset        : 0,
                                                                          paginateParams: [:],
                                                                          displayParams : [:]]
        (expected) * controller.scmService.loadScmConfig(_,'export')>>Mock(ScmPluginConfig){
            getEnabled()>>eenabled
            getType()>>(econfiged?'atype':null)
        }
        (expected) * controller.scmService.loadScmConfig(_,'import')>>Mock(ScmPluginConfig){
            getEnabled()>>ienabled
            getType()>>(iconfiged?'btype':null)
        }
        (ienabled?expected:0) * controller.scmService.projectHasConfiguredPlugin('import','test')>>iconfiged
        (eenabled?expected:0) * controller.scmService.projectHasConfiguredPlugin('export','test')>>econfiged
        (econfiged?expected:0) * controller.scmService.getPluginDescriptor('export','atype')>>new DescribedPlugin(null,null,null, null, null)
        (iconfiged?expected:0) * controller.scmService.getPluginDescriptor('import','btype')>>new DescribedPlugin(null,null,null, null, null)
        model.hasConfiguredScmPlugins==hasConfigured
        model.hasConfiguredScmPluginsEnabled==hasEnabled
        where:
            option                             | hasConfigured | hasEnabled  | ienabled | iconfiged | eenabled | econfiged | expected
            MenuController.JobsScmInfo.MINIMAL | true          | true        | true     | true      | true     | true      | 1
            MenuController.JobsScmInfo.MINIMAL | true          | true        | false    | false     | true     | true      | 1
            MenuController.JobsScmInfo.MINIMAL | true          | true        | true     | true      | false    | false     | 1
            MenuController.JobsScmInfo.MINIMAL | false         | false       | true     | false     | true     | false     | 1
            MenuController.JobsScmInfo.MINIMAL | false         | false       | false    | false     | false    | false     | 1
            MenuController.JobsScmInfo.MINIMAL | true          | false       | false    | true      | false    | true      | 1
            MenuController.JobsScmInfo.MINIMAL | true          | true        | false    | true      | true     | true      | 1
            MenuController.JobsScmInfo.MINIMAL | true          | true        | true     | true      | false    | true      | 1
            MenuController.JobsScmInfo.NONE    | null          | null        | false    | false     | false    | false     | 0
    }

    def "user summary"() {
        given:
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext)
        controller.frameworkService=Mock(FrameworkService){
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_USER,[AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
                1 * getAuthContextForSubject(_)>>auth
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

    }


    def "api job forecast xml"() {
        given:
        def testUUID = UUID.randomUUID().toString()
        def testUUID2 = UUID.randomUUID().toString()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime=200*1000
        job1.execCount=100
        job1.scheduled=true
        job1.save()
        def jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> job1.shouldScheduleExecution()
        }
        controller.jobSchedulesService = jobSchedulesService
        controller.scheduledExecutionService.jobSchedulesService = jobSchedulesService

        when:
        controller.response.format = "xml"
        params.id=testUUID
        def result = controller.apiJobForecast()

        then:
        1 * controller.apiService.requireApi(_, _, 31) >> true
        1 * controller.apiService.requireParameters(_, _, ['id']) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(testUUID) >> job1
        1 * controller.apiService.requireExists(_, job1, _) >> true
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, 'AProject') >>
                Mock(UserAndRolesAuthContext)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_, job1, ['read','view'], 'AProject') >> true
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime=200*1000
        job1.execCount=100
        job1.scheduled=true
        job1.save()
        def jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> job1.shouldScheduleExecution()
        }
        controller.jobSchedulesService = jobSchedulesService
        controller.scheduledExecutionService.jobSchedulesService = jobSchedulesService

        when:
        params.id=testUUID
        response.format='json'
        def result = controller.apiJobForecast()

        then:
        1 * controller.apiService.requireApi(_, _, 31) >> true
        1 * controller.apiService.requireParameters(_, _, ['id']) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(testUUID) >> job1
        1 * controller.apiService.requireExists(_, job1, _) >> true
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, 'AProject') >>
                Mock(UserAndRolesAuthContext)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_, job1, ['read','view'], 'AProject') >> true
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', uuid:testUUID))
        job1.serverNodeUUID = testUUID2
        job1.totalTime=200*1000
        job1.execCount=100
        job1.scheduled=true
        job1.save()
        def jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> job1.shouldScheduleExecution()
        }
        controller.jobSchedulesService = jobSchedulesService
        controller.scheduledExecutionService.jobSchedulesService = jobSchedulesService

        when:
        request.api_version = 32
        params.id=testUUID
        params.past='true'
        response.format='json'
        def result = controller.apiJobForecast()

        then:
        1 * controller.apiService.requireApi(_, _, 31) >> true
        1 * controller.apiService.requireParameters(_, _, ['id']) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(testUUID) >> job1
        1 * controller.apiService.requireExists(_, job1, _) >> true
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, 'AProject') >>
                Mock(UserAndRolesAuthContext)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_, job1, ['read','view'], 'AProject') >> true
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

    @Unroll
    def "nowrunningAjax requires authz check"() {
        given:
        controller.frameworkService = Mock(FrameworkService)
        controller.apiService = Mock(ApiService) {
            _ * renderErrorFormat(_, { it.status == 403 }) >> {
                it[0].status = 403
            }
            1 * requireExists(_, true, ['project', 'aProject']) >> true
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    params.project = 'aProject'
        params.projFilter = 'aProject'
        def action = 'read'
        request.addHeader('x-rundeck-ajax','true')
        when:
        controller.nowrunningAjax()
        then:
        response.status == 403
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_, _, action, 'aProject')
        1 * controller.frameworkService.existsFrameworkProject('aProject') >> true
    }
    @Unroll
    def "apiExecutionsRunning requires authz check"() {
        given:
        controller.frameworkService = Mock(FrameworkService)
        controller.apiService = Mock(ApiService) {
            _ * renderErrorFormat(_, { it.status == 403 }) >> {
                it[0].status = 403
            }
            1 * requireApi(_,_,14)>>true
            1 * requireExists(_, true, ['project', 'aProject']) >> true
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    params.project = 'aProject'
        params.projFilter = 'aProject'
        def action = 'read'
        when:
        controller.apiExecutionsRunningv14()
        then:
        response.status == 403
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_, _, action, 'aProject')
        1 * controller.frameworkService.existsFrameworkProject('aProject') >> true

    }

    @Unroll
    def "apiExecutionsRunning not found project"() {
        given:
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
                    controller.apiService = Mock(ApiService) {
            1 * requireApi(_,_,14)>>true
            1 * requireExists(_, false, ['project', 'aProject']) >> {
                it[0].status=404
                return false
            }
            0 * _ (*_)
        }
        params.project = 'aProject'
        params.projFilter = 'aProject'
        def action = 'read'
        when:
        controller.apiExecutionsRunningv14()
        then:
        response.status == 404
        0 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_, _, action, 'aProject')
        1 * controller.frameworkService.existsFrameworkProject('aProject') >> false

    }

    def "test project job list handler"() {
        given:
        controller.configurationService = Mock(ConfigurationService)
        controller.scmService = Mock(ScmService)
        controller.frameworkService = Mock(FrameworkService) {
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager)
            }
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_,_,_)>>true
            }
                    controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            listWorkflows(_,_) >> [schedlist:[]]
            finishquery(_,_,_) >> [:]
        }
        def mockJobListLinkHandler = Mock(JobListLinkHandler) {
            getName() >> projectJobListProperty
            generateRedirectMap(_) >> [controller:"menu",action:projectJobListProperty]
        }
        controller.jobListLinkHandlerRegistry = Mock(JobListLinkHandlerRegistry) {
            getJobListLinkHandlerForProject(_) >> mockJobListLinkHandler
        }
        controller.userService=Mock(UserService)
        controller.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()

        if(explicitJobListType) params.jobListType = explicitJobListType
        params.project = "prj"

        when:
        controller.jobs(new ScheduledExecutionQuery())

        then:
        (response.status == 302) == shouldRedirect

        where:
        projectJobListProperty | shouldRedirect | explicitJobListType
        "grouped"              | false          | null
        "test"                 | true           | null
        "test"                 | false          | "grouped"
    }

    @Unroll
    def "test api executions running list all projects with a valid project"() {
        given:
        controller.apiService = Mock(ApiService){
            1 * requireApi(_,_,14) >> true
        }

        controller.userService = Mock(UserService){}

        UserAndRolesAuthContext test = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }

        def projectMock = Mock(IRundeckProject) {
            getProperties() >> [:]
            getName() >> 'aProject'
        }

        def executionService = Mock(ExecutionService){
            finishQueueQuery(_,_,_) >> [:]
        }

        controller.executionService = executionService

        controller.frameworkService = Mock(FrameworkService){
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
            getServerUUID() >> 'uuid'

        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){

                authorizeProjectResource(_, AuthorizationUtil.resourceType('event'),'read', 'aProject') >> true

                1 * getAuthContextForSubject(_) >> test
            }

        params.project = '*'
        request.api_version = 35

        when:
        def result = controller.apiExecutionsRunningv14()

        then:
        0 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, '*')
        1 * controller.frameworkService.projectNames(_) >> ['aProject']
        0 * controller.apiService.renderErrorFormat(_,_)
        1 * controller.executionService.queryQueue(_) >> {
            assert it[0].projFilter == 'aProject'

        }
    }
    @Unroll
    def "test nowrunningAjax list all projects with a valid project"() {
        given:

        controller.userService = Mock(UserService){}

        UserAndRolesAuthContext test = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }

        def projectMock = Mock(IRundeckProject) {
            getProperties() >> [:]
            getName() >> 'aProject'
        }

        def executionService = Mock(ExecutionService){
            finishQueueQuery(_,_,_) >> [:]
        }

        controller.executionService = executionService

        controller.frameworkService = Mock(FrameworkService){
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
            getServerUUID() >> 'uuid'

        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectResource(_, AuthorizationUtil.resourceType('event'),'read', 'aProject') >> true

                1 * getAuthContextForSubject(_) >> test
            }
        params[qparam] = '*'
        request.api_version = 35
        request.addHeader('x-rundeck-ajax', 'true')

        when:
        def result = controller.nowrunningAjax()

        then:
        0 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, '*')
        1 * controller.frameworkService.projectNames(_) >> ['aProject']
        0 * controller.apiService.renderErrorFormat(_,_)
        1 * controller.executionService.queryQueue(_) >> {
            assert it[0].projFilter == 'aProject'
        }
        where:
            qparam << ['project','projFilter']
    }
    @Unroll
    def "test nowrunningAjax with single project"() {
        given:

            controller.userService = Mock(UserService) {}

            UserAndRolesAuthContext test = Mock(UserAndRolesAuthContext) {
                getUsername() >> 'test'
                getRoles() >> new HashSet<String>(['test'])
            }

            def projectMock = Mock(IRundeckProject) {
                getProperties() >> [:]
                getName() >> 'aProject'
            }

            def executionService = Mock(ExecutionService) {
                finishQueueQuery(_, _, _) >> [:]
            }

            controller.executionService = executionService

            controller.frameworkService = Mock(FrameworkService) {
                _ * getRundeckBase() >> ''
                _ * getFrameworkProject(_) >> projectMock
                _ * getServerUUID() >> 'uuid'
                1 * existsFrameworkProject('aProject') >> true
                0 * projectNames(_) >> ['aProject']
            }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeProjectResource(_, AuthorizationUtil.resourceType('event'), 'read', 'aProject') >>
                true

                1 * getAuthContextForSubjectAndProject(_, 'aProject')
            }
            controller.apiService = Mock(ApiService)

            params[qparam] = 'aProject'
            request.api_version = 35
            request.addHeader('x-rundeck-ajax', 'true')

        when:
            def result = controller.nowrunningAjax()

        then:
            0 * controller.apiService.renderErrorFormat(_, _)
            1 * controller.apiService.requireExists(_, true, ['project', 'aProject']) >> true
            1 * controller.executionService.queryQueue({ it.projFilter == 'aProject' })
        where:
            qparam << ['project', 'projFilter']
    }

    def "test list all projects with an invalid project"() {
        given:
        controller.apiService = Mock(ApiService){
            1 * requireApi(_,_,14) >> true
        }

        controller.userService = Mock(UserService){}

        UserAndRolesAuthContext test = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }

        def projectMock = Mock(IRundeckProject) {
            getProperties() >> [:]
            getName() >> 'aProject'
        }

        def executionService = Mock(ExecutionService){
            finishQueueQuery(_,_,_) >> [:]
        }

        controller.executionService = executionService

        controller.frameworkService = Mock(FrameworkService){
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
            getServerUUID() >> 'uuid'

        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectResource(_, AuthorizationUtil.resourceType('event'),'read', 'aProject') >> false

                1 * getAuthContextForSubject(_) >> test
            }

        params.project = '*'
        request.api_version = 35
        def action = 'read'

        when:
        def result = controller.apiExecutionsRunningv14()

        then:
        0 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, '*')
        1 * controller.frameworkService.projectNames(_) >> ['aProject']
        1 * controller.apiService.renderErrorFormat(_,{map->
            map.status==401 && map.code=='api.error.execution.project.notfound'
        })
        0 * controller.executionService.queryQueue(_) >> {
            assert it[0].projFilter == 'aProject'

        }

    }

    def "test list all projects with an invalid and a valid project"() {
        given:
        controller.apiService = Mock(ApiService){
            1 * requireApi(_,_,14) >> true
        }

        controller.userService = Mock(UserService){}

        UserAndRolesAuthContext test = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }

        def projectMock = Mock(IRundeckProject) {
            getProperties() >> [:]
            getName() >> ['aProject', 'bProject']
        }

        def executionService = Mock(ExecutionService){
            finishQueueQuery(_,_,_) >> [:]
        }

        controller.executionService = executionService

        controller.frameworkService = Mock(FrameworkService){
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
            getServerUUID() >> 'uuid'

        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectResource(_, AuthorizationUtil.resourceType('event'),'read', 'aProject') >> true

                1 * getAuthContextForSubject(_) >> test
            }

        params.project = '*'
        request.api_version = 35
        def action = 'read'

        when:
        def result = controller.apiExecutionsRunningv14()

        then:
        0 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, '*')
        1 * controller.frameworkService.projectNames(_) >> ['aProject', 'bProject']
        0 * controller.apiService.renderErrorFormat(_,{map->
            map.status==401 && map.code=='api.error.execution.project.notfound'
        })
        1 * controller.executionService.queryQueue(_) >> {
            assert it[0].projFilter == 'aProject'

        }

    }

    def "test list all projects with multiple valid projects"() {
        given:
        controller.apiService = Mock(ApiService){
            1 * requireApi(_,_,14) >> true
        }

        controller.userService = Mock(UserService){}

        UserAndRolesAuthContext test = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }

        def projectMock = Mock(IRundeckProject) {
            getProperties() >> [:]
            getName() >>  ['aProject', 'bProject', 'cProject']
        }

        def executionService = Mock(ExecutionService){
            finishQueueQuery(_,_,_) >> [:]
        }

        controller.executionService = executionService

        controller.frameworkService = Mock(FrameworkService){
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
            getServerUUID() >> 'uuid'


        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectResource(_, AuthorizationUtil.resourceType('event'),'read', _) >> {
                    it[3] == 'aProject' ||  it[3] == 'bProject'||  it[3] == 'cProject'
                }

                1 * getAuthContextForSubject(_) >> test
            }

        params.project = '*'
        request.api_version = 35
        def action = 'read'

        when:
        def result = controller.apiExecutionsRunningv14()

        then:
        0 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, '*')
        1 * controller.frameworkService.projectNames(_) >> ['aProject', 'bProject', 'cProject' ]
        0 * controller.apiService.renderErrorFormat(_,_)
        1 * controller.executionService.queryQueue(_) >> {
            assert it[0].projFilter == 'aProject,bProject,cProject'

        }
    }

    @Unroll
    def "RdAuthorizeSystem required for endpoint #endpoint authorize #access"() {
        when:
            def result = getControllerMethodAnnotation(endpoint, RdAuthorizeSystem)
        then:
            result.value() == access
        where:
            endpoint                                | access
            'logStorageIncompleteAjax'              | RundeckAccess.System.AUTH_READ_OR_OPS_ADMIN
            'logStorageMissingAjax'                 | RundeckAccess.System.AUTH_READ_OR_OPS_ADMIN
            'logStorageAjax'                        | RundeckAccess.System.AUTH_READ_OR_OPS_ADMIN
            'apiLogstorageInfo'                     | RundeckAccess.System.AUTH_READ_OR_OPS_ADMIN
            'apiLogstorageListIncompleteExecutions' | RundeckAccess.System.AUTH_READ_OR_OPS_ADMIN
            'systemConfig'                          | RundeckAccess.System.AUTH_READ_OR_ANY_ADMIN
            'systemInfo'                            | RundeckAccess.System.AUTH_READ_OR_OPS_ADMIN
    }

    @Unroll
    @Ignore("TODO: could enforce authorize annotation of every controller Action")
    def "all methods #method authorize check"() {
        expect:
            NamedAuthRequestUtil.requestsFromAnnotations(method).size() >0
        where:
            method << MenuController.declaredMethods.findAll { it.getAnnotation(Action) != null }
    }

    def "jobs group"() {
        given:
        controller.jobListLinkHandlerRegistry = Mock(JobListLinkHandlerRegistry) {
            getJobListLinkHandlerForProject(_) >> new GroupedJobListLinkHandler()
        }
        def testUUID = UUID.randomUUID().toString()
        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager)
            }
        }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        controller.aclFileManagerService = Mock(AclFileManagerService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.scmService = Mock(ScmService)
        controller.userService = Mock(UserService)
        controller.jobSchedulesService = Mock(JobSchedulesService)
        controller.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
        controller.configurationService = Mock(ConfigurationService){
            getBoolean("gui.realJobTree",true)>>jobTree
        }

        def query = new ScheduledExecutionQuery()
        params.project='test'
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName: 'job1', groupPath: 'demo/level1', uuid:testUUID))
        ScheduledExecution job2 = new ScheduledExecution(createJobParams(jobName: 'job2', groupPath: 'demo/level2', uuid:testUUID))
        ScheduledExecution job3 = new ScheduledExecution(createJobParams(jobName: 'job3', groupPath: 'acme', uuid:testUUID))

        when:
        def model = controller.jobs(query)
        then:
        1 * controller.rundeckAuthContextProcessor.authResourceForJob(_) >>
                [authorized: true, action: AuthConstants.ACTION_READ, resource: job1]
        1 * controller.rundeckAuthContextProcessor.authResourceForJob(_) >>
                [authorized: true, action: AuthConstants.ACTION_READ, resource: job2]
        1 * controller.rundeckAuthContextProcessor.authResourceForJob(_) >>
                [authorized: true, action: AuthConstants.ACTION_READ, resource: job3]
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResource(_, _, _, _) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectResources(_, _, _, _) >> [[authorized: true,
                                                                                              action    : AuthConstants.ACTION_READ,
                                                                                              resource  : [group: job1.groupPath, name: job1.jobName]]]
        1 * controller.scheduledExecutionService.listWorkflows(_,_) >> [schedlist: [job1, job2,job3]]
        1 * controller.scheduledExecutionService.finishquery(_, _, _) >> [max           : 20,
                                                                          offset        : 0,
                                                                          paginateParams: [:],
                                                                          displayParams : [:]]
        model.jobListIds.size() == 3
        model.jobgroups.size() == groupsSize
        model.jobgroups.keySet().sort() == group
        where:
        jobTree | groupsSize    | group
        true    | 4             | ["acme","demo","demo/level1","demo/level2"]
        false   | 3             | ["acme","demo/level1","demo/level2"]
    }

    private <T extends Annotation> T getControllerMethodAnnotation(String name, Class<T> clazz) {
        artefactInstance.getClass().getDeclaredMethods().find { it.name == name }.getAnnotation(clazz)
    }
}
