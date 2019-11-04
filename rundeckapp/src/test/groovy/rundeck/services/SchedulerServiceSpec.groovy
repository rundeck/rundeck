package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.schedule.JobCalendarBase
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.quartz.ListenerManager
import org.quartz.Scheduler
import org.quartz.impl.calendar.BaseCalendar
import rundeck.ScheduleDef
import rundeck.ScheduledExecution
import spock.lang.Specification

import java.text.DateFormat
import java.text.SimpleDateFormat


@TestFor(SchedulerService)
@Mock([ScheduleDef, ScheduledExecution])
class SchedulerServiceSpec extends Specification {

    public static final String TEST_UUID1 = 'BB27B7BB-4F13-44B7-B64B-D2435E2DD8C7'
    public static final String TEST_UUID2 = 'BB27B7BB-4F13-44B7-B64B-D2435E2DD8C8'

    def setupMockCalendards(){
        service.jobSchedulerCalendarService = Mock(JobSchedulerCalendarService){
            isCalendarEnable() >> false
        }
    }

    def setupBeans(scheduleDate = null){
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
            if(scheduleDate){
                scheduleJob(_, _) >> scheduleDate
            }
        }
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }
        service.frameworkService = Mock(FrameworkService) {
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
            getServerUUID() >> 'uuid'
            isClusterModeEnabled() >> false
        }
    }

    void "retrieveProjectSchedulesDefinitionsWithFilters not found"() {
        given:
            ScheduleDef scheduleDef = new ScheduleDef(name: 'scheduleName', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON')
            scheduleDef.save()
        when:
            def result = service.retrieveProjectSchedulesDefinitionsWithFilters(projectName, containsName, paginationParams, filteredNames)
        then:
            result.schedules.isEmpty()

        where:
        projectName     | containsName      | paginationParams      | filteredNames
        'testProject'   | 'scheduleName 1'  | [max:10, offset:0]    | []
        'testProject'   | 'schedule'        | [max:10, offset:0]    | ['scheduleName']
    }

    void "retrieveProjectSchedulesDefinitionsWithFilters found"() {
        given:
            ScheduleDef scheduleDef = new ScheduleDef(name: 'scheduleName', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON')
            scheduleDef.save()
        when:
            def result = service.retrieveProjectSchedulesDefinitionsWithFilters(projectName, containsName, paginationParams, filteredNames)
        then:
            !result.schedules.isEmpty()
            result.schedules[0].name == 'scheduleName'

        where:
        projectName     | containsName      | paginationParams      | filteredNames
        'testProject'   | 'schedule'        | [max:10, offset:0]    | []
    }

    void "findAllByProject with 2 schedules found"(){
        given:
            ScheduleDef scheduleDef1 = new ScheduleDef(name: 'scheduleName 1', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON')
            scheduleDef1.save()
            ScheduleDef scheduleDef2 = new ScheduleDef(name: 'scheduleName 2', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON')
            scheduleDef2.save()
        when:
            def result = service.findAllByProject('testProject')
        then:
            !result.isEmpty()
            result.size() == 2
    }

    void "findAllByProject with no schedules found"(){
        given:
            ScheduleDef scheduleDef1 = new ScheduleDef(
                    name: 'scheduleName 1',
                    crontabString: '0 5 * ? * * *',
                    project: 'testProject',
                    type:'CRON')
            scheduleDef1.save()
            ScheduleDef scheduleDef2 = new ScheduleDef(
                    name: 'scheduleName 2',
                    crontabString: '0 5 * ? * * *',
                    project: 'testProject',
                    type:'CRON')
            scheduleDef2.save()
        when:
            def result = service.findAllByProject('testProject1')
        then:
            result.isEmpty()
            result.size() == 0
    }

    void "reassociate"(){
        given:
            setupMockCalendards()
            setupBeans()
            ScheduledExecution scheduledExecution = new ScheduledExecution(
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah2',
                    adhocExecution: false,
                    name: 'aResource',
                    type: 'aType',
                    command: 'aCommand',
                    uuid: TEST_UUID1).save()
            ScheduleDef scheduleDef = new ScheduleDef(
                    name: 'scheduleName 1',
                    crontabString: '0 5 * ? * * *',
                    project: 'testProject',
                    type:'CRON').addToScheduledExecutions(scheduledExecution)
            scheduleDef.save()
        when:
            def result = service.reassociate(scheduleDef.id, jobUuidsToAssociate, jobUuidsToDeassociate)
        then:
            result == null
        where:
        jobUuidsToAssociate   | jobUuidsToDeassociate
        []                    | []
        []                    | [TEST_UUID1]
        [TEST_UUID1]          | []
    }

    void "reassociate checking scheduled execution association check"(){
        given:
            setupMockCalendards()
            setupBeans()
            ScheduledExecution scheduledExecution = new ScheduledExecution(
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah2',
                    adhocExecution: false,
                    name: 'aResource',
                    type: 'aType',
                    command: 'aCommand',
                    uuid: TEST_UUID1).save()
            ScheduledExecution scheduledExecution1 = new ScheduledExecution(
                    jobName: 'monkey2',
                    project: 'testProject',
                    description: 'blah2',
                    adhocExecution: false,
                    name: 'aResource',
                    type: 'aType',
                    command: 'aCommand',
                    uuid: TEST_UUID2).save()
            ScheduleDef scheduleDef = new ScheduleDef(
                    name: 'scheduleName 1',
                    crontabString: '0 5 * ? * * *',
                    project: 'testProject',
                    type:'CRON').addToScheduledExecutions(scheduledExecution).addToScheduledExecutions(scheduledExecution1)
            scheduleDef.save()
        when:
            service.reassociate(scheduleDef.id, jobUuidsToAssociate, jobUuidsToDeassociate)
        then:
            service.findAllByProject('testProject')[0].scheduledExecutions[0].jobName == 'monkey1'
            service.findAllByProject('testProject')[0].scheduledExecutions.size() == 2
        where:
        jobUuidsToAssociate     | jobUuidsToDeassociate   | resultExpected
        [TEST_UUID1,TEST_UUID2] | []                      | []
    }

    def "persistScheduleDef"(){
        given:
            setupMockCalendards()
            setupBeans()
            Map scheduleDefMap = [name: 'scheduleName 1',
                                  crontabString: '0 5 * ? * * *',
                                  project: 'testProject',
                                  type:'CRON']

        when:
            def result = service.persistScheduleDef(scheduleDefMap)
        then:
            result
    }

    def "delete"(){
        given:
            ScheduleDef scheduleDef = new ScheduleDef(
                    name: 'scheduleName 1',
                    crontabString: '0 5 * ? * * *',
                    project: 'testProject',
                    type:'CRON')
            scheduleDef.save()
            def scheduleMap = [id:id]
        when:
            def result = service.delete(scheduleMap)
        then:
            result.success == resultExpected
        where:
        id  | resultExpected
        1   | true
        2   | false
    }

    def "updateScheduleDef"(){
        given:
            ScheduleDef scheduleDef = new ScheduleDef(
                    name: 'scheduleName 1',
                    crontabString: '0 5 * ? * * *',
                    project: 'testProject',
                    type:'CRON')
            ScheduleDef newSD = new ScheduleDef(
                    name: 'scheduleName Changed',
                    crontabString: '0 5 * ? * * *',
                    project: 'testProject',
                    type:'CRON')
        when:
            def result = service.updateScheduleDef(scheduleDef, newSD)
        then:
            result.name == newSD.name
    }

    def "cleanRemovedScheduleDef no schedules"(){
        given:
            setupBeans()
            ScheduledExecution scheduledExecution = new ScheduledExecution(
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah2',
                    adhocExecution: false,
                    name: 'aResource',
                    type: 'aType',
                    command: 'aCommand',
                    uuid: TEST_UUID1).save()
        when:

            def result = service.cleanRemovedScheduleDef(scheduledExecution)
        then:
            1 * service.quartzScheduler.getTriggersOfJob(_)
            0 * service.quartzScheduler.unscheduleJob(_)
            null == result
    }

    def "getTriggerNamesToRemoveFromQuartz empty" (){
        given:
        setupBeans()
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1).save()
        def trigger = service.createTrigger(scheduledExecution)
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
            getTriggersOfJob(_) >> [trigger]
        }
        when:
        def result = service.getTriggerNamesToRemoveFromQuartz(scheduledExecution)
        then:
        result.isEmpty()
    }

    def "getTriggerNamesToRemoveFromQuartz not empty" (){
        given:
        setupBeans()
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1).save()
        def trigger = service.createTrigger(new ScheduledExecution(jobName: 'mnk',project: 'testProject',id: 2))
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
            getTriggersOfJob(_) >> [trigger]
        }
        when:
        def result = service.getTriggerNamesToRemoveFromQuartz(scheduledExecution)
        then:
        !result.isEmpty()
        result.size() == 1
    }

    def "handleScheduleDefinitions with own schedule"(){
        given:
        def nextTime = new Date()
        setupBeans(nextTime)
        setupMockCalendards()
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1,
                scheduled: true).save()
        when:
        def result = service.handleScheduleDefinitions(scheduledExecution)
        then:
        result
        result.scheduled == true
        result.nextTime == nextTime
    }

    def "handleScheduleDefinitions with schedule definition"(){
        given:
        def nextTime = new Date()
        setupBeans(nextTime)
        setupMockCalendards()
        ScheduleDef scheduleDef = new ScheduleDef(name: 'scheduleName', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON')
        scheduleDef.save()
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1,
                scheduled: true).addToScheduleDefinitions(scheduleDef).save()
        def trigger = service.createTrigger(scheduledExecution)
        trigger.nextFireTime = nextTime
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
            getTriggersOfJob(_) >> [trigger]
        }
        when:
        def result = service.handleScheduleDefinitions(scheduledExecution)
        then:
        1 * service.quartzScheduler.getJobDetail(_)
        1 * service.quartzScheduler.checkExists(_)
        1 * service.quartzScheduler.unscheduleJob(_)
        result
        result.scheduled == true
        result.nextTime == nextTime
    }

    def "createTrigger"(){
        given:
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1,
                scheduled: true,
                id:1)
        when:
        def result = service.createTrigger(scheduledExecution)
        then:
        result
        result.name == scheduledExecution.generateJobScheduledName()
        result.group == scheduledExecution.generateJobGroupName()

    }

    def "createJobDetail"(){
        given:
        setupBeans()
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1,
                scheduled: true,
                id:1)
        when:
        def result = service.createJobDetail(scheduledExecution)
        then:
        result
        result.name == scheduledExecution.generateJobScheduledName()
        result.group == scheduledExecution.generateJobGroupName()
    }

    def "createJobDetailMap"(){
        given:
        setupBeans()
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1,
                scheduled: true).save()
        when:
        def result = service.createJobDetailMap(scheduledExecution)
        then:
        result
        result.scheduledExecutionId == '1'
    }

    def "nextExecutionTime"(){
        given:
        def nextTime = new Date()
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1,
                scheduled: true).save()
        def trigger = service.createTrigger(scheduledExecution)
        trigger.nextFireTime = nextTime
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
            getTriggersOfJob(_) >> [trigger]
        }
        when:
        def result = service.nextExecutionTime(scheduledExecution)
        then:
        result == nextTime
    }

    def "tempNextExecutionTime"(){
        given:
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1,
                scheduled: true).save()
        when:
        def result = service.tempNextExecutionTime(scheduledExecution)
        then:
        result
    }

    def "persistScheduleDefFromMap successfully"(){
        given:
        ScheduleDef scheduleDef = new ScheduleDef(name: 'scheduleName', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON')
        when:
        def result = service.persistScheduleDefFromMap(scheduleDef.toMap(), 'testProject')
        then:
        result
        result.errors.isEmpty()
    }

    def "persistScheduleDefFromMap with errors"(){
        given:
        ScheduleDef scheduleDef = new ScheduleDef(crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON')
        when:
        def result = service.persistScheduleDefFromMap(scheduleDef.toMap(), 'testProject')
        then:
        result
        result.errors == ['Property [name] of class [class rundeck.ScheduleDef] cannot be null']
    }

    def "massiveScheduleDelete all deleted"(){
        given:
        def schedulesId = [1,2,3,4]
        ScheduleDef scheduleDef1 = new ScheduleDef(name: 'scheduleName1', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON').save()
        ScheduleDef scheduleDef2 = new ScheduleDef(name: 'scheduleName2', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON').save()
        ScheduleDef scheduleDef3 = new ScheduleDef(name: 'scheduleName3', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON').save()
        ScheduleDef scheduleDef4 = new ScheduleDef(name: 'scheduleName4', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON').save()
        when:
        def result = service.massiveScheduleDelete(schedulesId, 'testProject')
        then:
        result
        result.messages[0] == 'Total Schedule definitions to be deleted : 4'
        result.messages[1] == 'Total Schedule definitions deleted : 4'
        result.success == true
    }

    def "massiveScheduleDelete partial delete"(){
        given:
        def schedulesId = [1,2,3,4]
        ScheduleDef scheduleDef1 = new ScheduleDef(name: 'scheduleName1', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON').save()
        ScheduleDef scheduleDef2 = new ScheduleDef(name: 'scheduleName2', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON').save()
        ScheduleDef scheduleDef3 = new ScheduleDef(name: 'scheduleName3', crontabString: '0 5 * ? * * *', project: 'testProject1', type:'CRON').save()
        ScheduleDef scheduleDef4 = new ScheduleDef(name: 'scheduleName4', crontabString: '0 5 * ? * * *', project: 'testProject1', type:'CRON').save()
        when:
        def result = service.massiveScheduleDelete(schedulesId, 'testProject')
        then:
        result
        result.messages[0] == 'Total Schedule definitions to be deleted : 4'
        result.messages[1] == 'Total Schedule definitions deleted : 2'
        result.success == true
    }

    def "findJobsAssociatedToSchedule with 2 founds"(){
        given:
        def project = "testProject"
        def scheduleName = "scheduleName1"
        def paginationParams = [max: 100, offset: 0]
        ScheduleDef scheduleDef1 = new ScheduleDef(name: 'scheduleName1', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON').save()
        ScheduledExecution scheduledExecution1 = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1).addToScheduleDefinitions(scheduleDef1).save()
        ScheduledExecution scheduledExecution2 = new ScheduledExecution(
                jobName: 'monkey2',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1).addToScheduleDefinitions(scheduleDef1).save()

        when:
        def result = service.findJobsAssociatedToSchedule(project, scheduleName, paginationParams)
        then:
        result
        result.totalRecords == 2
        result.scheduledExecutions.size() == 2
    }

    def "findJobsAssociatedToSchedule without results"(){
        given:
        def project = "testProject"
        def scheduleName = "scheduleName2"
        def paginationParams = [max: 100, offset: 0]
        ScheduleDef scheduleDef1 = new ScheduleDef(name: 'scheduleName1', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON').save()
        ScheduledExecution scheduledExecution1 = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1).addToScheduleDefinitions(scheduleDef1).save()
        ScheduledExecution scheduledExecution2 = new ScheduledExecution(
                jobName: 'monkey2',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                uuid: TEST_UUID1).addToScheduleDefinitions(scheduleDef1).save()

        when:
        def result = service.findJobsAssociatedToSchedule(project, scheduleName, paginationParams)
        then:
        result
        result.totalRecords == 0
        result.scheduledExecutions.size() == 0
    }

    def "check if job has calendars"(){
        given:
        service.jobSchedulerCalendarService = Mock(JobSchedulerCalendarService){
            isCalendarEnable() >> true
        }

        setupBeans()
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                scheduled: true,
                uuid: TEST_UUID1).save()


        when:
        def result = service.hasCalendars(scheduledExecution)
        then:
        1 * service.jobSchedulerCalendarService.getQuartzCalendar(_,_) >> new JobCalendar("Test Calendar","Test Calendar")

        result != null
        result == "Test Calendar"
    }


    def "register calendar"(){
        given:
        service.jobSchedulerCalendarService = Mock(JobSchedulerCalendarService){
            isCalendarEnable() >> true
        }

        setupBeans()
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand',
                scheduled: true,
                uuid: TEST_UUID1).save()

        Date date = new Date()
        DateFormat dateFormat = new SimpleDateFormat("yyyymmddHHmm");
        String strDate = dateFormat.format(date);

        String calendarName = "Test Calendar"
        String calendarRegisterName = "Test Calendar ${strDate}"


        when:
        def result = service.handleScheduleDefinitions(scheduledExecution)
        then:
        1 * service.jobSchedulerCalendarService.getQuartzCalendar(_,_) >> new JobCalendar(calendarName, calendarRegisterName)
        1 * service.quartzScheduler.getCalendar(calendarRegisterName)>>null
        1 * service.quartzScheduler.addCalendar(_,_,_,_)
        1 * service.quartzScheduler.getCalendarNames()

        result != null
    }


}


class JobCalendar extends BaseCalendar implements JobCalendarBase{
    String name
    String registerName

    JobCalendar(String name, String registerName) {
        this.name = name
        this.registerName = registerName
    }

    @Override
    String getName() {
        return name
    }

    @Override
    String getRegisterName() {
        return registerName
    }


    @Override
    public String toString() {
        return name
    }
}
