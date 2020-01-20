package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.schedule.SchedulesManager
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.quartz.JobDetail
import org.quartz.Scheduler
import org.quartz.SimpleTrigger
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Shared
import spock.lang.Specification

/**
 * Integration tests for the ScheduledExecutionService.
 */
@Integration
@Rollback
class ScheduledExecutionServiceIntegrationSpec extends Specification {
    public static final String TEST_UUID1 = 'BB27B7BB-4F13-44B7-B64B-D2435E2DD8C7'
    public static final String TEST_UUID2 = '490966E0-2E2F-4505-823F-E2665ADC66FB'

    @Shared
    ScheduledExecutionService service = new ScheduledExecutionService()

    private Map createJobParams(Map overrides = [:]) {
        [
                jobName       : 'blue',
                project       : 'AProject',
                groupPath     : 'some/where',
                description   : 'a job',
                argString     : '-a b -c d',
                workflow      : new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec([adhocRemoteString: 'test buddy']).save(flush: true, failOnError: true)]
                ).save(flush: true, failOnError: true),
                serverNodeUUID: null,
                scheduled     : true,
                userRoleList  : ''
        ] + overrides
    }

    def "reclaiming scheduled jobs includes ad hoc scheduled"() {
        given:
        def project = 'testProject'
        service.executionServiceBean    = Mock(ExecutionService)
        service.quartzScheduler         = Mock(Scheduler)
        service.jobSchedulerService = Mock(JobSchedulerService)
        service.frameworkService        = Stub(FrameworkService) {
            existsFrameworkProject(project) >> true
            isClusterModeEnabled() >> true
            getServerUUID() >> TEST_UUID2
            getRundeckBase() >> ''
            getFrameworkProject(project) >> Mock(IRundeckProject){
                getProjectProperties()>>[:]
            }
        }
        service.fileUploadService=Mock(FileUploadService)

        String jobUuid  = UUID.randomUUID().toString()
        def workflow = new Workflow(commands: []).save(flush: true,
                                                       failOnError: true)
        def se = new ScheduledExecution(
                jobName: 'viridian',
                groupPath: 'test/group',
                uuid: jobUuid,
                serverNodeUUID: TEST_UUID1,
                project: project,
                workflow: workflow,
                scheduled: false
        ).save(flush: true, failOnError: true)

        // Create an execution with a scheduled time +1 day
        def startTime   = new Date()
        startTime       = startTime.plus(1)

        def e = new Execution(
                scheduledExecution: se,
                argString: '-test args',
                user: 'testuser',
                project: project,
                loglevel: 'WARN',
                doNodedispatch: false,
                serverNodeUUID: TEST_UUID1,
                status: 'scheduled',
                dateStarted: startTime
        ).save(flush: true, failOnError: true)

        se.executions = [e]
        se.save(flush: true, failOnError: true)
        service.jobSchedulesService = Mock(JobSchedulesService){
            getAllScheduled(_) >> [se]
        }

        when:
        def results = service.reclaimAndScheduleJobs(TEST_UUID1, true, project, [jobUuid])
//        ScheduledExecution.withSession { session ->
//            session.flush()
//            se.refresh()
//            e.refresh()
//        }


        then:
        1 * service.executionServiceBean.getExecutionsAreActive() >> true
        jobUuid in results
        // This job should have been claimed by the node
        results[jobUuid].job.jobName == 'viridian'
        results[jobUuid].success
        se.serverNodeUUID == TEST_UUID1
    }

    def "reclaiming scheduled jobs should include both ad hoc and fixed"() {
        given:
        def project = 'testProject'
        service.executionServiceBean    = Mock(ExecutionService)
        service.quartzScheduler         = Mock(Scheduler)
        service.jobSchedulerService = Mock(JobSchedulerService)
        service.frameworkService        = Stub(FrameworkService) {
            existsFrameworkProject(project) >> true
            isClusterModeEnabled() >> true
            getServerUUID() >> TEST_UUID2
            getRundeckBase() >> ''
            getFrameworkProject(project) >> Mock(IRundeckProject){
                getProjectProperties()>>[:]
            }
        }
        service.fileUploadService=Mock(FileUploadService)

        String jobUuid  = UUID.randomUUID().toString()
        String jobUuid2 = UUID.randomUUID().toString()
        def workflow = new Workflow(commands: []).save(flush: true, failOnError: true)
        def se = new ScheduledExecution(
                createJobParams(
                    jobName: 'xanadu',
                    groupPath: 'test/group',
                    uuid: jobUuid,
                    serverNodeUUID: TEST_UUID1,
                    project: project,
                    workflow: workflow,
                    scheduled: false
                )
        ).save(flush: true, failOnError: true)

        def se2 = new ScheduledExecution(
                createJobParams(
                    jobName: 'amaranth',
                    groupPath: 'test/group',
                    uuid: jobUuid2,
                    serverNodeUUID: TEST_UUID1,
                    project: project,
                    workflow: workflow,
                    user: 'skywalker',
                    userRoleList: 'jedi',
                    scheduled: true
                )
        ).save(flush: true, failOnError: true)

        def startTime   = new Date()
        startTime       = startTime.plus(1)

        def e = new Execution(
                scheduledExecution: se,
                argString: '-test args',
                user: 'testuser',
                project: project,
                loglevel: 'WARN',
                doNodedispatch: false,
                serverNodeUUID: TEST_UUID1,
                status: 'scheduled',
                dateStarted: startTime
        ).save(flush: true)

        se.executions = [e]
        se.save(flush: true, failOnError: true)
        def jobSchedulesService = new SchedulesManagerImpl()
        def schedulesMap = [(se.uuid):se, (se2.uuid): se2]
        jobSchedulesService.executionsMap = schedulesMap
        service.jobSchedulesService = jobSchedulesService

        when:
        def results = service.reclaimAndScheduleJobs(TEST_UUID1, true, project)
        ScheduledExecution.withSession { session ->
            session.flush()
            [se, se2, e]*.refresh()
        }


        then:
        1 * service.executionServiceBean.getExecutionsAreActive() >> true
        // Both jobs should've been claimed
        jobUuid in results
        jobUuid2 in results
        results[jobUuid].job.jobName == 'xanadu'
        results[jobUuid].success
        results[jobUuid2].job.jobName == 'amaranth'
        results[jobUuid2].success
        se.serverNodeUUID == TEST_UUID1
        se2.serverNodeUUID == TEST_UUID2
    }

    def "ad hoc scheduled job should be rescheduled via Quartz"() {
        given:
        def project                     = 'testProject'
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }
        service.executionServiceBean    = Mock(ExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.quartzScheduler         = Mock(Scheduler)
        service.jobSchedulerService = Mock(JobSchedulerService)
        service.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject(project) >> true
            isClusterModeEnabled() >> true
            getServerUUID() >> TEST_UUID2
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
        }

        service.jobSchedulesService = Mock(JobSchedulesService){

        }

        String jobUuid  = UUID.randomUUID().toString()
        def workflow = new Workflow(commands: []).save(failOnError: true)
        def se = new ScheduledExecution(
                createJobParams(
                    jobName: 'cerulean',
                    groupPath: 'test/group',
                    uuid: jobUuid,
                    serverNodeUUID: TEST_UUID1,
                    project: project,
                    workflow: workflow,
                    scheduled: false
                )
        ).save(failOnError: true, flush: true)

        def startTime   = new Date()
        startTime       = startTime.plus(1)

        def e = new Execution(
                scheduledExecution: se,
                argString: '-test args',
                user: 'testuser',
                project: project,
                loglevel: 'WARN',
                doNodedispatch: false,
                serverNodeUUID: TEST_UUID1,
                status: 'scheduled',
                dateStarted: startTime,
                dateCompleted: null
        ).save(flush: true)

        se.executions = [e]
        se.save(flush: true)


        when:
        def results = service.reclaimAndScheduleJobs(TEST_UUID1, true, project)
        ScheduledExecution.withSession { session ->
            session.flush()
            [se, e]*.refresh()
        }


        then:
        e != null
        e.serverNodeUUID == TEST_UUID2
        se != null
        1 * service.executionServiceBean.executionsAreActive >> true
        1 * service.jobSchedulerService.scheduleJob(_,_,_, startTime) >>  startTime

        // Both jobs should've been claimed
        jobUuid in results
        results[jobUuid].job.jobName == 'cerulean'
        results[jobUuid].success
        se.serverNodeUUID == TEST_UUID1
        e.serverNodeUUID == TEST_UUID2
    }

    def "should not be rescheduled ad hoc if executions disabled"() {
        given:
        def project                     = 'testProject'
        service.executionServiceBean    = Mock(ExecutionService)
        service.quartzScheduler         = Mock(Scheduler)
        service.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject(project) >> true
            isClusterModeEnabled() >> true
            getServerUUID() >> TEST_UUID2
            getRundeckBase() >> ''
            getFrameworkProject(project) >> Mock(IRundeckProject){
                getProjectProperties()>>[:]
            }
        }

        String jobUuid  = UUID.randomUUID().toString()
        def workflow = new Workflow(commands: []).save(failOnError: true)
        def se = new ScheduledExecution(
                jobName: 'manatee',
                groupPath: 'test/group',
                uuid: jobUuid,
                serverNodeUUID: TEST_UUID1,
                project: project,
                workflow: workflow,
                scheduled: false,
                userRoleList: ''
        ).save(failOnError: true)

        def startTime   = new Date()
        startTime       = startTime.plus(1)

        def e = new Execution(
                scheduledExecution: se,
                argString: '-test args',
                user: 'testuser',
                project: project,
                loglevel: 'WARN',
                doNodedispatch: false,
                serverNodeUUID: TEST_UUID1,
                status: 'scheduled',
                dateStarted: startTime
        ).save(failOnError: true)

        se.executions = [e]
        se.save(flush: true, failOnError: true)


        when:
        def results = service.reclaimAndScheduleJobs(TEST_UUID1, true, project)
//        ScheduledExecution.withSession { session ->
//            session.flush()
//            [se, e]*.refresh()
//        }


        then:
        1 * service.executionServiceBean.getExecutionsAreActive() >> false
        // Should not have been scheduled sa executions are not active
        0 * service.quartzScheduler.scheduleJob(_ as JobDetail, _ as SimpleTrigger)

        jobUuid in results
        results[jobUuid].job.jobName == 'manatee'
        results[jobUuid].success
        se.serverNodeUUID == TEST_UUID1
    }

    def "job with secure input options should be cleaned up"() {
        given:
        def project                     = 'testProject'
        service.executionServiceBean    = Mock(ExecutionService)
        service.quartzScheduler         = Mock(Scheduler)
        service.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject(project) >> true
            isClusterModeEnabled() >> true
            getServerUUID() >> TEST_UUID2
            getRundeckBase() >> ''
            getFrameworkProject(project) >> Mock(IRundeckProject){
                getProjectProperties()>>[:]
            }
        }

        String jobUuid  = UUID.randomUUID().toString()
        def workflow = new Workflow(commands: []).save(failOnError: true)
        def se = new ScheduledExecution(
                jobName: 'byzantium',
                groupPath: 'test/group',
                uuid: jobUuid,
                serverNodeUUID: TEST_UUID1,
                project: project,
                workflow: workflow,
                scheduled: false,
				user: 'yoda',
                userRoleList: 'jedi,master',
				options: [new Option(name: 'foo', defaultValue: 'bar', enforced: false,
							secureInput: true, secureExposed: true, required: true)]
        ).save(failOnError: true)

        def startTime   = new Date()
        startTime       = startTime.plus(2)

        def e = new Execution(
                scheduledExecution: se,
                argString: '-test args',
                user: 'testuser',
                project: project,
                loglevel: 'WARN',
                doNodedispatch: false,
                serverNodeUUID: TEST_UUID1,
                status: 'scheduled',
                dateStarted: startTime
        ).save(failOnError: true)

        se.executions = [e]
        se.save(flush: true, failOnError: true)


        when:
        def results = service.reclaimAndScheduleJobs(TEST_UUID1, true, project)
//        ScheduledExecution.withSession { session ->
//            session.flush()
//            [se, e]*.refresh()
//        }


        then:
		1 * service.executionServiceBean.cleanupRunningJobs(_ as List) >> {
			arguments ->
            List<Execution> receivedExecutions = arguments[0]
            assert receivedExecutions.size() == 1
			assert receivedExecutions.get(0).id == e.id
            assert receivedExecutions.get(0).status == 'scheduled'
            assert receivedExecutions.get(0).dateStarted.getTime() == startTime.getTime()
		}
        // Should not have been scheduled as the job has secure input options
        0 * service.quartzScheduler.scheduleJob(_ as JobDetail, _ as SimpleTrigger)

        jobUuid in results
        results[jobUuid].job.jobName == 'byzantium'
        results[jobUuid].success
        se.serverNodeUUID == TEST_UUID1
	}

    void testClaimScheduledJobsUnassigned() {
        def (ScheduledExecution job1, String serverUUID2, ScheduledExecution job2, ScheduledExecution job3,
         String serverUUID) = setupTestClaimScheduledJobs()
        ScheduledExecutionService testService = new ScheduledExecutionService()

        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        def resultMap = testService.claimScheduledJobs(serverUUID)

        assertTrue(resultMap[job1.extid].success)
        assertEquals(null, resultMap[job2.extid])
        assertEquals(null, resultMap[job3.extid])
//        ScheduledExecution.withSession {session->
//            session.flush()
//
//            job1 = ScheduledExecution.get(job1.id)
//            job1.refresh()
//            job2 = ScheduledExecution.get(job2.id)
//            job2.refresh()
//            job3 = ScheduledExecution.get(job3.id)
//            job3.refresh()
//        }


        assertEquals(serverUUID, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

    }

    void testClaimScheduledJobsFromServerUUID() {
        def (ScheduledExecution job1, String serverUUID2, ScheduledExecution job2, ScheduledExecution job3,
         String serverUUID) = setupTestClaimScheduledJobs()
        ScheduledExecutionService testService = new ScheduledExecutionService()
        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        def resultMap = testService.claimScheduledJobs(serverUUID, serverUUID2)

//        ScheduledExecution.withSession { session ->
//            session.flush()
//
//            job1 = ScheduledExecution.get(job1.id)
//            job1.refresh()
//            job2 = ScheduledExecution.get(job2.id)
//            job2.refresh()
//            job3 = ScheduledExecution.get(job3.id)
//            job3.refresh()
//        }

        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        assertEquals(null, resultMap[job1.extid])
        assertTrue(resultMap[job2.extid].success)
        assertEquals(null, resultMap[job3.extid])
    }

    def "claim all scheduled jobs"() {
        given:
        def targetserverUUID = UUID.randomUUID().toString()
        def serverUUID1 = UUID.randomUUID().toString()
        def serverUUID2 = UUID.randomUUID().toString()
        ScheduledExecution job1 = new ScheduledExecution(
                createJobParams(jobName: 'blue1', project: 'AProject', serverNodeUUID: null, uuid:UUID.randomUUID().toString())
        ).save()
        ScheduledExecution job2 = new ScheduledExecution(
                createJobParams(jobName: 'blue2', project: 'AProject2', serverNodeUUID: serverUUID1, uuid:UUID.randomUUID().toString())
        ).save()
        ScheduledExecution job3 = new ScheduledExecution(
                createJobParams(jobName: 'blue3', project: 'AProject2', serverNodeUUID: serverUUID2, uuid:UUID.randomUUID().toString())
        ).save()
        ScheduledExecution job3x = new ScheduledExecution(
                createJobParams(jobName: 'blue3', project: 'AProject2', serverNodeUUID: targetserverUUID, uuid:UUID.randomUUID().toString())
        ).save()
        ScheduledExecution job4 = new ScheduledExecution(
                createJobParams(jobName: 'blue4', project: 'AProject2', scheduled: false, uuid:UUID.randomUUID().toString())
        ).save()
        def jobs = [job1, job2, job3, job3x, job4]

        def jobSchedulesService = new SchedulesManagerImpl()
        def schedulesMap = [(job1.uuid):job1, (job2.uuid): job2, (job3.uuid): job3, (job3x.uuid): job3x, (job4.uuid): job4]
        jobSchedulesService.executionsMap = schedulesMap
        service.jobSchedulesService = jobSchedulesService

        when:
        def resultMap = service.claimScheduledJobs(targetserverUUID, null, true)

//        ScheduledExecution.withSession { session ->
//            session.flush()
//            jobs*.refresh()
//        }
        then:

        assert job1.scheduled
        assert job2.scheduled
        assert job3.scheduled
        assert job3x.scheduled

        [job1, job2, job3, job3x] == jobs.findAll { it.serverNodeUUID == targetserverUUID }
        [job1, job2, job3]*.extid == resultMap.keySet() as List
    }

    def "claim all scheduled jobs in a project"(
            String targetProject,
            String targetServerUUID,
            String serverUUID1,
            List<Map> dataList,
            List<String> resultList
    )
    {
        setup:
        def jobs = dataList.collect {
            new ScheduledExecution(createJobParams(it)).save()
        }
        def jobSchedulesService = new SchedulesManagerImpl()
        def schedulesMap = jobs.collectEntries {job -> [(job.uuid): job]}
        jobSchedulesService.executionsMap = schedulesMap
        service.jobSchedulesService = jobSchedulesService

        when:
        def resultMap = service.claimScheduledJobs(targetServerUUID, null, true, targetProject)

//        ScheduledExecution.withSession { session ->
//            session.flush()
//            jobs*.refresh()
//        }
        then:

        resultList == resultMap.keySet() as List

        where:
        targetProject | targetServerUUID |
                serverUUID1 |
                dataList |
                resultList
        'AProject'    | TEST_UUID1       |
                TEST_UUID2  |
                [[uuid: 'job3', project: 'AProject', serverNodeUUID: TEST_UUID1], [uuid: 'job1', serverNodeUUID: TEST_UUID2], [project: 'AProject2', uuid: 'job2']] |
                ['job1']
        'AProject2'   | TEST_UUID1       |
                TEST_UUID2  |
                [[uuid: 'job3', project: 'AProject2', serverNodeUUID: TEST_UUID1], [uuid: 'job1', serverNodeUUID: TEST_UUID2], [project: 'AProject2', uuid: 'job2']] |
                ['job2']
    }
}

class SchedulesManagerImpl implements SchedulesManager{

    Map<String, ScheduledExecution> executionsMap

    @Override
    boolean isSchedulesEnable() {
        return false
    }

    @Override
    Map handleScheduleDefinitions(String jobUUID, boolean isUpdate) {
        return null
    }

    @Override
    Object createTrigger(String jobName, String jobGroup, String cronExpression, int priority) {
        return null
    }

    @Override
    Object createTrigger(String jobUUID, String calendarName, String cronExpression, String triggerName) {
        return null
    }

    @Override
    Date nextExecutionTime(String jobUUID, boolean require) {
        return null
    }

    @Override
    boolean isScheduled(String uuid) {
        return executionsMap[uuid].scheduled
    }

    @Override
    List getAllScheduled(String serverUUID, String project) {
        return null
    }

    @Override
    boolean shouldScheduleExecution(String uuid) {
        return false
    }

    @Override
    void persistSchedulesToJob(String uuid, List schedules, Boolean shouldSchedule, String project) {

    }

    @Override
    List getJobSchedules(String uuid, String project) {
        return null
    }

    @Override
    List getSchedulesJobToClaim(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter, List<String> jobids) {
        return null
    }

    @Override
    List<Date> nextExecutions(String jobUuid, Date to, boolean past) {
        return null
    }
}

