package rundeck.controllers

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService
import spock.lang.Specification

/**
 * Created by greg on 7/14/15.
 */
@TestFor(ScheduledExecutionController)
@Mock([ScheduledExecution,Workflow,CommandExec])
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

    def "api scheduler takeover XML input"(String requestXml, String requestUUID, boolean allserver, String project){
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
            1 * reclaimAndScheduleJobs(requestUUID,allserver,project)>>[:]
        }
        when:
        request.method='PUT'
        request.XML=requestXml
        response.format='xml'
        def result=controller.apiJobClusterTakeoverSchedule()

        then:
        response.status==200

        where:
        requestXml                                                                                              | requestUUID | allserver | project
        "<server uuid='${TEST_UUID1}' />".toString()                                                            | TEST_UUID1  | false     | null
        "<server all='true' />".toString()                                                                      | null        | true      | null
        "<takeoverSchedule><server uuid='${TEST_UUID1}' /></takeoverSchedule>".toString()                       | TEST_UUID1  | false     | null
        "<takeoverSchedule><server all='true' /></takeoverSchedule>".toString()                                 | null        | true      | null
        '<takeoverSchedule><server all="true" /><project name="asdf"/></takeoverSchedule>'                      | null        | true      | 'asdf'
        "<takeoverSchedule><server uuid='${TEST_UUID1}' /><project name='asdf'/></takeoverSchedule>".toString() | TEST_UUID1  | false     | 'asdf'
    }


}
