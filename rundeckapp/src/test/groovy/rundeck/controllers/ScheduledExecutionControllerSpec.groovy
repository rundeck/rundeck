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
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.core.utils.OptsUtil
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.plugins.codecs.URLCodec
import org.grails.plugins.testing.GrailsMockMultipartFile
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import rundeck.*
import rundeck.codecs.URIComponentCodec
import rundeck.services.*
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject

/**
 * Created by greg on 7/14/15.
 */
@TestFor(ScheduledExecutionController)
@Mock([ScheduledExecution, Option, Workflow, CommandExec, Execution, JobExec, ReferencedExecution, ScheduledExecutionStats])
class ScheduledExecutionControllerSpec extends Specification {
    def setup() {
        mockCodec(URIComponentCodec)
        mockCodec(URLCodec)
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

    def "workflow json"() {
        given:
        ScheduledExecution job = new ScheduledExecution(createJobParams())
        controller.frameworkService = Mock(FrameworkService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)

        when:
        params.id = job.extid
        def result = controller.workflowJson()

        then:
        1 * controller.frameworkService.authorizeProjectJobAny(_, job, ['read', 'view'], 'AProject') >> true
        1 * controller.frameworkService.authorizeProjectJobAny(_, job, ['read'], 'AProject') >> readauth
        1 * controller.scheduledExecutionService.getByIDorUUID(_) >> job
        1 * controller.scheduledExecutionService.getWorkflowDescriptionTree('AProject', _, readauth, 3) >>
        [test: 'data']
        response.json == [workflow: [test: 'data']]

        where:
        readauth | _
        true     | _
        false    | _

    }

    def "flip execution enabled"() {
        given:
        def job1 = new ScheduledExecution(createJobParams())
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.frameworkService = Mock(FrameworkService)

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
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(*_) >> auth
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
    def "flip schedule enabled"() {
        given:
        def job1 = new ScheduledExecution(createJobParams())
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.frameworkService = Mock(FrameworkService)

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
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(*_) >> auth
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

        when:
        request.api_version=18
        request.method='POST'
        params.id='ajobid'
        params.putAll(paramoptions)
        def result=controller.apiJobRun()

        then:


        1 * controller.apiService.requireApi(_,_)>>true
        1 * controller.scheduledExecutionService.getByIDorUUID('ajobid')>>[:]
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_,_)
        1 * controller.frameworkService.authorizeProjectJobAll(_,_,['run'],_)>>true
        1 * controller.apiService.requireExists(_,_,_)>>true
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
    def "api run job option params json"() {
        given:
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.apiService = Mock(ApiService)
        controller.executionService = Mock(ExecutionService)
        controller.frameworkService = Mock(FrameworkService)

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
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, _)
        1 * controller.frameworkService.authorizeProjectJobAll(_, _, ['run'], _) >> true
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

        when:
        request.api_version = 18
        request.method = 'POST'
        params.id = 'ajobid'
        def result = controller.apiJobRun()

        then:


        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID('ajobid') >> [:]
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, _)
        1 * controller.frameworkService.authorizeProjectJobAll(_, _, ['run'], _) >> true
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

        when:
        request.api_version = 18
        request.method = 'POST'
        params.id = 'ajobid'
        params.runAtTime = 'timetorun'
        def result = controller.apiJobRun()

        then:


        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID('ajobid') >> [:]
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, _)
        1 * controller.frameworkService.authorizeProjectJobAll(_, _, ['run'], _) >> true
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
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_,_)
        1 * controller.frameworkService.authorizeProjectJobAll(_,_,['run'],_)>>true
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
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1* renderSuccessXmlWrap(_,_,{args->
                args.delegate=[message:{str->
                    'No action performed, cluster mode is not enabled.'==str
                }]
                args.call()
            })>>null
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_)>>null
            1 * authorizeApplicationResource(_,_,_)>>true
            1 * isClusterModeEnabled()>>false
        }
        when:
        request.method='PUT'
        request.XML="<server uuid='${serverUUID1}' />"
        response.format='xml'
        def result=controller.apiJobClusterTakeoverSchedule()

        then:
        response.status==200
    }

    @Unroll
    def "api scheduler takeover XML input"(String requestXml, String requestUUID, boolean allserver, String project, String[] jobid, int api_version){
        given:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * renderSuccessXml(_,_,_) >> 'result'
            0 * renderErrorFormat(_,_,_) >> null
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_)>>null
            1 * authorizeApplicationResource(_,_,_)>>true
            1 * isClusterModeEnabled()>>true
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

        controller.frameworkService=Mock(FrameworkService){
            authorizeProjectJobAny(_,_,_,_)>>true
            filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
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
    def "show job download #format has content-disposition header"() {
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
            authorizeProjectJobAny(_, _, ['read'], _) >> true
            filterAuthorizedNodes(_, _, _, _) >> { args -> args[2] }
            filterNodeSet(_, _) >> testNodeSetB
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
        when:
        request.parameters = [id: se.id.toString(), project: 'project1']
        response.format = format
        def model = controller.show()
        then:
        response.status == 200
        response.header('Content-Disposition') == "attachment; filename=\"test1.$format\""
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
        controller.runJobInline(command, extra)

        then:
        response.status == 200
        response.contentType.contains 'application/json'
        response.json == [
                href   : "/execution/follow/${exec.id}",
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
                    href   : "/execution/follow/${exec.id}#"+follow,
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
            getAuthContextForSubjectAndProject(*_) >> testcontext
            authorizeProjectJobAll(*_) >> true
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
                href   : "/execution/follow/${exec.id}",
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
            getAuthContextForSubjectAndProject(*_) >> testcontext
            authorizeProjectJobAll(*_) >> true
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
                    href   : "/execution/follow/${exec.id}#" + follow,
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
            getAuthContextForSubjectAndProject(*_) >> testcontext
            authorizeProjectJobAll(*_) >> true
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
            getAuthContextForSubjectAndProject(*_) >> testcontext
            authorizeProjectJobAll(*_) >> true
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
            getAuthContextForSubjectAndProject(*_) >> testcontext
            authorizeProjectJobAll(_,_,['run'],'testProject') >> true
            authorizeProjectJobAny(*_) >> true
            getRundeckFramework() >> Mock(Framework) {
                getFrameworkNodeName() >> 'fwnode'
            }
            //0 * _(*_)
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
            requireVersion(_, _, 19) >> true
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
            1 * getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext)
            authorizeProjectJobAll(*_) >> true
            0 * _(*_)
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
            requireVersion(_, _, 19) >> true
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
            1 * getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext)
            authorizeProjectJobAll(*_) >> true
            0 * _(*_)
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
            _ * getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext) {
                getRoles() >> ['a', 'b']
                getUsername() >> 'bob'
            }
            authorizeProjectResourceAll(_, _, _, _) >> true
            authorizeProjectExecutionAny(_, exec, _) >> true
            getProjectGlobals(_) >> [:]
        }
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.notificationService = Mock(NotificationService) {
            1 * listNotificationPlugins() >> [:]
        }
        controller.orchestratorPluginService = Mock(OrchestratorPluginService) {
            1 * listDescriptions()
        }
        controller.pluginService = Mock(PluginService)
        when:
        def result = controller.createFromExecution()
        then:
        response.status == 200
        model.scheduledExecution != null
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
            getAuthContextForSubjectAndProject(*_) >> testcontext
            authorizeProjectJobAll(_,_,['run'],'testProject') >> true
            authorizeProjectJobAny(*_) >> true
            getRundeckFramework() >> Mock(Framework) {
                getFrameworkNodeName() >> 'fwnode'
            }
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

        controller.frameworkService=Mock(FrameworkService){
            authorizeProjectJobAll(_,_,['run'],'testProject') >> true
            authorizeProjectJobAny(_,_,_,_)>>true
            filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
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

        controller.frameworkService=Mock(FrameworkService){
            authorizeProjectJobAny(_,_,_,_)>>true
            filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
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
            authorizeProjectJobAny(_,_,_,_)>>true
            filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
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

        1 * controller.apiService.requireVersion(_,_,24)>>true
        1 * controller.apiService.requireApi(_,_)>>true
        1 * controller.scheduledExecutionService.getByIDorUUID(se.extid.toString())>>[:]
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_,_)
        1 * controller.frameworkService.authorizeProjectJobAll(_,_,['run'],_)>>true
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

        1 * controller.apiService.requireVersion(_,_,24)>>true
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.scheduledExecutionService.getByIDorUUID(se.extid.toString()) >> [:]
        1 * controller.frameworkService.getAuthContextForSubjectAndProject(_, _)
        1 * controller.frameworkService.authorizeProjectJobAll(_, _, ['run'], _) >> true
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
            authorizeProjectJobAny(_,_,_,_)>>true
            filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
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
            authorizeProjectJobAny(_,_,_,_)>>true
            filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
            filterNodeSet(nset,_)>>testNodeSet
            filterNodeSet(unselectedNset,_)>>testNodeSetB
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

        controller.frameworkService=Mock(FrameworkService){
            authorizeProjectJobAny(_,_,_,_)>>true
            filterAuthorizedNodes(_,_,_,_)>>{args-> args[2]}
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

        controller.frameworkService = Mock(FrameworkService) {
            authorizeProjectJobAny(_, _, _, _) >> true
            filterAuthorizedNodes(_, _, _, _) >> { args -> args[2] }
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
}
