package rundeck.services

import grails.testing.gorm.DataTest
import org.quartz.Scheduler
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by ronaveva on 2/17/20.
 */
class LocalJobSchedulesManagerSpec extends Specification implements DataTest {

    def setupSpec() { mockDomains ScheduledExecution, CommandExec }

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

    @Unroll
    def "nextExecutionTime"() {
        given:
        LocalJobSchedulesManager service = new LocalJobSchedulesManager()
        service.quartzScheduler = Mock(Scheduler)
        service.quartzScheduler.getTrigger(_) >> null
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            isProjectScheduledEnabled(_) >> projectScheduleEnabled
            isProjectExecutionEnabled(_) >> projectExecutionEnabled
            0 * registerOnQuartz(*_)
        }

        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b',
                        serverNodeUUID: TEST_UUID2,
                        year: year
                )
        ).save()

        when:
        def result = service.nextExecutionTime(job)

        then:
        if (expectScheduled) {
            assert null != result
        } else {
            assert null == result
        }


        where:
            scheduleEnabled | executionEnabled | hasSchedule | projectExecutionEnabled | projectScheduleEnabled | year  | expectScheduled
            true            | true             | true        | true                    | true                   | '*'   | true
            true            | true             | true        | true                    | true                   | '1971'| false
            false           | true             | true        | true                    | true                   | '*'   | false
            true            | false            | true        | true                    | true                   | '*'   | false
//            true            | true             | false       | true                    | true                   | '*'   | false
            true            | true             | true        | false                   | true                   | '*'   | false
            true            | true             | true        | true                    | false                  | '*'   | false
            false           | false            | true        | true                    | true                   | '*'   | false
            false           | false            | true        | true                    | false                  | '*'   | false
            true            | true             | true        | false                   | true                   | '*'   | false
            true            | true             | true        | false                   | true                   | '1971'| false
            false           | true             | true        | false                   | true                   | '*'   | false
            true            | false            | true        | false                   | true                   | '*'   | false
            false           | false            | true        | false                   | true                   | '*'   | false
            false           | false            | true        | false                   | false                  | '*'   | false
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
