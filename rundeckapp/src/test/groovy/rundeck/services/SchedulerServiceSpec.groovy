package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.quartz.ListenerManager
import org.quartz.Scheduler
import rundeck.ScheduleDef
import rundeck.ScheduledExecution
import spock.lang.Specification


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

    def setupBeans(){
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
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
            ScheduleDef schedulDef = new ScheduleDef(name: 'scheduleName', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON')
            schedulDef.save()
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
            ScheduleDef schedulDef = new ScheduleDef(name: 'scheduleName', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON')
            schedulDef.save()
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
            ScheduleDef schedulDef1 = new ScheduleDef(name: 'scheduleName 1', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON')
            schedulDef1.save()
            ScheduleDef schedulDef2 = new ScheduleDef(name: 'scheduleName 2', crontabString: '0 5 * ? * * *', project: 'testProject', type:'CRON')
            schedulDef2.save()
        when:
            def result = service.findAllByProject('testProject')
        then:
            !result.isEmpty()
            result.size() == 2
    }

    void "findAllByProject with no schedules found"(){
        given:
            ScheduleDef schedulDef1 = new ScheduleDef(
                    name: 'scheduleName 1',
                    crontabString: '0 5 * ? * * *',
                    project: 'testProject',
                    type:'CRON')
            schedulDef1.save()
            ScheduleDef schedulDef2 = new ScheduleDef(
                    name: 'scheduleName 2',
                    crontabString: '0 5 * ? * * *',
                    project: 'testProject',
                    type:'CRON')
            schedulDef2.save()
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
            ScheduleDef schedulDef = new ScheduleDef(
                    name: 'scheduleName 1',
                    crontabString: '0 5 * ? * * *',
                    project: 'testProject',
                    type:'CRON').addToScheduledExecutions(scheduledExecution)
            schedulDef.save()
        when:
            def result = service.reassociate(schedulDef.id, jobUuidsToAssociate, jobUuidsToDeassociate)
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
            ScheduleDef schedulDef = new ScheduleDef(
                    name: 'scheduleName 1',
                    crontabString: '0 5 * ? * * *',
                    project: 'testProject',
                    type:'CRON').addToScheduledExecutions(scheduledExecution).addToScheduledExecutions(scheduledExecution1)
            schedulDef.save()
        when:
            service.reassociate(schedulDef.id, jobUuidsToAssociate, jobUuidsToDeassociate)
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
}
