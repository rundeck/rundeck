package rundeck.services

import grails.test.mixin.Mock
import org.quartz.Scheduler
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull

/**
 * Created by ronaveva on 2/17/20.
 */
@Mock([ScheduledExecution])
class LocalJobSchedulesManagerSpec extends Specification {

    public static final String TEST_UUID2 = '490966E0-2E2F-4505-823F-E2665ADC66FB'

    private Map createJobParams(Map overrides = [:]) {
        [
                jobName       : 'blue',
                project       : 'AProject',
                groupPath     : 'some/where',
                description   : 'a job',
                argString     : '-a b -c d',
                workflow      : new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec([adhocRemoteString: 'test buddy'])]
                ),
                serverNodeUUID: null,
                scheduled     : true
        ] + overrides
    }

    def "nextExecutionTime"() {
        given:
        LocalJobSchedulesManager service = new LocalJobSchedulesManager()
        service.quartzScheduler = Mock(Scheduler)
        service.quartzScheduler.getTrigger(_) >> null
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            isProjectScheduledEnabled(_) >> projectScheduleEnabled
            isProjectExecutionEnabled(_) >> executionEnabled
            registerOnQuartz(_,_,_,_) >> new Date()
        }

        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b',
                        serverNodeUUID: TEST_UUID2
                )
        ).save()

        when:
        def result = service.nextExecutionTime(job)

        then:
        if(expectScheduled){
            assertNotNull(result)
        }else{
            assertNull(result)
        }


        where:
        scheduleEnabled | executionEnabled | hasSchedule | expectScheduled  | projectScheduleEnabled
        true            | true             | true        | true             | true
        false           | true             | true        | false            | true
        true            | false            | true        | false            | true
        false           | false            | true        | false            | true
        false           | false            | true        | false            | false
    }

    def "nextExecutions"(){
        given:
        LocalJobSchedulesManager service = new LocalJobSchedulesManager();
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: true,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b',
                        serverNodeUUID: TEST_UUID2
                )
        ).save()

        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            createJobDetail(_) >> null
            applyTriggerComponents(_,_) >> service.createTriggerBuilder(job.uuid,null,null)
        }
        when:
        def result = service.nextExecutions(job.uuid, new Date().plus(10), false)
        then:
        result.size() == 10

    }

}
