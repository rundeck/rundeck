package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRoles
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Notification
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.controllers.ScheduledExecutionController
import spock.lang.Specification

/**
 * Created by greg on 6/24/15.
 */
@TestFor(ScheduledExecutionService)
@Mock([Workflow, ScheduledExecution, CommandExec, Notification])
class ScheduledExecutionServiceSpec extends Specification {

    public static final String TEST_UUID1 = 'BB27B7BB-4F13-44B7-B64B-D2435E2DD8C7'
    public static final String TEST_UUID2 = '490966E0-2E2F-4505-823F-E2665ADC66FB'

    def "blank email notification"() {
        given:
        service.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject(_) >> true
        }

        when:
        def params = [jobName       : 'monkey1',
                      project       : 'testProject',
                      description   : 'blah',
                      adhocExecution: false,

                      workflow      : new Workflow(
                              threadcount: 1,
                              keepgoing: true,
                              commands: [new CommandExec(adhocRemoteString: 'a remote string')]
                      ),
                      notifications : [
                              [
                                      eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                                      type        : ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                                      content     : ''
                              ]
                      ]
        ]
        def results = service._dovalidate(params, Mock(UserAndRoles))
        def ScheduledExecution scheduledExecution = results.scheduledExecution

        then:

        results.failed
        scheduledExecution != null
        scheduledExecution instanceof ScheduledExecution

        scheduledExecution.errors != null
        scheduledExecution.errors.hasErrors()
        scheduledExecution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS)

    }
    def "blank webhook notification"() {
        given:
        service.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject(_) >> true
        }

        when:
        def params = [jobName       : 'monkey1',
                      project       : 'testProject',
                      description   : 'blah',
                      adhocExecution: false,

                      workflow      : new Workflow(
                              threadcount: 1,
                              keepgoing: true,
                              commands: [new CommandExec(adhocRemoteString: 'a remote string')]
                      ),
                      notifications : [
                              [
                                      eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                                      type        : ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                                      content     : ''
                              ]
                      ]
        ]
        def results = service._dovalidate(params, Mock(UserAndRoles))
        def ScheduledExecution scheduledExecution = results.scheduledExecution

        then:

        results.failed
        scheduledExecution != null
        scheduledExecution instanceof ScheduledExecution

        scheduledExecution.errors != null
        scheduledExecution.errors.hasErrors()
        scheduledExecution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_URL)

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

    def "claim all scheduled jobs"(){
        given:
        def targetserverUUID = UUID.randomUUID().toString()
        def serverUUID1 = UUID.randomUUID().toString()
        def serverUUID2 = UUID.randomUUID().toString()
        ScheduledExecution job1 = new ScheduledExecution(createJobParams(jobName:'blue1',project:'AProject',serverNodeUUID:null)).save()
        ScheduledExecution job2 = new ScheduledExecution(createJobParams(jobName:'blue2',project:'AProject2',serverNodeUUID:serverUUID1)).save()
        ScheduledExecution job3 = new ScheduledExecution(createJobParams(jobName:'blue3',project:'AProject2',serverNodeUUID:serverUUID2)).save()
        ScheduledExecution job4 = new ScheduledExecution(createJobParams(jobName:'blue4',project:'AProject2',scheduled:false)).save()
        def jobs=[job1,job2,job3,job4]
        when:
        def resultMap=service.claimScheduledJobs(targetserverUUID,null,true)

        ScheduledExecution.withSession { session ->
            session.flush()
            jobs*.refresh()
        }
        then:

        [job1,job2,job3]==jobs.findAll{it.serverNodeUUID==targetserverUUID}
        [job1,job2,job3]*.extid == resultMap.keySet() as List
    }
    def "claim all scheduled jobs in a project"(String targetProject, String targetServerUUID, String serverUUID1, List<Map> dataList, List<String> resultList){
        setup:
        def jobs = dataList.collect{
            new ScheduledExecution(createJobParams(it)).save()
        }

        when:
        def resultMap=service.claimScheduledJobs(targetServerUUID,null,true,targetProject)

        ScheduledExecution.withSession { session ->
            session.flush()
            jobs*.refresh()
        }
        then:
        resultList==jobs.findAll{it.serverNodeUUID==targetServerUUID}*.uuid

        resultList == resultMap.keySet() as List

        where:
        targetProject | targetServerUUID| serverUUID1|dataList | resultList
        'AProject'    | TEST_UUID1 | TEST_UUID2 |[[uuid:'job1',serverNodeUUID: TEST_UUID2],[project:'AProject2',uuid:'job2']]       | ['job1']
        'AProject2'   | TEST_UUID1 | TEST_UUID2 |[[uuid:'job1',serverNodeUUID: TEST_UUID2],[project:'AProject2',uuid:'job2']]       | ['job2']
    }
}
