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

import com.dtolabs.rundeck.app.support.ExtraCommand
import com.dtolabs.rundeck.app.support.RunJobCommand
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.NodesSelector
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.core.utils.OptsUtil
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.commons.fileupload.FileItem
import org.grails.plugins.codecs.URLCodec
import org.grails.plugins.testing.GrailsMockMultipartFile
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.ImportedJob
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.web.RdAuthorizeJob
import org.springframework.web.multipart.commons.CommonsMultipartFile
import rundeck.*
import rundeck.codecs.URIComponentCodec
import rundeck.services.*
import rundeck.services.feature.FeatureService
import rundeck.services.optionvalues.OptionValuesService
import spock.lang.Unroll
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import testhelper.RundeckHibernateSpec

import javax.security.auth.Subject
import javax.servlet.http.HttpServletResponse
import java.lang.annotation.Annotation

/**
 * Created by greg on 7/14/15.
 */
class ScheduledExecutionControllerSpec extends RundeckHibernateSpec implements ControllerUnitTest<ScheduledExecutionController>{

    List<Class> getDomainClasses() { [ScheduledExecution, Option, Workflow, CommandExec, Execution, JobExec, ReferencedExecution, ScheduledExecutionStats] }

    def setup() {
        mockCodec(URIComponentCodec)
        mockCodec(URLCodec)

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'

        defineBeans {
            configurationService(ConfigurationService) {
                grailsApplication = grailsApplication
            }
        }
    }

    private <T extends Annotation> T getControllerMethodAnnotation(String name, Class<T> clazz) {
        artefactInstance.getClass().getDeclaredMethods().find { it.name == name }.getAnnotation(clazz)
    }
    public static final String TEST_UUID1 = UUID.randomUUID().toString()

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

    def "RdAuthorizeJob required for #endpoint access #access"() {
        given:
            def annotation = getControllerMethodAnnotation(endpoint, RdAuthorizeJob)
        expect:
            annotation.value() == access
        where:
            endpoint             | access
            'actionMenuFragment' | RundeckAccess.Job.AUTH_APP_READ_OR_VIEW
    }

    def "workflow json"() {
        given:
        ScheduledExecution job = new ScheduledExecution(createJobParams())
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        controller.response.format = "json"

        when:
        request.api_version = 34
        request.method = 'GET'
        params.id = job.extid
        def result = controller.apiJobWorkflow()

        then:
        1 * controller.apiService.requireApi(_,_)>>true
        1 * controller.apiService.requireApi(_,_,34) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_, job, ['read', 'view'], 'AProject') >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_, job, ['read'], 'AProject') >> readauth
        1 * controller.scheduledExecutionService.getByIDorUUID(_) >> job
        1 * controller.scheduledExecutionService.getWorkflowDescriptionTree('AProject', _, readauth, 3) >>
        [test: 'data']
        response.json == [workflow: [test: 'data']]

        where:
        readauth | _
        true     | _
        false    | _

    }
    def "workflow json no read auth"() {
        given:
        ScheduledExecution job = new ScheduledExecution(createJobParams())
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

        when:
        request.api_version = 34
        request.method = 'GET'
        params.id = job.extid
        def result = controller.apiJobWorkflow()

        then:
        1 * controller.apiService.requireApi(_,_)>>true
        1 * controller.apiService.requireApi(_,_,34) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_, job, ['read', 'view'], 'AProject') >> readorview
        1 * controller.scheduledExecutionService.getByIDorUUID(_) >> job
        0 * controller.scheduledExecutionService.getWorkflowDescriptionTree('AProject', _, _, 3)
        1 * controller.apiService.renderErrorFormat(_,[status: HttpServletResponse.SC_FORBIDDEN,
                                                       code  : 'api.error.item.unauthorized', args: ['View', 'Job ' + 'ID', job.extid]])

        where:
            readorview | _
            false      | _
    }

    def "flip execution enabled"() {
        given:
        def job1 = new ScheduledExecution(createJobParams())
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        params.id = 'dummy'
        params.executionEnabled = isEnabled
        request.subject=new Subject()
        setupFormTokens(params)

        request.method = "POST"
        when:
        def result = controller.flipExecutionEnabled()

        then:
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(*_) >> auth
        1 * controller.frameworkService.getRundeckFramework()
        0 * controller.frameworkService._(*_)
        1 * controller.scheduledExecutionService.getByIDorUUID('dummy') >> job1
        1 * controller.scheduledExecutionService._doUpdateExecutionFlags(
                [id: 'dummy', executionEnabled: isEnabled],
                _,
                _,
                _,
                _,
                _
        )>>[success:true]
        0 * controller.scheduledExecutionService._(*_)

        response.status == 302
        response.redirectedUrl=='/project'

        where:
        isEnabled | _
        true      | _
        false     | _
    }

    def "flip execution disable bulk"() {
        given:
        def job1 = new ScheduledExecution(createJobParams())
        job1.save()
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        params.idList = job1.id
        params.ids = [job1.id]
        params.project = 'project'
        params.executionEnabled = false
        request.subject=new Subject()
        setupFormTokens(params)

        request.method = "POST"
        when:
        def result = controller.flipExecutionDisabledBulk()

        then:
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_,_) >> auth
        0 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_) >> auth
        1 * controller.frameworkService.getRundeckFramework()
        0 * controller.frameworkService._(*_)
        1 * controller.scheduledExecutionService.getByIDorUUID(job1.id.toString()) >> job1
        1 * controller.scheduledExecutionService._doUpdateExecutionFlags(
                [id: job1.id.toString(), executionEnabled: false],
                _,
                _,
                _,
                _,
                _
        )>>[success:true, scheduledExecution: job1]

        response.status == 302
        response.redirectedUrl=='/project/project/jobs'
    }

    def "flip execution enable bulk"() {
        given:
        def job1 = new ScheduledExecution(createJobParams())
        job1.save()
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        params.idList = job1.id
        params.ids = [job1.id]
        params.project = 'project'
        params.executionEnabled = true
        request.subject=new Subject()
        setupFormTokens(params)

        request.method = "POST"
        when:
        def result = controller.flipExecutionEnabledBulk()

        then:
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_,_) >> auth
        0 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_) >> auth
        1 * controller.frameworkService.getRundeckFramework()
        1 * controller.scheduledExecutionService.getByIDorUUID(job1.id.toString()) >> job1
        0 * controller.frameworkService._(*_)
        1 * controller.scheduledExecutionService._doUpdateExecutionFlags(
                [id: job1.id.toString(), executionEnabled: true],
                _,
                _,
                _,
                _,
                _
        )>>[success:true, scheduledExecution: job1]

        response.status == 302
        response.redirectedUrl=='/project/project/jobs'
    }


    def "flip schedule enabled"() {
        given:
        def job1 = new ScheduledExecution(createJobParams())
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        params.id = 'dummy'
        params.scheduleEnabled = isEnabled
        request.subject=new Subject()
        setupFormTokens(params)

        request.method = "POST"
        when:
        def result = controller.flipScheduleEnabled()

        then:
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(*_) >> auth
        1 * controller.frameworkService.getRundeckFramework()
        0 * controller.frameworkService._(*_)
        1 * controller.scheduledExecutionService.getByIDorUUID('dummy') >> job1
        1 * controller.scheduledExecutionService._doUpdateExecutionFlags(
                [id: 'dummy', scheduleEnabled: isEnabled],
                _,
                _,
                _,
                _,
                _
        )>>[success:true]
        0 * controller.scheduledExecutionService._(*_)

        response.status == 302
        response.redirectedUrl=='/project'

        where:
        isEnabled | _
        true      | _
        false     | _
    }
    def "api run job option params"() {
        given:
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.apiService = Mock(ApiService)
        controller.executionService = Mock(ExecutionService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        when:
        request.api_version=18
        request.method='POST'
        params.id='ajobid'
        params.putAll(paramoptions)
        def result=controller.apiJobRun()

        then:


        1 * controller.apiService.requireApi(_,_)>>true
        1 * controller.scheduledExecutionService.getByIDorUUID('ajobid')>>[:]
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_,_)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_,_,['run'],_)>>true
        1 * controller.apiService.requireExists(_,_,_)>>true
        1 * controller.executionService.executeJob(
                _,
                _,
                _,
                { it['option.abc'] == 'tyz' && it['option.def'] == 'xyz'}
        ) >> [success: true]
        1 * controller.executionService.respondExecutionsXml(_,_,_)
        0 * controller.executionService._(*_)

        where:

        paramoptions                               | _
        [option: [abc: 'tyz', def: 'xyz']]         | _
        ['option.abc': 'tyz', 'option.def': 'xyz'] | _
    }

    def "api run job nodes selected by default"() {
        given:
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.apiService = Mock(ApiService)
        controller.executionService = Mock(ExecutionService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        when:
        request.api_version=18
        request.method='POST'
        params.id='ajobid'
        def result=controller.apiJobRun()

        then:


        1 * controller.apiService.requireApi(_,_)>>true
        1 * controller.scheduledExecutionService.getByIDorUUID('ajobid')>>[nodesSelectedByDefault:nodesSelectedByDefault]
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_,_)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_,_,['run'],_)>>true
        1 * controller.apiService.requireExists(_,_,_)>>true
        1 * controller.executionService.executeJob(
                _,
                _,
                _,
                { it['_replaceNodeFilters'] == replaceNodeFilters}
        ) >> [success: true]
        1 * controller.executionService.respondExecutionsXml(_,_,_)
        0 * controller.executionService._(*_)

        where:

        nodesSelectedByDefault | replaceNodeFilters
        true                   | null
        false                  | 'true'
    }
    def "api run job option params json"() {
        given:
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.apiService = Mock(ApiService)
        controller.executionService = Mock(ExecutionService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        when:
        request.api_version = 18
        request.method = 'POST'
        params.id = 'ajobid'
        request.json = [
                options: [abc: 'tyz', def: 'xyz']
        ]
        def result = controller.apiJobRun()

        then:


        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID('ajobid') >> [:]
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, _)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_, _, ['run'], _) >> true
        1 * controller.apiService.requireExists(_, _, _) >> true
        1 * controller.executionService.executeJob(
                _,
                _,
                _,
                { it['option.abc'] == 'tyz' && it['option.def'] == 'xyz' }
        ) >> [success: true]
        1 * controller.executionService.respondExecutionsXml(_, _, _)
        0 * controller.executionService._(*_)
    }
    def "api run job now"() {
        given:
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.apiService = Mock(ApiService)
        controller.executionService = Mock(ExecutionService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        when:
        request.api_version = 18
        request.method = 'POST'
        params.id = 'ajobid'
        def result = controller.apiJobRun()

        then:


        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID('ajobid') >> [:]
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, _)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_, _, ['run'], _) >> true
        1 * controller.apiService.requireExists(_, _, _) >> true
        1 * controller.executionService.executeJob(
                _,
                _,
                _,
                [executionType: 'user']
        ) >> [success: true]
        1 * controller.executionService.respondExecutionsXml(_, _, _)
        0 * controller.executionService._(*_)
    }
    def "api run job at time"() {
        given:
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.apiService = Mock(ApiService)
        controller.executionService = Mock(ExecutionService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        when:
        request.api_version = 18
        request.method = 'POST'
        params.id = 'ajobid'
        params.runAtTime = 'timetorun'
        def result = controller.apiJobRun()

        then:


        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID('ajobid') >> [:]
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, _)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_, _, ['run'], _) >> true
        1 * controller.apiService.requireExists(_, _, _) >> true
        1 * controller.executionService.scheduleAdHocJob(
                _,
                _,
                _,
                [runAtTime: 'timetorun']
        ) >> [success: true]
        1 * controller.executionService.respondExecutionsXml(_, _, _)
        0 * controller.executionService._(*_)
    }
    def "api run job at time json"(){
        given:
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.apiService = Mock(ApiService)
        controller.executionService = Mock(ExecutionService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        when:
        request.api_version=18
        request.method='POST'
        request.format='json'
        request.json=[
                runAtTime:'timetorun'
        ]
        params.id='ajobid'
        def result=controller.apiJobRun()

        then:


        1 * controller.apiService.requireApi(_,_)>>true
        1 * controller.scheduledExecutionService.getByIDorUUID('ajobid')>>[:]
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_,_)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_,_,['run'],_)>>true
        1 * controller.apiService.requireExists(_,_,_)>>true
        1 * controller.executionService.scheduleAdHocJob(
                _,
                _,
                _,
                [runAtTime: 'timetorun']
        ) >> [success: true]
        1 * controller.executionService.respondExecutionsXml(_,_,_)
        0 * controller.executionService._(*_)
    }
    def "api scheduler takeover cluster mode disabled"(){
        given:
        def serverUUID1 = TEST_UUID1
            def msgResult=null
        def msgClos={str-> msgResult=str }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1* renderSuccessXml(_,_,_)>> {
                def clos=it[2]
                clos.delegate=[message:msgClos,result:{map,clos2->clos2.delegate=[message:msgClos];clos2.call()}]
                clos.call()
                null
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * isClusterModeEnabled()>>false
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_JOB,[AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN])>>true

                1 * getAuthContextForSubject(_)>>null
            }
        when:
        request.method='PUT'
        request.XML="<server uuid='${serverUUID1}' />"
        response.format='xml'
        def result=controller.apiJobClusterTakeoverSchedule()

        then:
        response.status==200
        msgResult=='No action performed, cluster mode is not enabled.'
    }

    @Unroll
    def "api scheduler takeover XML input"(String requestXml, String requestUUID, boolean allserver, String project, String[] jobid, int api_version){
        given:
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * renderSuccessXml(_,_,_) >> 'result'
            0 * renderErrorFormat(_,_,_) >> null
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * isClusterModeEnabled()>>true
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_JOB,[AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN])>>true

                1 * getAuthContextForSubject(_)>>null
            }
        controller.scheduledExecutionService=Mock(ScheduledExecutionService){
            1 * reclaimAndScheduleJobs(requestUUID,allserver,project,jobid)>>[:]
            0 * _(*_)
        }
        when:
        request.method='PUT'
        request.XML=requestXml
        request.api_version = api_version
        response.format='xml'
        def result=controller.apiJobClusterTakeoverSchedule()

        then:
        response.status==200

        where:
        requestXml                                                                                              | requestUUID | allserver | project | jobid             | api_version
        "<server uuid='${TEST_UUID1}' />".toString()                                                            | TEST_UUID1  | false     | null    | null              | 17
        "<server all='true' />".toString()                                                                      | null        | true      | null    | null              | 17
        "<takeoverSchedule><server uuid='${TEST_UUID1}' /></takeoverSchedule>".toString()                       | TEST_UUID1  | false     | null    | null              | 17
        "<takeoverSchedule><server all='true' /></takeoverSchedule>".toString()                                 | null        | true      | null    | null              | 17
        '<takeoverSchedule><server all="true" /><project name="asdf"/></takeoverSchedule>'                      | null        | true      | 'asdf'  | null              | 17
        '<takeoverSchedule><server all="true" /><job id="ajobid"/></takeoverSchedule>'                          | null        | true      | null    | ['ajobid']          | 17
        "<takeoverSchedule><server uuid='${TEST_UUID1}' /><project name='asdf'/></takeoverSchedule>".toString() | TEST_UUID1  | false     | 'asdf'  | null              | 17
        '<takeoverSchedule><server all="true" /><job id="ajobid"/><job id="ajobidb"/></takeoverSchedule>'       | null        | true      | null    | ['ajobid','ajobidb']  | 32

    }


    protected void setupFormTokens(params) {
        def token = SynchronizerTokensHolder.store(session)
        params[SynchronizerTokensHolder.TOKEN_KEY] = token.generateToken('/test')
        params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
    }

    def "show job retry failed exec id filter nodes"(){
        given:
        ScheduledExecution.metaClass.static.withNewSession = {Closure c -> c.call() }

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: se
        ).save()



        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))
        testNodeSet.putNode(new NodeEntryImpl("nodec xyz"))
        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))
        testNodeSetB.putNode(new NodeEntryImpl("nodec xyz"))

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                _*authorizeProjectJobAny(_,_,_,_)>>true
                _*filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
            }

        controller.frameworkService=Mock(FrameworkService){
            filterNodeSet({ NodesSelector selector->
                selector.acceptNode(new NodeEntryImpl("nodea")) &&
                selector.acceptNode(new NodeEntryImpl("nodec xyz")) &&
                !selector.acceptNode(new NodeEntryImpl("nodeb"))

                          },_)>>testNodeSetB
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }
        controller.scheduledExecutionService=Mock(ScheduledExecutionService){
            getByIDorUUID(_)>>se
        }
        controller.notificationService=Mock(NotificationService)
        controller.orchestratorPluginService=Mock(OrchestratorPluginService)
        controller.pluginService = Mock(PluginService)
        controller.featureService = Mock(FeatureService)


        when:
        request.parameters = [id: se.id.toString(),project:'project1',retryFailedExecId:exec.id.toString()]

        def model = controller.show()
        then:
        response.redirectedUrl==null
        model != null
        model.scheduledExecution != null
        'fwnode' == model.localNodeName
        'name: ${option.nodes}' == model.nodefilter
        false == model.nodesetvariables
        'nodec xyz,nodea' == model.failedNodes
        null == model.nodesetempty
        testNodeSetB.nodes == model.nodes
        null == model.selectedNodes
        [:] == model.grouptags
        null == model.selectedoptsmap
        true == model.nodesSelectedByDefault
        [:] == model.dependentoptions
        [:] == model.optiondependencies
        null == model.optionordering
        [:] == model.remoteOptionData

    }

    @Unroll
    def "job download #format"() {
        given:

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter: 'name: ${option.nodes}',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString        : '-delay 12 -monkey cheese -particle'
                                ]
                                )
                        ]
                )
        ).save()


        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))



        controller.frameworkService = Mock(FrameworkService) {
            filterNodeSet(_, _) >> testNodeSetB
            getRundeckFramework() >> Mock(Framework) {
                getFrameworkNodeName() >> 'fwnode'
            }
        }

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                _*authorizeProjectJobAny(_,_,['read'],_)>>true
                _*filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
            }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            getByIDorUUID(_) >> se
        }
        controller.notificationService = Mock(NotificationService)
        controller.orchestratorPluginService = Mock(OrchestratorPluginService)
        controller.pluginService = Mock(PluginService)
            controller.featureService = Mock(FeatureService)
        controller.rundeckJobDefinitionManager=Mock(RundeckJobDefinitionManager){
            1 * exportAs(format,[se],_)>>{
                it[2]<<"format: $format"
            }
        }


        when:
        request.parameters = [id: se.id.toString(), project: 'project1']
        response.format = format
        def model = controller.show()
        then:
        response.status == 200
        response.header('Content-Disposition') == "attachment; filename=\"test1.$format\""
        response.text=="format: $format"
        where:
        format << ['xml', 'yaml']

    }

    def "run job now"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.save()

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        def testcontext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        controller.frameworkService = Mock(FrameworkService) {


        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true

                getAuthContextForSubjectAndProject(*_) >> testcontext
            }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getByIDorUUID(_) >> se
            isProjectExecutionEnabled(_) >> true
        }


        controller.executionService = Mock(ExecutionService) {
            1 * getExecutionsAreActive() >> true
            1 * executeJob(se, testcontext, _,  {opts->
                opts['runAtTime']==null && opts['executionType']=='user'
            }) >> [executionId: exec.id]
        }
        controller.fileUploadService = Mock(FileUploadService)

        def command = new RunJobCommand()
        command.id = se.id.toString()
        def extra = new ExtraCommand()


        request.subject = new Subject()
        setupFormTokens(params)
        when:
        request.method = 'POST'
        controller.runJobNow(command, extra)

        then:
        response.status == 302
        response.redirectedUrl == '/scheduledExecution/show'
    }
    def "run job now inline"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )

        se.save()

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        ).save()

        def testcontext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        controller.frameworkService = Mock(FrameworkService) {

        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true

                getAuthContextForSubjectAndProject(*_) >> testcontext
            }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getByIDorUUID(_) >> se
            isProjectExecutionEnabled(_) >> true
        }


        controller.executionService = Mock(ExecutionService) {
            1 * getExecutionsAreActive() >> true
            1 * executeJob(se, testcontext, _,  {opts->
                    opts['runAtTime']==null && opts['executionType']=='user' && opts['option.emptyOpt'] == ''
            }) >> [executionId: exec.id]
        }
        controller.fileUploadService = Mock(FileUploadService)

        def command = new RunJobCommand()
        command.id = se.id.toString()
        def extra = new ExtraCommand()


        request.subject = new Subject()
        setupFormTokens(params)
        params.extra = ['option.emptyOpt': '']
        when:
        request.method = 'POST'
        controller.runJobInline(command, extra)

        then:
        response.status == 200
        response.contentType.contains 'application/json'
        response.json == [
                href   : "/execution/show/${exec.id}",
                success: true,
                id     : exec.id,
                follow : false
        ]

    }

    def "run job now inline and option with empty value"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.save()

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        ).save()

        def testcontext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
            getAuthContextForSubjectAndProject(*_) >> testcontext
            authorizeProjectJobAll(*_) >> true
        }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getByIDorUUID(_) >> se
            isProjectExecutionEnabled(_) >> true
        }


        controller.executionService = Mock(ExecutionService) {
            1 * getExecutionsAreActive() >> true
            1 * executeJob(se, testcontext, _,  {opts->
                    opts['runAtTime']==null && opts['executionType']=='user' && opts['option.emptyOpt'] == ''
            }) >> [executionId: exec.id]
        }
        controller.fileUploadService = Mock(FileUploadService)

        def command = new RunJobCommand()
        command.id = se.id.toString()
        def extra = new ExtraCommand()


        request.subject = new Subject()
        setupFormTokens(params)
        params.extra = ['option.emptyOpt': '']

        when:
        request.method = 'POST'
        controller.runJobInline(command, extra)

        then:
        response.status == 200
        response.contentType.contains 'application/json'
        response.json == [
                href   : "/execution/show/${exec.id}",
                success: true,
                id     : exec.id,
                follow : false
        ]

    }

    def "run job now inline with option file and the file is empty"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.addToOptions([name: "OPT1", optionType: "file"])
        se.save()

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        ).save()

        def testcontext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        controller.frameworkService = Mock(FrameworkService) {

        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true

                getAuthContextForSubjectAndProject(*_) >> testcontext
            }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getByIDorUUID(_) >> se
            isProjectExecutionEnabled(_) >> true
        }


        controller.executionService = Mock(ExecutionService) {
            1 * getExecutionsAreActive() >> true
            1 * executeJob(se, testcontext, _,  {opts->
                opts['runAtTime']==null && opts['executionType']=='user'
            }) >> [executionId: exec.id]
        }
        controller.fileUploadService = Mock(FileUploadService){
            //assert the empty file will be uploaded
            1 * receiveFile(_,_,_,'afile.txt', 'OPT1',_,_,_,_) >> 'aref'
        }

        def command = new RunJobCommand()
        command.id = se.id.toString()
        def extra = new ExtraCommand()


        request.subject = new Subject()
        request.addFile(new GrailsMockMultipartFile('extra.option.OPT1', 'afile.txt', 'application/octet-stream', ''.bytes))
        setupFormTokens(params)
        when:
        request.method = 'POST'
        controller.runJobInline(command, extra)

        then:
        response.status == 200
        response.contentType.contains 'application/json'
        response.json == [
                href   : "/execution/show/${exec.id}",
                success: true,
                id     : exec.id,
                follow : false
        ]

    }


    def "run job now inline to selected tab"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.save()

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        ).save()

        def testcontext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        controller.frameworkService = Mock(FrameworkService) {
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true

                getAuthContextForSubjectAndProject(*_) >> testcontext
            }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getByIDorUUID(_) >> se
            isProjectExecutionEnabled(_) >> true
        }


        controller.executionService = Mock(ExecutionService) {
            1 * getExecutionsAreActive() >> true
            1 * executeJob(se, testcontext, _,  {opts->
                opts['runAtTime']==null && opts['executionType']=='user'
            }) >> [executionId: exec.id]
        }
        controller.fileUploadService = Mock(FileUploadService)

        def command = new RunJobCommand()
        command.id = se.id.toString()
        def extra = new ExtraCommand()


        request.subject = new Subject()
        setupFormTokens(params)
        when:
        request.method = 'POST'
        params.project='testproject'
        params.followdetail = follow
        controller.runJobInline(command, extra)

        then:
        response.status == 200
        response.contentType.contains 'application/json'
        if(follow != 'html'){
            response.json == [
                    href   : "/execution/show/${exec.id}#"+follow,
                    success: true,
                    id     : exec.id,
                    follow : false
            ]
        }else{
            response.json == [
                    href : "/project/${params.project}/execution/renderOutput/${exec.id}?convertContent=on&loglevels=on&ansicolor=on&reload=true",
                    success: true,
                    id     : exec.id,
                    follow : false
            ]
        }

        where:
        follow      |_
        'output'    |_
        'summary'   |_
        'monitor'   |_
        'html'      |_

    }

    def "schedule job inline"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.save()

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        exec.save()
        def testcontext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        controller.frameworkService = Mock(FrameworkService) {
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true

                getAuthContextForSubjectAndProject(*_) >> testcontext
            }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getByIDorUUID(_) >> se
            isProjectExecutionEnabled(_) >> true
        }


        controller.executionService = Mock(ExecutionService) {
            1 * getExecutionsAreActive() >> true
            0 * executeJob(*_)
            1 * scheduleAdHocJob(se, testcontext, _, { opts ->
                opts['runAtTime'] == 'dummy' /*&& opts['executionType'] == 'user-scheduled'*/
            }
            ) >> [executionId: exec.id, id: exec.id]
        }
        controller.fileUploadService = Mock(FileUploadService)

        def command = new RunJobCommand()
        command.id = se.id.toString()
        def extra = new ExtraCommand()


        request.subject = new Subject()
        setupFormTokens(params)

        params.runAtTime = 'dummy'
        when:
        request.method = 'POST'
        controller.scheduleJobInline(command, extra)

        then:
        response.status == 200
        response.contentType.contains 'application/json'
        response.json == [
                href   : "/execution/show/${exec.id}",
                success: true,
                id     : exec.id,
                follow : false
        ]
    }

    def "schedule job inline to selected tab"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.save()

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        exec.save()
        def testcontext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        controller.frameworkService = Mock(FrameworkService) {
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true

                getAuthContextForSubjectAndProject(*_) >> testcontext
            }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getByIDorUUID(_) >> se
            isProjectExecutionEnabled(_) >> true
        }


        controller.executionService = Mock(ExecutionService) {
            1 * getExecutionsAreActive() >> true
            0 * executeJob(*_)
            1 * scheduleAdHocJob(se, testcontext, _, { opts ->
                opts['runAtTime'] == 'dummy' /*&& opts['executionType'] == 'user-scheduled'*/
            }
            ) >> [executionId: exec.id, id: exec.id]
        }
        controller.fileUploadService = Mock(FileUploadService)

        def command = new RunJobCommand()
        command.id = se.id.toString()
        def extra = new ExtraCommand()


        request.subject = new Subject()
        setupFormTokens(params)

        params.runAtTime = 'dummy'
        when:
        request.method = 'POST'
        params.project='testproject'
        params.followdetail = follow
        controller.scheduleJobInline(command, extra)

        then:
        response.status == 200
        response.contentType.contains 'application/json'
        if(follow != 'html') {
            response.json == [
                    href   : "/execution/show/${exec.id}#" + follow,
                    success: true,
                    id     : exec.id,
                    follow : false
            ]
        } else {
            response.json == [
                    href   : "/project/${params.project}/execution/renderOutput/${exec.id}?convertContent=on&loglevels=on&ansicolor=on&reload=true",
                    success: true,
                    id     : exec.id,
                    follow : false
            ]
        }

        where:
        follow      |_
        'output'    |_
        'summary'   |_
        'monitor'   |_
        'html'      |_
    }
    def "run job later at time"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.save()

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        def testcontext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        controller.frameworkService = Mock(FrameworkService) {
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true

                getAuthContextForSubjectAndProject(*_) >> testcontext
            }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getByIDorUUID(_) >> se
            isProjectExecutionEnabled(_) >> true
        }


        controller.executionService = Mock(ExecutionService) {
            1 * getExecutionsAreActive() >> true
            0 * executeJob(*_)
            1 * scheduleAdHocJob(se, testcontext, _, {opts->
                opts['runAtTime']=='dummy' /*&& opts['executionType']=='user-scheduled'*/
            }) >> [executionId: exec.id]
        }
        controller.fileUploadService = Mock(FileUploadService)

        def command = new RunJobCommand()
        command.id = se.id.toString()
        def extra = new ExtraCommand()


        request.subject = new Subject()
        setupFormTokens(params)

        params.runAtTime = 'dummy'
        when:
        request.method = 'POST'
        controller.runJobLater(command, extra)

        then:
        response.status == 302
        response.redirectedUrl == '/scheduledExecution/show'
    }

    def "run job now missing form token"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.save()

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        def testcontext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        controller.frameworkService = Mock(FrameworkService) {
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true

                getAuthContextForSubjectAndProject(*_) >> testcontext
            }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            0 * getByIDorUUID(_) >> se
        }


        controller.executionService = Mock(ExecutionService) {
            0 * getExecutionsAreActive() >> true
            0 * executeJob(se, testcontext, _, _) >> [executionId: exec.id]
        }
        controller.fileUploadService = Mock(FileUploadService)

        def command = new RunJobCommand()
        command.id = se.id.toString()
        def extra = new ExtraCommand()


        request.subject = new Subject()
//        setupFormTokens(params)
        when:
        request.method = 'POST'
        controller.runJobNow(command, extra)

        then:
        response.status == 400
        response.redirectedUrl == null
        model.error == 'Invalid request token'
        request.errorCode == 'request.error.invalidtoken.message'
    }

    def "run job now passive mode"() {
        given:
        def executionModeActive = false
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.save()

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        def testcontext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        controller.frameworkService = Mock(FrameworkService) {
            getRundeckFramework() >> Mock(Framework) {
                getFrameworkNodeName() >> 'fwnode'
            }
            //0 * _(*_)
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(_,_,['run'],'testProject') >> true
                authorizeProjectJobAny(*_) >> true

                getAuthContextForSubjectAndProject(*_) >> testcontext
            }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            2 * getByIDorUUID(_) >> se
        }


        controller.executionService = Mock(ExecutionService) {
            1 * getExecutionsAreActive() >> executionModeActive
            0 * executeJob(se, testcontext, _, _) >> [executionId: exec.id]
            0 * _(*_)
        }
        controller.fileUploadService = Mock(FileUploadService)

        def command = new RunJobCommand()
        command.id = se.id.toString()
        def extra = new ExtraCommand()


        request.subject = new Subject()
        setupFormTokens(params)
        //called by show()
        controller.notificationService = Mock(NotificationService) {
            1 * listNotificationPlugins() >> [:]
        }
        controller.orchestratorPluginService = Mock(OrchestratorPluginService) {
            1 * getOrchestratorPlugins()
        }
        controller.pluginService = Mock(PluginService)
            controller.featureService = Mock(FeatureService)



        when:
        params.project = 'testProject'
        request.method = 'POST'
        def resp = controller.runJobNow(command, extra)

        then:
        response.status == 200
        response.redirectedUrl == null
        !model.success
        model.failed
        model.error == 'disabled.execution.run'
    }

    def "api job file upload single"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save(),
                options: [
                        new Option(name: 'f1', optionType: 'file', required: true, enforced: false,)
                ],
                )
        se.save()
        request.api_version = 19
        params.id = se.extid
        params.optionName = 'f1'
        controller.apiService = Mock(ApiService) {
            requireApi(_, _) >> true
            requireApi(_, _, 19) >> true
            requireParameters(_, _, ['id']) >> true
            requireParameters(_, _, ['optionName']) >> true
            requireExists(_, _, _) >> true
            0 * _(*_)
        }
        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            getByIDorUUID(se.extid) >> se
            0 * _(*_)
        }
        controller.frameworkService = Mock(FrameworkService) {
            0 * _(*_)
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true

                1 * getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext)
            }
        controller.fileUploadService = Mock(FileUploadService) {
            1 * receiveFile(_, 4l, null, null, 'f1', _, _, 'testProject', _) >> 'filerefid1'
            1 * getOptionUploadMaxSize() >> 0l
            0 * _(*_)
        }
        request.method = 'POST'
        request.content = 'data'.bytes
        request.addHeader('accept', 'application/json')

        when:
        controller.apiJobFileUpload()
        then:
        response.status == 200
        response.json == [total: 1, options: [f1: 'filerefid1']]

    }

    def "api job file multifile"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save(),
                options: [
                        new Option(name: 'f1', optionType: 'file', required: true, enforced: false,),
                        new Option(name: 'f2', optionType: 'file', required: true, enforced: false,),
                ],
                )
        se.save()
        request.api_version = 19
        params.id = se.extid
        params.optionName = 'f1'
        controller.apiService = Mock(ApiService) {
            requireApi(_, _) >> true
            requireApi(_, _, 19) >> true
            requireParameters(_, _, ['id']) >> true
            requireParameters(_, _, ['optionName']) >> true
            requireExists(_, _, _) >> true
            0 * _(*_)
        }
        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            getByIDorUUID(se.extid) >> se
            0 * _(*_)
        }
        controller.frameworkService = Mock(FrameworkService) {
            0 * _(*_)
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true

                1 * getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext)
            }
        controller.fileUploadService = Mock(FileUploadService) {
            1 * receiveFile(_, 5l, null, 'name1', 'f1', _, _, 'testProject', _) >> 'filerefid1'
            1 * receiveFile(_, 5l, null, 'name2', 'f2', _, _, 'testProject', _) >> 'filerefid2'
            1 * getOptionUploadMaxSize() >> 0l
            0 * _(*_)
        }
        request.method = 'POST'
        request.addHeader('accept', 'application/json')
        request.addFile(new GrailsMockMultipartFile('option.f1', 'name1', 'application/octet-stream', 'data1'.bytes))
        request.addFile(new GrailsMockMultipartFile('option.f2', 'name2', 'application/octet-stream', 'data2'.bytes))

        when:
        controller.apiJobFileUpload()
        then:
        response.status == 200
        response.json == [total: 2, options: [f1: 'filerefid1', f2: 'filerefid2']]

    }
    def "create from execution"() {
        given:

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        ).save()
        params.executionId = exec.id.toString()
        controller.frameworkService = Mock(FrameworkService) {
            getProjectGlobals(_) >> [:]
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectResource(_, _, _, _) >> true
                authorizeProjectExecutionAny(_, exec, _) >> true

                _ * getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext) {
                    getRoles() >> roles
                    getUsername() >> 'bob'
                }
            }
        when:
        def result = controller.createFromExecution()
        then:
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            1 * prepareCreateEditJob(params, _, _,_) >> {
                [scheduledExecution: it[1]]
            }
        }
        response.status == 200
        model.scheduledExecution != null
        model.scheduledExecution.userRoleList == userRoleList
        model.scheduledExecution.userRoles == roles

        where:
        roles                   | userRoleList
        ['a', 'b']              | "[\"a\",\"b\"]"
        ['a, with commas', 'b'] | "[\"a, with commas\",\"b\"]"
    }


    def "run job now project passive mode"() {
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.save()

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        def testcontext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }

        controller.frameworkService = Mock(FrameworkService) {
            getRundeckFramework() >> Mock(Framework) {
                getFrameworkNodeName() >> 'fwnode'
            }
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(_,_,['run'],'testProject') >> true
                authorizeProjectJobAny(*_) >> true

                getAuthContextForSubjectAndProject(*_) >> testcontext
            }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            2 * getByIDorUUID(_) >> se
            isProjectExecutionEnabled(_) >> false
        }


        controller.executionService = Mock(ExecutionService) {
            1 * getExecutionsAreActive() >> true
            0 * executeJob(se, testcontext, _, _) >> [executionId: exec.id]
        }
        controller.fileUploadService = Mock(FileUploadService)

        def command = new RunJobCommand()
        command.id = se.id.toString()
        def extra = new ExtraCommand()


        request.subject = new Subject()
        setupFormTokens(params)
        //called by show()
        controller.notificationService = Mock(NotificationService) {
            1 * listNotificationPlugins() >> [:]
        }
        controller.orchestratorPluginService = Mock(OrchestratorPluginService) {
            1 * getOrchestratorPlugins()
        }
        controller.pluginService = Mock(PluginService)
            controller.featureService = Mock(FeatureService)



        when:
        params.project = 'testProject'
        request.method = 'POST'
        def resp = controller.runJobNow(command, extra)

        then:
        response.status == 200
        response.redirectedUrl == null
        !model.success
        model.failed
        model.error == 'project.execution.disabled'
    }


    def "total and reftotal on show"(){
        given:
        ScheduledExecution.metaClass.static.withNewSession = {Closure c -> c.call() }

        def refTotal = 10
        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: se
        ).save()



        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))
        testNodeSet.putNode(new NodeEntryImpl("nodec xyz"))
        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))
        testNodeSetB.putNode(new NodeEntryImpl("nodec xyz"))


            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                _*authorizeProjectJobAll(_,_,['run'],'testProject') >> true
                _*authorizeProjectJobAny(_,_,_,_)>>true
                _*filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
            }

        controller.frameworkService=Mock(FrameworkService){
            filterNodeSet({ NodesSelector selector->
                selector.acceptNode(new NodeEntryImpl("nodea")) &&
                        selector.acceptNode(new NodeEntryImpl("nodec xyz")) &&
                        !selector.acceptNode(new NodeEntryImpl("nodeb"))

            },_)>>testNodeSetB
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }
        controller.scheduledExecutionService=Mock(ScheduledExecutionService){
            getByIDorUUID(_)>>se
        }
        controller.notificationService=Mock(NotificationService)
        controller.orchestratorPluginService=Mock(OrchestratorPluginService)
        controller.pluginService = Mock(PluginService)
            controller.featureService = Mock(FeatureService)


        when:
        request.parameters = [id: se.id.toString(),project:'project1',retryFailedExecId:exec.id.toString()]

        def model = controller.show()
        then:
        response.redirectedUrl==null
        model != null
        model.reftotal == refTotal

    }

    def "isReference on show searching by uuid"(){
        given:
        def refTotal = 10
        def se = new ScheduledExecution(
                uuid: jobuuid,
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def seb = new ScheduledExecution(
                jobName: 'test2',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: seb
        ).save()

        if(expected){
            def re = new ReferencedExecution(scheduledExecution: se,execution: exec).save()
        }



        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))
        testNodeSet.putNode(new NodeEntryImpl("nodec xyz"))
        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))
        testNodeSetB.putNode(new NodeEntryImpl("nodec xyz"))


            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                _*authorizeProjectJobAny(_,_,_,_)>>true
                _*filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
            }

        controller.frameworkService=Mock(FrameworkService){
            filterNodeSet(_,_)>>testNodeSetB
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }
        controller.scheduledExecutionService=Mock(ScheduledExecutionService){
            getByIDorUUID(_)>>se
        }
        controller.notificationService=Mock(NotificationService)
        controller.orchestratorPluginService=Mock(OrchestratorPluginService)
        controller.pluginService = Mock(PluginService)
            controller.featureService = Mock(FeatureService)


        when:
        request.parameters = [id: se.id.toString(),project:'project1',retryFailedExecId:exec.id.toString()]

        def model = controller.show()
        then:
        response.redirectedUrl==null
        model != null
        model.isReferenced==expected

        where:
        jobuuid     | expected
        '000000'    | false
        '111111'    | true

    }

    def "isReference on show searching by jobname/group"(){
        given:
        def refTotal = 10
        def se = new ScheduledExecution(
                uuid: 'uuid',
                jobName: jobname,
                project: 'project1',
                groupPath: '',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def seb = new ScheduledExecution(
                jobName: 'test2',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: seb
        ).save()

        if(expected){
            def re = new ReferencedExecution(scheduledExecution: se,execution: exec).save()
        }



        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))
        testNodeSet.putNode(new NodeEntryImpl("nodec xyz"))
        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))
        testNodeSetB.putNode(new NodeEntryImpl("nodec xyz"))

        controller.frameworkService=Mock(FrameworkService){
            filterNodeSet(_,_)>>testNodeSetB
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                _*authorizeProjectJobAny(_,_,_,_)>>true
                _*filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
            }

        controller.scheduledExecutionService=Mock(ScheduledExecutionService){
            getByIDorUUID(_)>>se
        }
        controller.notificationService=Mock(NotificationService)
        controller.orchestratorPluginService=Mock(OrchestratorPluginService)
        controller.pluginService = Mock(PluginService)
            controller.featureService = Mock(FeatureService)


        when:
        request.parameters = [id: se.id.toString(),project:'project1',retryFailedExecId:exec.id.toString()]

        def model = controller.show()
        then:
        response.redirectedUrl==null
        model != null
        model.isReferenced==expected

        where:
        jobname | expected
        'a'     | false
        'b'     | true

    }

    def "api retry job option params"() {
        given:
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.apiService = Mock(ApiService)
        controller.executionService = Mock(ExecutionService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)


        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: se
        ).save()


        when:
        request.api_version=24
        request.method='POST'
        params.executionId = exec.id.toString()
        params.id = se.extid.toString()
        params.putAll(paramoptions)
        def result=controller.apiJobRetry()

        then:

        1 * controller.apiService.requireApi(_,_,24)>>true
        1 * controller.apiService.requireApi(_,_)>>true
        1 * controller.scheduledExecutionService.getByIDorUUID(se.extid.toString())>>se
        2 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_,'project1')
        1 * controller.rundeckAuthContextProcessor.authorizeProjectExecutionAny(_,_,['read','view'])>>true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_,_,['run'],_)>>true
        1 * controller.apiService.requireAuthorized(_,_,_)>>true
        3 * controller.apiService.requireExists(_,_,_)>>true
        1 * controller.executionService.executeJob(
                _,
                _,
                _,
                { it['option.abc'] == 'tyz' && it['option.def'] == 'xyz' }
        ) >> [success: true]
        1 * controller.executionService.respondExecutionsXml(_,_,_)
        0 * controller.executionService._(*_)

        where:

        paramoptions                               | _
        [option: [abc: 'tyz', def: 'xyz']]         | _
        ['option.abc': 'tyz', 'option.def': 'xyz'] | _
    }
    def "api retry job option params json"() {
        given:
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.apiService = Mock(ApiService)
        controller.executionService = Mock(ExecutionService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: se
        ).save()

        when:
        request.api_version = 24
        request.method = 'POST'
        params.executionId = exec.id.toString()
        params.id = se.extid.toString()
        request.json = [
                options: [abc: 'tyz', def: 'xyz']
        ]
        def result = controller.apiJobRetry()

        then:

        1 * controller.apiService.requireApi(_,_,24)>>true
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(se.extid.toString()) >> se
        2 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, 'project1')
        1 * controller.rundeckAuthContextProcessor.authorizeProjectExecutionAny(_, _, ['read', 'view']) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_, _, ['run'], _) >> true
        1 * controller.apiService.requireAuthorized(_, _, _) >> true
        3 * controller.apiService.requireExists(_, _, _) >> true
        1 * controller.executionService.executeJob(
                _,
                _,
                _,
                { it['option.abc'] == 'tyz' && it['option.def'] == 'xyz' }
        ) >> [success: true]
        1 * controller.executionService.respondExecutionsXml(_, _, _)
        0 * controller.executionService._(*_)
    }

    def "isReference deleted parent"(){
        given:
        def refTotal = 10
        def se = new ScheduledExecution(
                uuid: jobuuid,
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def seb = new ScheduledExecution(
                jobName: 'test2',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: null
        ).save()

        //if(expected){
            def re = new ReferencedExecution(scheduledExecution: se,execution: exec).save()
        //}



        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))
        testNodeSet.putNode(new NodeEntryImpl("nodec xyz"))
        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))
        testNodeSetB.putNode(new NodeEntryImpl("nodec xyz"))

        controller.frameworkService=Mock(FrameworkService){
            filterNodeSet(_,_)>>testNodeSetB
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                _*authorizeProjectJobAny(_,_,_,_)>>true
                _*filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
            }

        controller.scheduledExecutionService=Mock(ScheduledExecutionService){
            getByIDorUUID(_)>>se
        }
        controller.notificationService=Mock(NotificationService)
        controller.orchestratorPluginService=Mock(OrchestratorPluginService)
        controller.pluginService = Mock(PluginService)
            controller.featureService = Mock(FeatureService)


        when:
        request.parameters = [id: se.id.toString(),project:'project1',retryFailedExecId:exec.id.toString()]

        def model = controller.show()
        then:
        response.redirectedUrl==null
        model != null
        model.isReferenced==false

        where:
        jobuuid     | expected
        '000000'    | false
        '111111'    | false

    }

    def "unselect nodes from exclude filter"(){
        given:
        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'tags: running',
                filterExclude:'name: nodea',
                excludeFilterUncheck: true,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()

        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))
        testNodeSet.putNode(new NodeEntryImpl("nodec xyz"))

        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))

        NodeSetImpl expected = new NodeSetImpl()
        expected.putNode(new NodeEntryImpl("nodeb"))
        expected.putNode(new NodeEntryImpl("nodec xyz"))

        NodeSet nset = ExecutionService.filtersAsNodeSet(se)
        NodeSet unselectedNset = ExecutionService.filtersExcludeAsNodeSet(se)


        controller.frameworkService=Mock(FrameworkService){
            filterNodeSet(nset,_)>>testNodeSet
            filterNodeSet(unselectedNset,_)>>testNodeSetB
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                _*authorizeProjectJobAny(_,_,_,_)>>true
                _*filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
            }

        controller.scheduledExecutionService=Mock(ScheduledExecutionService){
            getByIDorUUID(_)>>se
        }
        controller.notificationService=Mock(NotificationService)
        controller.orchestratorPluginService=Mock(OrchestratorPluginService)
        controller.pluginService = Mock(PluginService)
            controller.featureService = Mock(FeatureService)


        when:
        request.parameters = [id: se.id.toString(),project:'project1']

        def model = controller.show()

        then:
        model != null
        model.scheduledExecution != null
        null != model.selectedNodes
        expected.nodes*.nodename == model.selectedNodes

    }

    def "show job retry failed exec id empty filter"(){
        given:
        ScheduledExecution.metaClass.static.withNewSession = {Closure c -> c.call() }

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                nodeFilterEditable: true,
                filter:'',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: se
        ).save()


        NodeSetImpl testNodeSetEmpty = new NodeSetImpl()
        NodeSetImpl testNodeSetFailed = new NodeSetImpl()
        testNodeSetFailed.putNode(new NodeEntryImpl("nodea"))
        testNodeSetFailed.putNode(new NodeEntryImpl("nodec xyz"))

        def failedSet = ExecutionService.filtersAsNodeSet([filter: OptsUtil.join("name:", exec.failedNodeList)])
        def nset = ExecutionService.filtersAsNodeSet(se)


            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                _*authorizeProjectJobAny(_,_,_,_)>>true
                _*filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
            }

        controller.frameworkService=Mock(FrameworkService){
            filterNodeSet(nset,_)>>testNodeSetEmpty
            filterNodeSet(failedSet,_)>>testNodeSetFailed
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }
        controller.scheduledExecutionService=Mock(ScheduledExecutionService){
            getByIDorUUID(_)>>se
        }
        controller.notificationService=Mock(NotificationService)
        controller.orchestratorPluginService=Mock(OrchestratorPluginService)
        controller.pluginService = Mock(PluginService)
            controller.featureService = Mock(FeatureService)


        when:
        request.parameters = [id: se.id.toString(),project:'project1',retryFailedExecId:exec.id.toString()]

        def model = controller.show()
        then:
        model != null
        model.scheduledExecution != null
        '' == model.nodefilter
        false == model.nodesetvariables
        'nodec xyz,nodea' == model.failedNodes
        testNodeSetFailed.nodes.size() == model.nodes.size()
    }

    def "show job retry failed exec id when overwrite the filter"() {
        given:
        ScheduledExecution.metaClass.static.withNewSession = {Closure c -> c.call() }

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                nodeFilterEditable: true,
                filter: '.*',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString        : '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter: 'name: nodea',
                succeededNodeList: 'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: se
        ).save()


        NodeSetImpl testNodeSetFilter = new NodeSetImpl()
        testNodeSetFilter.putNode(new NodeEntryImpl("node1"))
        testNodeSetFilter.putNode(new NodeEntryImpl("node2"))
        testNodeSetFilter.putNode(new NodeEntryImpl("node3"))
        testNodeSetFilter.putNode(new NodeEntryImpl("node4"))

        NodeSetImpl testNodeSetFailed = new NodeSetImpl()
        testNodeSetFailed.putNode(new NodeEntryImpl("nodea"))
        testNodeSetFailed.putNode(new NodeEntryImpl("nodec xyz"))

        def failedSet = ExecutionService.filtersAsNodeSet([filter: OptsUtil.join("name:", exec.failedNodeList)])
        def nset = ExecutionService.filtersAsNodeSet(se)


            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                _*authorizeProjectJobAny(_,_,_,_)>>true
                _*filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
            }

        controller.frameworkService = Mock(FrameworkService) {
            filterNodeSet(nset, _) >> testNodeSetFilter
            filterNodeSet(failedSet, _) >> testNodeSetFailed
            getRundeckFramework() >> Mock(Framework) {
                getFrameworkNodeName() >> 'fwnode'
            }
        }
        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            getByIDorUUID(_) >> se
        }
        controller.notificationService = Mock(NotificationService)
        controller.orchestratorPluginService = Mock(OrchestratorPluginService)
        controller.pluginService = Mock(PluginService)
            controller.featureService = Mock(FeatureService)


        when:
        request.parameters = [id: se.id.toString(), project: 'project1', retryFailedExecId: exec.id.toString()]

        def model = controller.show()
        then:
        model != null
        model.scheduledExecution != null
        false == model.nodesetvariables
        'nodec xyz,nodea' == model.failedNodes
        model.nodes.size() == testNodeSetFailed.nodes.size() + testNodeSetFilter.nodes.size()
    }

    @Unroll
    def "upload job file via #type"() {
        given:
            String xmlString = ''' dummy string '''
            def multipartfile = new CommonsMultipartFile(Mock(FileItem){
                getInputStream()>>{new ByteArrayInputStream(xmlString.bytes)}
            })
            ScheduledExecution job = new ScheduledExecution(createJobParams(project:'dunce'))
            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                1 * parseUploadedFile(_, 'xml') >> [
                        jobset: [
                                RundeckJobDefinitionManager.importedJob(job,[:])
                        ]
                ]
                1 * loadImportedJobs(_,_,_,_,_,false)>>[
                        jobs:[job]
                ]
                1 * issueJobChangeEvents(_)
                1 * isScheduled(job)>>true
                1 * nextExecutionTimes([job])>>[(job.id):new Date()]
                0 * _(*_)
            }
            controller.frameworkService = Mock(FrameworkService) {
            }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
            {
                getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext)
            }

            request.method = 'POST'
            params.project='anuncle'
            setupFormTokens(params)
        when:
            if(type=='params'){
                params.xmlBatch=xmlString
            }else if(type=='multipart'){
                params.xmlBatch=multipartfile
            }else if(type=='multipartreq'){
                request.addFile('xmlBatch',xmlString.bytes)
            }else{
                throw new Exception("unexpected")
            }
            controller.uploadPost()
        then:
            response.status == 200
            !request.error
            !request.message
            !request.warn
            !flash.error
            view == '/scheduledExecution/upload'
            model.jobs == [job]
            //job project set to upload parameter
            job.project=='anuncle'
            model.nextExecutions!=null
            model.nextExecutions.size()==1

        where:
            type<<['params','multipart','multipartreq']
    }

    @Unroll
    def "upload job file error from parse result "() {
        given:
            String xmlString = ''' dummy string '''
            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                1 * parseUploadedFile(_, 'xml') >> [
                        (errType):'some error'
                ]
                0 * loadImportedJobs(_,_,_,_,_,false)
                0 * issueJobChangeEvents(_)
            }
            controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
            {
                getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext)
            }

            request.method = 'POST'
            setupFormTokens(params)
        when:
            params.xmlBatch=xmlString
            controller.uploadPost()
        then:
            response.status == 200
            request.error=='some error'
            !request.message
            !request.warn
            !flash.error
            view == '/scheduledExecution/upload'
            model.jobs == null

        where:
            errType<<['errorCode','error']
    }
    def "read/view auth for execution required for apiJobRetry"(){
        given:
            controller.scheduledExecutionService = Mock(ScheduledExecutionService)
            controller.apiService = Mock(ApiService)
            controller.executionService = Mock(ExecutionService)
            controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)

            def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                workflow: new Workflow(
                    keepgoing: true,
                    commands: [
                        new CommandExec([
                            adhocRemoteString: 'test buddy',
                            argString: '-delay 12 -monkey cheese -particle'
                        ])
                    ]
                )
            ).save()
            def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: se
            ).save()

            request.api_version = 24
            request.method = 'POST'
            params.executionId = exec.id.toString()
            params.id = se.extid.toString()
        when:
            def result = controller.apiJobRetry()

        then:

            1 * controller.apiService.requireApi(_,_,24)>>true
            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, _)
            1 * controller.rundeckAuthContextProcessor.authorizeProjectExecutionAny(_, _, ['read','view']) >> false
            1 * controller.apiService.requireExists(_, _, _) >> true
            1 * controller.apiService.requireAuthorized(false, _, _) >> false
            0 * controller.executionService.executeJob(
                _,
                _,
                _,
                { it['option.abc'] == 'tyz' && it['option.def'] == 'xyz' }
            ) >> [success: true]
            0 * controller.executionService.respondExecutionsXml(_, _, _)
            0 * controller.executionService._(*_)
    }

    def "api retry job option jobAsUser not set"() {
        given:
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.apiService = Mock(ApiService)
        controller.executionService = Mock(ExecutionService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: se
        ).save()

        when:
        request.api_version = 24
        request.method = 'POST'
        params.executionId = exec.id.toString()
        params.id = se.extid.toString()
        request.json = [
                options: [abc: 'tyz', def: 'xyz']
        ]
        def result = controller.apiJobRetry()

        then:
        0 * controller.apiService.requireApi(_,_,5)
        0 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_, _, [AuthConstants.ACTION_RUNAS], 'project1')
        1 * controller.apiService.requireApi(_,_,24)>>true
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(se.extid.toString()) >> se
        2 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, 'project1')
        1 * controller.rundeckAuthContextProcessor.authorizeProjectExecutionAny(_, _, ['read', 'view']) >> true
        1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_, _, ['run'], _) >> true
        1 * controller.apiService.requireAuthorized(_, _, _) >> true
        3 * controller.apiService.requireExists(_, _, _) >> true
        1 * controller.executionService.executeJob(
                _,
                _,
                _,
                { it['option.abc'] == 'tyz' && it['option.def'] == 'xyz' }
        ) >> [success: true]
        1 * controller.executionService.respondExecutionsXml(_, _, _)
        0 * controller.executionService._(*_)


    }

    def "detail fragment ajax requires ajax request header"() {
        given:
            params.id = '111'
            params.project = 'aProject'
        when:
            controller.detailFragmentAjax()
        then:
            response.status == 302
            response.redirectedUrl == '/project/aProject/job/show/111'
    }
    @Unroll
    def "detail fragment ajax with ajax request header"() {

        ScheduledExecution job = new ScheduledExecution(createJobParams())
        job.save()
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)

        controller.apiService = Mock(ApiService)

        given: "params for job and request has ajax header"
            params.id = '111'
            params.project = 'AProject'
            request.addHeader('x-rundeck-ajax', 'true')

        when: "request detailFragmentAjax"
            controller.detailFragmentAjax()

        then: "status is correct"
            response.status == expect
            (authed ? 2 : 1) * controller.scheduledExecutionService.getByIDorUUID('111') >> (isFound ? job : null)
            (isFound ? 1 : 0) * controller.
                rundeckAuthContextProcessor.
                authorizeProjectJobAny(_, job, ['read', 'view'], 'AProject') >> authed
        where:
            isFound | authed | expect
            false   | false  | 404
            true    | false  | 403
            true    | true   | 200
    }

    @Unroll
    def "test Show Node Dispatch"() {
        ScheduledExecution.metaClass.static.withNewSession = {Closure c -> c.call() }

        def jobInfo = [
            jobName: 'test1',
            project: 'project1',
            groupPath: 'testgroup',
            doNodedispatch: true,
            filter:'name: nodea,nodeb',
        ]
        ScheduledExecution sec = new ScheduledExecution(createJobParams(jobInfo))
        sec.save()
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            getByIDorUUID(_) >> sec
        }
        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))

        controller.frameworkService = Mock(FrameworkService){
            filterNodeSet(_,_)>>null
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(*_) >> auth

            1 * authorizeProjectJobAny(_, sec, ['read', 'view'], 'project1') >> true
            1 * filterAuthorizedNodes(_,_,_,_)>>testNodeSet
        }

        controller.apiService = Mock(ApiService)
        controller.optionValuesService = Mock(OptionValuesService)
        controller.notificationService = Mock(NotificationService) {
            listNotificationPlugins() >> [:]
        }
        controller.featureService = Mock(FeatureService)
        controller.storageService = Mock(StorageService){
            storageTreeWithContext(_) >> Mock(KeyStorageTree)
        }
        controller.orchestratorPluginService = Mock(OrchestratorPluginService){
            getOrchestratorPlugins() >> null
        }
        controller.pluginService = Mock(PluginService){
            listPlugins() >> []
        }

        given: "params for job"
        params.id = '1'
        params.project = 'project1'


        when: "request detailFragmentAjax"
        def model = controller.show()

        then: "model is correct"
        controller.response.redirectedUrl == null
        null!=model
        null!=model.scheduledExecution
        null == model.selectedNodes
        'fwnode' == model.localNodeName
        'name: nodea,nodeb' == model.nodefilter
        null == model.nodesetvariables
        null == model.failedNodes
        null == model.nodesetempty
        true == model.nodesSelectedByDefault
        testNodeSet.nodes == model.nodes
        [:] == model.grouptags
        null == model.selectedoptsmap
        [:] == model.dependentoptions
        [:] == model.optiondependencies
        null == model.optionordering
        [:] == model.remoteOptionData
    }

    @Unroll
    def "test Show Node Dispatch Selected False"() {
        ScheduledExecution.metaClass.static.withNewSession = {Closure c -> c.call() }

        def jobInfo = [
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                nodesSelectedByDefault: false,
                filter:'name: nodea,nodeb',
        ]
        ScheduledExecution sec = new ScheduledExecution(createJobParams(jobInfo))
        sec.save()
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            getByIDorUUID(_) >> sec
        }
        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))

        controller.frameworkService = Mock(FrameworkService){
            filterNodeSet(_,_)>>null
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(*_) >> auth
            1 * authorizeProjectJobAny(_, sec, ['read', 'view'], 'project1') >> true
            1 * filterAuthorizedNodes(_,_,_,_)>>testNodeSet
        }

        controller.apiService = Mock(ApiService)
        controller.optionValuesService = Mock(OptionValuesService)
        controller.notificationService = Mock(NotificationService) {
            listNotificationPlugins() >> [:]
        }
        controller.featureService = Mock(FeatureService)
        controller.storageService = Mock(StorageService){
            storageTreeWithContext(_) >> Mock(KeyStorageTree)
        }
        controller.orchestratorPluginService = Mock(OrchestratorPluginService){
            getOrchestratorPlugins() >> null
        }
        controller.pluginService = Mock(PluginService){
            listPlugins() >> []
        }

        given: "params for job"
        params.id = '1'
        params.project = 'project1'


        when: "request detailFragmentAjax"
        def model = controller.show()

        then: "model is correct"
        controller.response.redirectedUrl == null
        null!=model
        null!=model.scheduledExecution
        [] == model.selectedNodes
        'fwnode' == model.localNodeName
        'name: nodea,nodeb' == model.nodefilter
        null == model.nodesetvariables
        null == model.failedNodes
        null == model.nodesetempty
        false == model.nodesSelectedByDefault
        testNodeSet.nodes == model.nodes
        [:] == model.grouptags
        null == model.selectedoptsmap
        [:] == model.dependentoptions
        [:] == model.optiondependencies
        null == model.optionordering
        [:] == model.remoteOptionData
    }

    @Unroll
    def "test Show Node Dispatch Selected True"() {
        ScheduledExecution.metaClass.static.withNewSession = {Closure c -> c.call() }

        def jobInfo = [
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                nodesSelectedByDefault: true,
                filter:'name: nodea,nodeb',
        ]
        ScheduledExecution sec = new ScheduledExecution(createJobParams(jobInfo))
        sec.save()
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            getByIDorUUID(_) >> sec
        }
        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))

        controller.frameworkService = Mock(FrameworkService){
            filterNodeSet(_,_)>>null
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(*_) >> auth
            1 * authorizeProjectJobAny(_, sec, ['read', 'view'], 'project1') >> true
            1 * filterAuthorizedNodes(_,_,_,_)>>testNodeSet
        }

        controller.apiService = Mock(ApiService)
        controller.optionValuesService = Mock(OptionValuesService)
        controller.notificationService = Mock(NotificationService) {
            listNotificationPlugins() >> [:]
        }
        controller.featureService = Mock(FeatureService)
        controller.storageService = Mock(StorageService){
            storageTreeWithContext(_) >> Mock(KeyStorageTree)
        }
        controller.orchestratorPluginService = Mock(OrchestratorPluginService){
            getOrchestratorPlugins() >> null
        }
        controller.pluginService = Mock(PluginService){
            listPlugins() >> []
        }

        given: "params for job"
        params.id = '1'
        params.project = 'project1'


        when: "request detailFragmentAjax"
        def model = controller.show()

        then: "model is correct"
        controller.response.redirectedUrl == null
        null!=model
        null!=model.scheduledExecution
        null == model.selectedNodes
        'fwnode' == model.localNodeName
        'name: nodea,nodeb' == model.nodefilter
        null == model.nodesetvariables
        null == model.failedNodes
        null == model.nodesetempty
        true == model.nodesSelectedByDefault
        testNodeSet.nodes == model.nodes
        [:] == model.grouptags
        null == model.selectedoptsmap
        [:] == model.dependentoptions
        [:] == model.optiondependencies
        null == model.optionordering
        [:] == model.remoteOptionData
    }

    @Unroll
    def "test Show Node Dispatch Empty"() {
        ScheduledExecution.metaClass.static.withNewSession = {Closure c -> c.call() }

        def jobInfo = [
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: nodea,nodeb',
        ]
        ScheduledExecution sec = new ScheduledExecution(createJobParams(jobInfo))
        sec.save()
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            getByIDorUUID(_) >> sec
        }
        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        NodeSetImpl testNodeSet = new NodeSetImpl()

        controller.frameworkService = Mock(FrameworkService){
            filterNodeSet(_,_)>>null
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(*_) >> auth
            1 * authorizeProjectJobAny(_, sec, ['read', 'view'], 'project1') >> true
            1 * filterAuthorizedNodes(_,_,_,_)>>testNodeSet
        }

        controller.apiService = Mock(ApiService)
        controller.optionValuesService = Mock(OptionValuesService)
        controller.notificationService = Mock(NotificationService) {
            listNotificationPlugins() >> [:]
        }
        controller.featureService = Mock(FeatureService)
        controller.storageService = Mock(StorageService){
            storageTreeWithContext(_) >> Mock(KeyStorageTree)
        }
        controller.orchestratorPluginService = Mock(OrchestratorPluginService){
            getOrchestratorPlugins() >> null
        }
        controller.pluginService = Mock(PluginService){
            listPlugins() >> []
        }

        given: "params for job"
        params.id = '1'
        params.project = 'project1'


        when: "request detailFragmentAjax"
        def model = controller.show()

        then: "model is correct"
        controller.response.redirectedUrl == null
        null!=model
        null!=model.scheduledExecution
        null == model.selectedNodes
        'fwnode' == model.localNodeName
        'name: nodea,nodeb' == model.nodefilter
        null == model.nodesetvariables
        null == model.failedNodes
        true == model.nodesetempty
        null == model.nodesSelectedByDefault
        null == model.grouptags
        null == model.selectedoptsmap
        [:] == model.dependentoptions
        [:] == model.optiondependencies
        null == model.optionordering
        [:] == model.remoteOptionData
    }

    @Unroll
    def "test Show Node Dispatch Retry ExecId"() {
        ScheduledExecution.metaClass.static.withNewSession = {Closure c -> c.call() }
        def jobInfo = [
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: nodea,nodeb',
        ]
        ScheduledExecution job = new ScheduledExecution(createJobParams(jobInfo))
        job.save()

        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'nodea,fwnode',
                failedNodeList: 'nodeb',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: job
        )
        exec.validate()
        if(exec.hasErrors()){
            exec.errors.allErrors.each{
            }
        }
        exec.save()

        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            getByIDorUUID(_) >> job
        }
        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))
        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))

        controller.frameworkService = Mock(FrameworkService){

            filterNodeSet({ NodesSelector selector->
                selector.acceptNode(new NodeEntryImpl("nodeb"))?testNodeSet:testNodeSetB
            },_)>>testNodeSet >> testNodeSetB
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(*_) >> auth
            1 * authorizeProjectJobAny(_, job, ['read', 'view'], 'project1') >> true
            2 * filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
        }

        controller.apiService = Mock(ApiService)
        controller.optionValuesService = Mock(OptionValuesService)
        controller.notificationService = Mock(NotificationService) {
            listNotificationPlugins() >> [:]
        }
        controller.featureService = Mock(FeatureService)
        controller.storageService = Mock(StorageService){
            storageTreeWithContext(_) >> Mock(KeyStorageTree)
        }
        controller.orchestratorPluginService = Mock(OrchestratorPluginService){
            getOrchestratorPlugins() >> null
        }
        controller.pluginService = Mock(PluginService){
            listPlugins() >> []
        }

        given: "params for job"
        params.id = '1'
        params.project = 'project1'
        params.retryExecId = exec.id

        when: "request detailFragmentAjax"
        def model = controller.show()

        then: "model is correct"
        controller.response.redirectedUrl == null
        null!=model
        null!=model.scheduledExecution
        'fwnode' == model.localNodeName
        'name: nodea,nodeb' == model.nodefilter
        null == model.nodesetvariables
        null == model.failedNodes
        null == model.nodesetempty
        true == model.nodesSelectedByDefault
        testNodeSet.nodes == model.nodes
        ['nodea'] == model.selectedNodes
        [:] == model.grouptags
        null == model.selectedoptsmap
        [:] == model.dependentoptions
        [:] == model.optiondependencies
        null == model.optionordering
        [:] == model.remoteOptionData
    }

    @Unroll
    def "test Show With OptionValues Plugin"() {
        ScheduledExecution.metaClass.static.withNewSession = {Closure c -> c.call() }

        ScheduledExecution job = new ScheduledExecution(createJobParams())
        job.addToOptions(new Option(name: 'optvals', optionValuesPluginType: 'test', required: true, enforced: false))
        job.save()
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            getByIDorUUID(_) >> job
        }
        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))

        controller.frameworkService = Mock(FrameworkService){
            filterNodeSet({ NodesSelector selector->
                selector.acceptNode(new NodeEntryImpl("nodea")) &&
                        selector.acceptNode(new NodeEntryImpl("nodec xyz")) &&
                        !selector.acceptNode(new NodeEntryImpl("nodeb"))

            },_)>>testNodeSetB
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(*_) >> auth
            1 * authorizeProjectJobAny(_, job, ['read', 'view'], 'AProject') >> true
            0 * filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
        }

        controller.apiService = Mock(ApiService)
        controller.optionValuesService = Mock(OptionValuesService){
            getOptions('AProject', 'test',_) >> [[name:"opt1",value:"o1"]]
        }
        controller.notificationService = Mock(NotificationService) {
            listNotificationPlugins() >> [:]
        }
        controller.featureService = Mock(FeatureService)
        controller.storageService = Mock(StorageService){
            storageTreeWithContext(_) >> Mock(KeyStorageTree)
        }
        controller.orchestratorPluginService = Mock(OrchestratorPluginService){
            getOrchestratorPlugins() >> null
        }
        controller.pluginService = Mock(PluginService){
            listPlugins() >> []
        }

        given: "params for job and request has ajax header"
        params.id = '1'
        params.project = 'AProject'


        when: "request detailFragmentAjax"
        def model = controller.show()

        then: "model is correct"
        model.scheduledExecution != null
        model.optionordering == ['optvals']
        model.remoteOptionData == [optvals:[optionDependencies:null,optionDeps:null,localOption:true]]
        job.options[0].valuesFromPlugin.size() == 1
        job.options[0].valuesFromPlugin[0].name == 'opt1'
        job.options[0].valuesFromPlugin[0].value == 'o1'
    }

    @Unroll
    def "test Show With OptionValues Plugin with error"() {
        ScheduledExecution.metaClass.static.withNewSession = {Closure c -> c.call() }

        ScheduledExecution job = new ScheduledExecution(createJobParams())
        job.addToOptions(new Option(name: 'optvals', optionValuesPluginType: 'test', required: true, enforced: false))
        job.save()
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            getByIDorUUID(_) >> job
        }
        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))

        controller.frameworkService = Mock(FrameworkService){
            filterNodeSet({ NodesSelector selector->
                selector.acceptNode(new NodeEntryImpl("nodea")) &&
                        selector.acceptNode(new NodeEntryImpl("nodec xyz")) &&
                        !selector.acceptNode(new NodeEntryImpl("nodeb"))

            },_)>>testNodeSetB
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(*_) >> auth
            1 * authorizeProjectJobAny(_, job, ['read', 'view'], 'AProject') >> true
            0 * filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
        }

        controller.apiService = Mock(ApiService)
        controller.optionValuesService = Mock(OptionValuesService){
            getOptions('AProject', 'test',_) >> {throw new Exception ("error calling plugin")}
        }
        controller.notificationService = Mock(NotificationService) {
            listNotificationPlugins() >> [:]
        }
        controller.featureService = Mock(FeatureService)
        controller.storageService = Mock(StorageService){
            storageTreeWithContext(_) >> Mock(KeyStorageTree)
        }
        controller.orchestratorPluginService = Mock(OrchestratorPluginService){
            getOrchestratorPlugins() >> null
        }
        controller.pluginService = Mock(PluginService){
            listPlugins() >> []
        }

        given: "params for job and request has ajax header"
        params.id = '1'
        params.project = 'AProject'


        when: "request detailFragmentAjax"
        def model = controller.show()

        then: "model is correct"
        model.scheduledExecution != null
        job.options[0].valuesFromPlugin == null
    }

    def "test Save Fail"(){

        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        se.save()


        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))

        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            getAuthContextForSubjectAndProject(*_) >> auth
            authorizeProjectJobAll(_, _, ['create','view'], _) >> true
        }

        controller.apiService = Mock(ApiService)

        given: "params for job and request has ajax header"

        params.jobName = 'monkey1'
        params.project = 'testProject'
        params.description = 'blah'
        params.workflow =  [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]

        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
        request.setAttribute("subject", subject)
        request.method = "POST"
        setupFormTokens(params)

        when: "request detailFragmentAjax"


        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            _docreateJobOrParams(_,_,_,_) >> [success: false]
            getByIDorUUID() >> se
            1 * prepareCreateEditJob(params,
                    {scheduledExecution -> scheduledExecution == null },
                    AuthConstants.ACTION_CREATE,
                    _) >> [scheduledExecution: null]
        }

        def model = controller.save()

        then: "model is correct"
        response.redirectedUrl == null
        request.message != null
        view == '/scheduledExecution/create'
    }

    def "test Save Unauthorized"(){

        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        se.save()


        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))

        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            getAuthContextForSubjectAndProject(*_) >> auth
            authorizeProjectJobAll(_, _, ['create','view'], _) >> true
        }

        controller.apiService = Mock(ApiService)

        given: "params for job and request has ajax header"

        params.jobName = 'monkey1'
        params.project = 'testProject'
        params.description = 'blah'
        params.workflow =  [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]

        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
        request.setAttribute("subject", subject)
        request.method = "POST"
        setupFormTokens(params)

        when: "request detailFragmentAjax"
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            _docreateJobOrParams(_,_,_,_) >>  [success: false,unauthorized:true,error:'unauthorizedMessage']
            getByIDorUUID() >> se
            1 * prepareCreateEditJob(params,
                    {scheduledExecution -> scheduledExecution == null },
                    AuthConstants.ACTION_CREATE,
                    _) >> [scheduledExecution: null]
        }

        def model = controller.save()

        then: "model is correct"
        response.redirectedUrl == null
        request.message == 'unauthorizedMessage'
        view == '/scheduledExecution/create'
    }


    def "test copy"(){

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'monkey1', project: 'testProject', description: 'blah2',
                groupPath: 'testgroup',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        )

        se.save()


        def auth = Mock(UserAndRolesAuthContext){
            getUsername() >> 'bob'
        }

        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))

        controller.frameworkService = Mock(FrameworkService){
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
            }
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            getAuthContextForSubjectAndProject(*_) >> auth
            authorizeProjectJobAll(_, _, ['create','view'], _) >> true
            authorizeProjectResource(_, _ , 'create' , _ ) >> true
            authorizeProjectJobAll(_, _ , ['read'] , _ ) >> true
        }


        controller.apiService = Mock(ApiService)
        controller.rundeckJobDefinitionManager=new RundeckJobDefinitionManager()

        given: "params for job and request has ajax header"

        params.id = se.id.toString()

        setupFormTokens(params)

        when: "request detailFragmentAjax"
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            _docreateJobOrParams(_,_,_,_) >>  [success: false,unauthorized:true,error:'unauthorizedMessage']
            userAuthorizedForJob(_,_,_) >> true
            getByIDorUUID(_) >> se
            1 * prepareCreateEditJob(_,
                    {newScheduledExecution ->
                        newScheduledExecution.jobName == se.jobName
                    },
                    _,
                    _) >> [scheduledExecution: se]
        }

        controller.copy()

        then: "model is correct"
        controller.modelAndView.model.scheduledExecution != null
    }

    def "api jobs import require 14"() {
        given:
            controller.apiService = Mock(ApiService)
            request.method = 'POST'
        when:
            controller.apiJobsImportv14()
        then:
            1 * controller.apiService.requireApi(_, _, 14) >> false

    }
    def "api jobs import require project param"() {
        given:
            controller.apiService = Mock(ApiService)
            request.method = 'POST'
        when:
            controller.apiJobsImportv14()
        then:
            1 * controller.apiService.requireApi(_, _, 14) >> true
            1 * controller.apiService.requireParameters(_, _, ['project']) >> false

    }

    def "api jobs import content type #type"() {
        given:
            controller.apiService = Mock(ApiService)
            controller.scheduledExecutionService = Mock(ScheduledExecutionService)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
            request.method = 'POST'
            request.contentType = type
            request.format = format
            params.project = 'aproj'
            def job = new ScheduledExecution()
            def jobset = [
                Mock(ImportedJob) {
                    getJob() >> job
                }
            ]
            params.dupeOption = dupeoption
            params.uuidOption = uuidoption
            params.validateJobref = validateJobref
        when:
            controller.apiJobsImportv14()
        then:
            1 * controller.apiService.requireApi(_, _, 14) >> true
            1 * controller.apiService.requireParameters(_, _, ['project']) >> true
            1 * controller.scheduledExecutionService.parseUploadedFile(!null, format) >> [
                jobset: jobset
            ]
            1 * controller.
                scheduledExecutionService.
                loadImportedJobs(jobset, dupeoption, uuidoption, _, _, validateJobref == 'true') >> [:]
            1 * controller.scheduledExecutionService.issueJobChangeEvents(_)
            1 * controller.apiService.renderSuccessXml(_, _, _)
            job.project == 'aproj'
        where:
            type               | format | dupeoption | uuidoption | validateJobref
            'application/xml'  | 'xml'  | null       | null       | null
            'application/yaml' | 'yaml' | null       | null       | null
            'application/yaml' | 'yaml' | null       | null       | 'true'
            'application/yaml' | 'yaml' | null       | 'remove'   | null
            'application/yaml' | 'yaml' | 'update'   | null       | null
    }
    @Unroll
    def "api jobs import xmlBatch text format #format fileformat #fformat"() {
        given:
            controller.apiService = Mock(ApiService)
            controller.scheduledExecutionService = Mock(ScheduledExecutionService)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
            request.method = 'POST'
            def job = new ScheduledExecution()
            def jobset = [
                Mock(ImportedJob) {
                    getJob() >> job
                }
            ]
            params.project = 'aproj'
            params.dupeOption = dupeoption
            params.uuidOption = uuidoption
            params.validateJobref = validateJobref
            params.format = format
            params.fileformat = fformat
            params.xmlBatch='datacontent'
        when:
            controller.apiJobsImportv14()
        then:
            1 * controller.apiService.requireApi(_, _, 14) >> true
            1 * controller.apiService.requireParameters(_, _, ['project']) >> true
            1 * controller.apiService.requireParameters(_, _, ['xmlBatch']) >> true
            1 * controller.scheduledExecutionService.parseUploadedFile('datacontent', (format?:fformat?:'xml')) >> [
                jobset: jobset
            ]
            1 * controller.
                scheduledExecutionService.
                loadImportedJobs(jobset, dupeoption, uuidoption, _, _, validateJobref == 'true') >> [:]
            1 * controller.scheduledExecutionService.issueJobChangeEvents(_)
            1 * controller.apiService.renderSuccessXml(_, _, _)
            job.project == 'aproj'
        where:
            format |fformat | dupeoption | uuidoption | validateJobref
            null |null  | null       | null       | null
            null |'xml'  | null       | null       | null
            null |'yaml' | null       | null       | null
            null |'yaml' | null       | null       | 'true'
            null |'yaml' | null       | 'remove'   | null
            null |'yaml' | 'update'   | null       | null
            'xml'  |null | null       | null       | null
            'yaml' |null | null       | null       | null
            'yaml' |null | null       | null       | 'true'
            'yaml' |null | null       | 'remove'   | null
            'yaml' |null | 'update'   | null       | null
    }
    def "api jobs import multipart file #format fileformat #fformat"() {
        given:
            controller.apiService = Mock(ApiService)
            controller.scheduledExecutionService = Mock(ScheduledExecutionService)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
            request.method = 'POST'
            def job = new ScheduledExecution()
            def jobset = [
                Mock(ImportedJob) {
                    getJob() >> job
                }
            ]
            params.project = 'aproj'
            params.dupeOption = dupeoption
            params.uuidOption = uuidoption
            params.validateJobref = validateJobref
            params.format = format
            params.fileformat = fformat
            request.format = 'multipartForm'
            request.addFile('xmlBatch','datacontent'.bytes)
        when:
            controller.apiJobsImportv14()
        then:
            1 * controller.apiService.requireApi(_, _, 14) >> true
            1 * controller.apiService.requireParameters(_, _, ['project']) >> true
            1 * controller.apiService.requireParameters(_, _, ['xmlBatch']) >> true
            1 * controller.scheduledExecutionService.parseUploadedFile(!null, (format?:fformat?:'xml')) >> [
                jobset: jobset
            ]
            1 * controller.
                scheduledExecutionService.
                loadImportedJobs(jobset, dupeoption, uuidoption, _, _, validateJobref == 'true') >> [:]
            1 * controller.scheduledExecutionService.issueJobChangeEvents(_)
            1 * controller.apiService.renderSuccessXml(_, _, _)
            job.project == 'aproj'
        where:
            format |fformat | dupeoption | uuidoption | validateJobref
            null |null  | null       | null       | null
            null |'xml'  | null       | null       | null
            null |'yaml' | null       | null       | null
            null |'yaml' | null       | null       | 'true'
            null |'yaml' | null       | 'remove'   | null
            null |'yaml' | 'update'   | null       | null
            'xml'  |null | null       | null       | null
            'yaml' |null | null       | null       | null
            'yaml' |null | null       | null       | 'true'
            'yaml' |null | null       | 'remove'   | null
            'yaml' |null | 'update'   | null       | null
    }

    def "test broken plugin on editing"() {

        given:
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        se.save()

        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'bob'
        }

        params.id = se.id
        params.project = se.project

        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            getByIDorUUID(_) >> se
            prepareCreateEditJob(_,
                    { newScheduledExecution ->
                        newScheduledExecution.jobName == se.jobName
                    },
                    _,
                    _) >> [scheduledExecution: se]
            1 * validateJobDefinition(_,_,_,_,_) >> false
        }

        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
            getAuthContextForSubjectAndProject(*_) >> auth
            authorizeProjectJobAll(_, _, ['update', 'read'], _) >> true
        }

        when:
        controller.edit()

        then:
        controller.response.redirectedUrl == null
        flash.message != null
    }

    def "test special chars in URL user-password"() {
        given:
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)

        when:
        controller.getRemoteJSON(url, 100, 100, 5,false)

        then:
        def e = thrown(UnknownHostException)
        e.message.contains("web.server")

        where:
        url                                                         |_
        'https://admin:my^$!pass1@web.server/option.json'           |_
        'https://admin:m^^y^^$!pass1@web.server/option.json'        |_
        'https://web.server/geto'                                   |_
        'http://web.server/geto'                                    |_
    }
}
