package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.NodesSelector
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import rundeck.services.NotificationService
import rundeck.services.OrchestratorPluginService
import rundeck.services.ScheduledExecutionService
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject

/**
 * Created by greg on 7/14/15.
 */
@TestFor(ScheduledExecutionController)
@Mock([ScheduledExecution,Workflow,CommandExec,Execution])
class ScheduledExecutionControllerSpec extends Specification {


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
        response.redirectedUrl=='/menu/jobs?project='

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
        response.redirectedUrl=='/menu/jobs?project='

        where:
        isEnabled | _
        true      | _
        false     | _
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
    def "api scheduler takeover XML input"(String requestXml, String requestUUID, boolean allserver, String project, String jobid){
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
        response.format='xml'
        def result=controller.apiJobClusterTakeoverSchedule()

        then:
        response.status==200

        where:
        requestXml                                                                                              | requestUUID | allserver | project | jobid
        "<server uuid='${TEST_UUID1}' />".toString()                                                            | TEST_UUID1  | false     | null    | null
        "<server all='true' />".toString()                                                                      | null        | true      | null    | null
        "<takeoverSchedule><server uuid='${TEST_UUID1}' /></takeoverSchedule>".toString()                       | TEST_UUID1  | false     | null    | null
        "<takeoverSchedule><server all='true' /></takeoverSchedule>".toString()                                 | null        | true      | null    | null
        '<takeoverSchedule><server all="true" /><project name="asdf"/></takeoverSchedule>'                      | null        | true      | 'asdf'  | null
        '<takeoverSchedule><server all="true" /><job id="ajobid"/></takeoverSchedule>'                          | null        | true      | null    | 'ajobid'
        "<takeoverSchedule><server uuid='${TEST_UUID1}' /><project name='asdf'/></takeoverSchedule>".toString() | TEST_UUID1  | false     | 'asdf'  | null
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
            authorizeProjectJobAll(_,_,_,_)>>true
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
}
