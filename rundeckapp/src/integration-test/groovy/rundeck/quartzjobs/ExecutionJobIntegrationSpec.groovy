package rundeck.quartzjobs

import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import com.dtolabs.rundeck.core.schedule.JobScheduleManager

import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import rundeck.*
import rundeck.services.ExecutionService
import rundeck.services.ExecutionUtilService
import rundeck.services.FrameworkService
import rundeck.services.JobSchedulerService
import rundeck.services.JobSchedulesService
import rundeck.services.execution.ThresholdValue
import spock.lang.Unroll

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

@Integration
@Rollback
class ExecutionJobIntegrationSpec extends Specification {

    /**
     * executeAsyncBegin succeeds,finish succeeds, thread fails
     */
    def testExecuteCommandStartOkFinishOkThreadFails() {
        given:
            ScheduledExecution se = setupJob()
            Execution execution = setupExecution(se, new Date(), new Date())
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)
            def mockeus = Mock(ExecutionUtilService)

            WorkflowExecutionServiceThread stb = new WorkflowExecutionServiceThread(null, null, null, null, null)
            stb.result = new TestWFEResult(success: false)

            def testExecmap = new ExecutionService.AsyncStarted(thread: stb)

            job.finalizeRetryMax = 1
            job.finalizeRetryDelay = 0

        when:
            def result = job.executeCommand(
                new ExecutionJob.RunContext(
                    executionService: mockes,
                    executionUtilService: mockeus,
                    execution: execution,
                    )
            )
        then:
            1 * mockes.executeAsyncBegin(_, _, execution, _, _, _) >> testExecmap
            1 * mockeus.finishExecution(testExecmap)
            !result.success
            testExecmap == result.execmap

    }


    @Unroll
    def "saveState With JobStats Failure Retry #retryMax"() {
        given:
            def job = new ExecutionJob()
            job.finalizeRetryMax = 1
            job.finalizeRetryDelay = 0
            def scheduledExecution = setupJob()
            def execution = setupExecution(scheduledExecution, new Date(), null)
            def mockes = Mock(ExecutionService)
            def execMap = new ExecutionService.AsyncStarted()

            boolean saveStateCalled = false
            1 * mockes.saveExecutionState(
                scheduledExecution.id, execution.id, {
                it.status == 'succeeded'
                it.cancelled == false
                it.failedNodes == null
                it.failedNodesMap == null
            }, _, _
            ) >> {
                saveStateCalled = true
            }
            def saveStatsComplete = false
            def fail3times = throwXTimes(3)
            retryMax * mockes.updateScheduledExecStatistics(scheduledExecution.id, execution.id, { it > 0 }) >> {
                fail3times()
                saveStatsComplete = true
            }

            job.statsRetryMax = retryMax
        when:
            def result = job.
                saveState(
                    null,
                    mockes,
                    execution,
                    true,
                    false,
                    false,
                    false,
                    null,
                    scheduledExecution.id,
                    null,
                    execMap
                )
        then:
            saveStatsComplete == succeeded
            saveStateCalled
        where:
            retryMax | succeeded
            2        | false
            4        | true
    }

    /**
     * executeAsyncBegin succeeds,threshold is not met
     */
    def testExecuteCommandThresholdNotMet() {
        given:
            ScheduledExecution se = setupJob()
            Execution execution = setupExecution(se, new Date(), new Date())
            Assert.assertNotNull(execution)
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)
            def mockeus = Mock(ExecutionUtilService)
            WorkflowExecutionServiceThread stb = new TestWEServiceThread(null, null, null, null, null)
            stb.successful = true
            def threshold = new testThreshold()
            def testExecmap = new ExecutionService.AsyncStarted(thread: stb, threshold: threshold)
            1 * mockes.executeAsyncBegin(_, _, execution, _, _, _,) >> testExecmap

            1 * mockeus.finishExecution(testExecmap)
        when:

            def result = job.executeCommand(
                new ExecutionJob.RunContext(
                    executionService: mockes,
                    executionUtilService: mockeus,
                    execution: execution,
                    )
            )
        then:
            result.success
            result.execmap == testExecmap
            !job.wasThreshold
    }

    def testSaveStateWithJob() {
        given:
            def job = new ExecutionJob()
            job.finalizeRetryMax = 1
            job.finalizeRetryDelay = 0
            def scheduledExecution = setupJob()
            def execution = setupExecution(scheduledExecution, new Date(), null)
            def mockes = Mock(ExecutionService)

            def expectresult = [
                status        : 'succeeded',
                cancelled     : false,
                failedNodes   : null,
                failedNodesMap: null,
            ]
            def execMap = new ExecutionService.AsyncStarted()

        when:
            def result = job.
                saveState(
                    null,
                    mockes,
                    execution,
                    true,
                    false,
                    false,
                    false,
                    null,
                    scheduledExecution.id,
                    null,
                    execMap
                )
        then:
            1 * mockes.saveExecutionState(
                scheduledExecution.id, execution.id, {
                it.subMap(expectresult.keySet()) == expectresult
            }, _, _
            ) >> true

            1 * mockes.updateScheduledExecStatistics(scheduledExecution.id, execution.id, { it > 0 }) >> true
    }

    def testSaveStateWithFailureNoJob() {
        given:
            def job = new ExecutionJob()
            job.finalizeRetryMax = 2
            job.finalizeRetryDelay = 0
            def scheduledExecution = setupJob()
            def execution = setupExecution(scheduledExecution, new Date(), null)
            def mockes = Mock(ExecutionService)

            def expectresult = [
                status        : 'succeeded',
                cancelled     : false,
                failedNodes   : null,
                failedNodesMap: null,
            ]
            def execMap = new ExecutionService.AsyncStarted()
            def fail3times = throwXTimes(3)

            2 * mockes.saveExecutionState(
                scheduledExecution.id, execution.id, {
                it.subMap(expectresult.keySet()) == expectresult
            }, _, _
            ) >> {
                fail3times.call()
            }

        when:
            def result = job.
                saveState(
                    null,
                    mockes,
                    execution,
                    true,
                    false,
                    false,
                    false,
                    null,
                    scheduledExecution.id,
                    null,
                    execMap
                )
        then:
            !result

            1 * mockes.updateScheduledExecStatistics(scheduledExecution.id, execution.id, { it > 0 }) >> true
    }


    /**
     * executeAsyncBegin succeeds,finish succeeds, thread succeeds, calls avgDurationExceeded
     */
    def testExecuteCommandNotification() {
        given:
            ScheduledExecution se = setupJob()
            Notification notif = new Notification()
            notif.eventTrigger = "onavgduration"
            notif.type = "email"
            notif.content = '{"recipients":"test@test","subject":"test"}'
            notif.save()
            se.addToNotifications(notif)
            se.totalTime = 100
            se.execCount = 10
            se.save()
            Execution execution = setupExecution(se, new Date(), new Date())
            Assert.assertNotNull(execution)
            ExecutionJob job = new ExecutionJob()
            def es = Mock(ExecutionService)
            def eus = Mock(ExecutionUtilService)
            WorkflowExecutionServiceThread stb = new TestWEServiceThread(null, null, null, null, null)
            stb.successful = true
            stb.result = new TestWFEResult(success: true)
            def testExecmap = new ExecutionService.AsyncStarted(thread: stb, scheduledExecution: se)

            1 * es.executeAsyncBegin(_, _, execution, _, _, _,) >> {
                stb.start()
                testExecmap
            }
            _ * es.isApplicationShutdown()


            1 * eus.finishExecution(testExecmap)
            job.finalizeRetryMax = 1
            job.finalizeRetryDelay = 0

        when:
            def result = job.executeCommand(
                new ExecutionJob.RunContext(
                    executionService: es,
                    executionUtilService: eus,
                    execution: execution,
                    scheduledExecution: se
                )
            )
        then:
            result.success
            testExecmap == result.execmap
            1 * es.avgDurationExceeded(_, _)
    }

    static class TestThreshold implements ThresholdValue<Long> {
        String action
        boolean thresholdExceeded
        Long value
        String description
    }

    def testSaveStateThresholdCustomStatus() {
        given:
            def job = new ExecutionJob()
            job.finalizeRetryMax = 1
            job.finalizeRetryDelay = 0
            def execution = setupExecution(null, new Date(), new Date())
            def mockes = Mock(ExecutionService)

            def expectresult = [
                status        : 'custom',
                cancelled     : false,
                failedNodes   : null,
                failedNodesMap: null,
            ]
            def execMap = new ExecutionService.AsyncStarted(
                threshold: new TestThreshold(action: 'halt')
            )


            job.wasThreshold = true
            def initMap = new ExecutionJob.RunContext([scheduledExecution: [logOutputThresholdStatus: 'custom']])

        when:
            job.saveState(null, mockes, execution, true, false, false, true, null, -1, initMap, execMap)
        then:
            1 * mockes.saveExecutionState(
                null, execution.id, {
                it.subMap(expectresult.keySet()) == expectresult
            }, execMap, _
            )
    }

    def testWithRetrySuccessful() {
        given:
            def job = new ExecutionJob()
        when:
            ExecutionJob.Retried val = job.withRetry(1, 1, "test1", successClos)
        then:
            val.complete
            val.caught == null
    }

    def testWithRetryException() {
        given:
            def job = new ExecutionJob()
        when:
            ExecutionJob.Retried val = job.withRetry(2, 1, "test1", alwaysThrowClos)
        then:
            !val.complete
            val.caught != null
            val.caught.message == "test failure"
    }

    def testWithRetryFailure() {
        given:
            def job = new ExecutionJob()
        when:
            ExecutionJob.Retried val = job.withRetry(2, 1, "test1", null, failedClos)
        then:
            !val.complete
            val.caught == null
    }

    def testWithRetryXTimesWithException() {
        given:
            def job = new ExecutionJob()
        when:
            ExecutionJob.Retried val = job.withRetry(3, 1, "test1", throwXTimes(3))
        then:
            !val.complete
            val.caught != null
            val.caught.message == "test failure number 3"
    }

    def testWithRetryXTimesWithFailure() {
        given:
            def job = new ExecutionJob()
        when:
            ExecutionJob.Retried val = job.withRetry(3, 1, "test1", null, failXTimes(3))
        then:
            !val.complete
            val.caught == null
    }

    def testWithRetryXTimesWithSuccessAfterException() {
        given:
            def job = new ExecutionJob()
        when:
            ExecutionJob.Retried val = job.withRetry(3, 1, "test1", throwXTimes(2))
        then:
            val.complete
            val.caught == null
    }

    def testWithRetryXTimesWithSuccessAfterFailure() {
        given:
            def job = new ExecutionJob()
        when:
            ExecutionJob.Retried val = job.withRetry(3, 1, "test1", failXTimes(2))
        then:
            val.complete
            val.caught == null
    }


    def testInitializeEmpty() {
        given:
            ExecutionJob job = new ExecutionJob()
            def contextMock = new JobDataMap()
        when:
            job.initialize(null, contextMock)
        then:
            RuntimeException e = thrown()
            e.message.contains("scheduledExecutionId could not be retrieved from JobDataMap")

    }

    def testInitializeNotFoundJob() {
        given:
            ExecutionJob job = new ExecutionJob()
            def contextMock = new JobDataMap([scheduledExecutionId: '1'])

        when:

            job.initialize(null, contextMock)
        then:
            RuntimeException e = thrown()
            e.message.contains("failed to lookup scheduledException object from job data map")

    }

    /**
     * Initialize for an execution specified via job ID
     */
    def testInitializeJobExecution() {
        given:
            ScheduledExecution se = setupJob()
            se.user = 'test'
            se.userRoleList = 'a,b'
            se.save()
            Execution e = new Execution(
                project: "AProject",
                user: 'bob',
                dateStarted: new Date(),
                dateCompleted: new Date(),

                workflow: new Workflow(
                    keepgoing: true,
                    commands: [new CommandExec(
                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                    )]
                )
            ).save()
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)
            def mockeus = Mock(ExecutionUtilService)
            def mockfs = Mock(FrameworkService)
            def authProvider = Mock(AuthContextProvider)
            def jobSchedulesServiceMock = Mock(JobSchedulesService)
            def jobSchedulerServiceMock = Mock(JobSchedulerService)
            1 * mockes.selectSecureOptionInput(se, _, true) >> [test: 'input']
            1 * mockes.createExecution(se, { it.username == se.user }, _, { it.executionType == 'scheduled' }) >> e

            def proj = Mock(IRundeckProject) {
                2 * getProjectProperties() >> [:]
            }
            1 * mockfs.getFrameworkProject(_) >> proj
            IFramework fwk = Mock(IFramework)
            1 * mockfs.getRundeckFramework() >> fwk
            def mockAuth = Mock(UserAndRolesAuthContext) {
                getUsername() >> 'test'

            }

            1 * authProvider.getAuthContextForUserAndRolesAndProject(_, _, _) >> mockAuth

            def contextMock = new JobDataMap(
                scheduledExecutionId: se.id.toString(),
                frameworkService: mockfs,
                executionService: mockes,
                executionUtilService: mockeus,
                authContext: mockAuth,
                jobSchedulesService: jobSchedulesServiceMock,
                jobSchedulerService: jobSchedulerServiceMock,
                authContextProvider: authProvider
            )
        when:
            def result = job.initialize(null, contextMock)

        then:
        result.scheduledExecutionId == se.id
        result.scheduledExecution.id == se.id
        result.executionService == mockes
        result.executionUtilService == mockeus
        result.secureOptsExposed == [test: 'input']
        result.framework == fwk
        result.execution == e

    }

    def "testInitializeWithoutExecutionUtilService"() {
        given:
            ScheduledExecution se = setupJob()
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)

            def contextMock = new JobDataMap(scheduledExecutionId: se.id.toString(), executionService: mockes)

        when:
            job.initialize(null, contextMock)
        then:
            RuntimeException e = thrown()
            e.message.contains("executionUtilService could not be retrieved")

    }

    def "testInitializeWithoutExecutionService"() {
        given:
            ScheduledExecution se = setupJob()
            ExecutionJob job = new ExecutionJob()

            def contextMock = new JobDataMap(scheduledExecutionId: se.id.toString())

        when:
            job.initialize(null, contextMock)
        then:
            RuntimeException e = thrown()
            e.message.contains("executionService could not be retrieved")

    }

    /**
     * Job timeout determined by ScheduledExecution option value
     */
    @Unroll
    def "initialize job timeout #jobTimeout"() {
        given:
            ScheduledExecution se = setupJob()
            se.user = 'test'
            se.userRoleList = 'a,b'
            se.timeout = jobTimeout
            se.save()

            Execution e = setupExecution(se, new Date(), null)
            e.argString = args
            e.save(flush: true)
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)
            def mockeus = Mock(ExecutionUtilService)
            def mockfs = Mock(FrameworkService)
            def jobSchedulesServiceMock = Mock(JobSchedulesService)
            def jobSchedulerServiceMock = Mock(JobSchedulerService)

            IFramework fwk = Mock(IFramework)
            1 * mockfs.getRundeckFramework() >> fwk
            def mockAuth = Mock(UserAndRolesAuthContext) {
                getUsername() >> 'test'
            }
            def authProvider = Mock(AuthContextProvider)

            def contextMock = new JobDataMap(
                timeout: '123',
                scheduledExecutionId: se.id.toString(),
                executionId: e.id.toString(),
                frameworkService: mockfs,
                executionService: mockes,
                executionUtilService: mockeus,
                authContext: mockAuth,
                jobSchedulesService: jobSchedulesServiceMock,
                jobSchedulerService: jobSchedulerServiceMock,
                authContextProvider: authProvider,
                secureOpts: [:],
                secureOptsExposed: [:],
                )
        when:
            def result = job.initialize(null, contextMock)
        then:
            result.timeout == expected
            result.execution == e
        where:
            jobTimeout          | args         | expected
            '${option.timeout}' | '-timeout 2' | 2
            '60m'               | ''           | 3600L
    }

    /**
     * executeAsyncBegin fails to start, result is success=false
     */
    def testExecuteCommandStartFailed() {
        given:
            ScheduledExecution se = setupJob()
            Execution execution = setupExecution(se, new Date(), new Date())
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)
            def mockeus = Mock(ExecutionUtilService)
            1 * mockes.executeAsyncBegin(*_) >> null//fail to start

        when:
            def result = job.executeCommand(
                new ExecutionJob.RunContext(
                    executionService: mockes,
                    executionUtilService: mockeus,
                    execution: execution,
                    scheduledExecution: se
                )
            )
        then:
            !result.success
    }

    def "execute beforeExecution skip value preempts"() {
        given:
            ScheduledExecution se = setupJob()
            se.user = 'test'
            se.userRoleList = 'a,b'
            se.save()
            Execution e = new Execution(
                project: "AProject",
                user: 'bob',
                dateStarted: new Date(),
                dateCompleted: new Date(),

                workflow: new Workflow(
                    keepgoing: true,
                    commands: [new CommandExec(
                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                    )]
                )
            ).save()
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)
            def mockeus = Mock(ExecutionUtilService)
            def mockfs = Mock(FrameworkService)
            def jobSchedulesServiceMock = Mock(JobSchedulesService)
            def jobSchedulerServiceMock = Mock(JobSchedulerService)
            1 * mockes.selectSecureOptionInput(se, _, true) >> [test: 'input']
            1 * mockes.createExecution(se, { it.username == se.user }, _, { it.executionType == 'scheduled' }) >> e

            def proj = Mock(IRundeckProject) {
                2 * getProjectProperties() >> [:]
            }
            1 * mockfs.getFrameworkProject(_) >> proj
            IFramework fwk = Mock(IFramework)
            1 * mockfs.getRundeckFramework() >> fwk
            def mockAuth = Mock(UserAndRolesAuthContext) {
                getUsername() >> 'test'
            }

            def authProvider = Mock(AuthContextProvider)
            1 * authProvider.getAuthContextForUserAndRolesAndProject(_, _, _) >> mockAuth

            def contextMock = new JobDataMap(
                scheduledExecutionId: se.id.toString(),
                frameworkService: mockfs,
                executionService: mockes,
                executionUtilService: mockeus,
                authContext: mockAuth,
                jobSchedulesService: jobSchedulesServiceMock,
                jobSchedulerService: jobSchedulerServiceMock,
                authContextProvider: authProvider,
            )
            def qjobContext = Mock(JobExecutionContext){
                getJobDetail()>>Mock(JobDetail){
                    getJobDataMap()>>contextMock
                }
            }
        when:
            job.execute(qjobContext)
        then:
            1 * jobSchedulerServiceMock.beforeExecution(_, _, _) >> JobScheduleManager.BeforeExecutionBehavior.skip
            0 * mockes.executeAsyncBegin(*_)
            0 * mockes.saveExecutionState(*_)
            0 * jobSchedulerServiceMock.afterExecution(_,_,_)
    }
    def "execute beforeExecution proceed value continues"() {
        given:
            ScheduledExecution se = setupJob()
            se.user = 'test'
            se.userRoleList = 'a,b'
            se.save()
            Execution e = new Execution(
                project: "AProject",
                user: 'bob',
                dateStarted: new Date(),
                dateCompleted: new Date(),

                workflow: new Workflow(
                    keepgoing: true,
                    commands: [new CommandExec(
                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                    )]
                )
            ).save()
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)
            def mockeus = Mock(ExecutionUtilService)
            def mockfs = Mock(FrameworkService)
            def jobSchedulesServiceMock = Mock(JobSchedulesService)
            def jobSchedulerServiceMock = Mock(JobSchedulerService)
            1 * mockes.selectSecureOptionInput(se, _, true) >> [test: 'input']
            1 * mockes.createExecution(se, { it.username == se.user }, _, { it.executionType == 'scheduled' }) >> e

            def proj = Mock(IRundeckProject) {
                2 * getProjectProperties() >> [:]
            }
            1 * mockfs.getFrameworkProject(_) >> proj
            IFramework fwk = Mock(IFramework)
            1 * mockfs.getRundeckFramework() >> fwk
            def mockAuth = Mock(UserAndRolesAuthContext) {
                getUsername() >> 'test'
            }

            def authProvider = Mock(AuthContextProvider)
            1 * authProvider.getAuthContextForUserAndRolesAndProject(_, _, _) >> mockAuth

            def contextMock = new JobDataMap(
                scheduledExecutionId: se.id.toString(),
                frameworkService: mockfs,
                executionService: mockes,
                executionUtilService: mockeus,
                authContext: mockAuth,
                jobSchedulesService: jobSchedulesServiceMock,
                jobSchedulerService: jobSchedulerServiceMock,
                authContextProvider: authProvider,
            )
            def qjobContext = Mock(JobExecutionContext){
                getJobDetail()>>Mock(JobDetail){
                    getJobDataMap()>>contextMock
                }
            }
            WorkflowExecutionServiceThread stb = new TestWEServiceThread(null, null, null, null, null)
            stb.successful = true
        when:
            job.execute(qjobContext)
        then:
            1 * jobSchedulerServiceMock.beforeExecution(_, _, _) >> JobScheduleManager.BeforeExecutionBehavior.proceed
            1 * mockes.executeAsyncBegin(*_)>>new ExecutionService.AsyncStarted(
                [thread: stb, scheduledExecution: se])
            1 * mockes.saveExecutionState(*_)
            1 * jobSchedulerServiceMock.afterExecution(_,_, _)
    }

    /**
     * executeAsyncBegin succeeds,threshold is  met, action 'fail'
     */
    def testExecuteCommandThresholdWasMetActionHalt() {
        given:
            ScheduledExecution se = setupJob()
            Execution execution = setupExecution(se, new Date(), new Date())
            Assert.assertNotNull(execution)
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)
            def mockeus = Mock(ExecutionUtilService)
            WorkflowExecutionServiceThread stb = new TestWEServiceThread(null, null, null, null, null)
            stb.successful = true
            def threshold = new testThreshold(wasMet: true, action: 'halt')
            def testExecmap = new ExecutionService.AsyncStarted(
                [thread: stb, threshold: threshold, scheduledExecution: se]
            )
            1 * mockes.executeAsyncBegin(_, _, _, _, _, _) >> {
                stb.start()
                testExecmap
            }

            1 * mockeus.finishExecution(testExecmap)

        when:
            def result = job.executeCommand(
                new ExecutionJob.RunContext(
                    executionService: mockes,
                    executionUtilService: mockeus,
                    execution: execution,
                    scheduledExecution: se
                )
            )
        then:
            !result.success
            job.wasThreshold
    }


    /**
     * executeAsyncBegin succeeds,finish succeeds, thread succeeds
     */
    void testExecuteCommandStartOkFinishOkThreadSuccessful() {
        given:
            ScheduledExecution se = setupJob()
            Execution execution = setupExecution(se, new Date(), new Date())
            Assert.assertNotNull(execution)
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)
            def mockeus = Mock(ExecutionUtilService)
            WorkflowExecutionServiceThread stb = new TestWEServiceThread(null, null, null, null, null)
            stb.successful = true
            stb.result = new TestWFEResult(success: true)
            def testExecmap = new ExecutionService.AsyncStarted([thread: stb, scheduledExecution: se])
            1 * mockes.executeAsyncBegin(*_) >> testExecmap

            1 * mockeus.finishExecution(testExecmap)
            job.finalizeRetryMax = 1
            job.finalizeRetryDelay = 0

        when:
            def result = job.executeCommand(
                new ExecutionJob.RunContext(
                    executionService: mockes,
                    executionUtilService: mockeus,
                    execution: execution,
                    scheduledExecution: se
                )
            )
        then:
            result.success
            result.execmap == testExecmap
    }

    /**
     * executeAsyncBegin succeeds,finish fails,  retry does  succeed
     */
    def testExecuteCommandStartOkFinishRetryWithSuccess() {
        given:
            ScheduledExecution se = setupJob()
            Execution execution = setupExecution(se, new Date(), new Date())
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)
            def mockeus = Mock(ExecutionUtilService)
            WorkflowExecutionServiceThread stb = new WorkflowExecutionServiceThread(null, null, null, null, null)
            stb.result = new TestWFEResult(success: false)
            def testExecmap = new ExecutionService.AsyncStarted([thread: stb, scheduledExecution: se])
            1 * mockes.executeAsyncBegin(*_) >> testExecmap
            def count = 3
            4 * mockeus.finishExecution(testExecmap) >> {
                if (count > 0) {
                    count--
                    throw new Exception("expected failure")
                }
            }
            job.finalizeRetryDelay = 10
            job.finalizeRetryMax = 4
        when:
            def result = job.executeCommand(
                new ExecutionJob.RunContext(
                    executionService: mockes,
                    executionUtilService: mockeus,
                    execution: execution,
                    scheduledExecution: se
                )
            )
        then:
            !result.success
            result.execmap == testExecmap
    }
    /**
     * executeAsyncBegin succeeds,finish fails,  retry does  succeed
     */
    def testExecuteCommandStartOkFinishRetryWithoutSuccess() {
        given:
            ScheduledExecution se = setupJob()
            Execution execution = setupExecution(se, new Date(), new Date())
            ExecutionJob job = new ExecutionJob()
            def mockes = Mock(ExecutionService)
            def mockeus = Mock(ExecutionUtilService)
            WorkflowExecutionServiceThread stb = new WorkflowExecutionServiceThread(null, null, null, null, null)
            stb.result = new TestWFEResult(success: false)
            def testExecmap = new ExecutionService.AsyncStarted([thread: stb, scheduledExecution: se])
            1 * mockes.executeAsyncBegin(*_) >> testExecmap
            3 * mockeus.finishExecution(testExecmap) >> {
                throw new Exception("expected failure")
            }
            job.finalizeRetryDelay = 10
            job.finalizeRetryMax = 3
        when:
            def result = job.executeCommand(
                new ExecutionJob.RunContext(
                    executionService: mockes,
                    executionUtilService: mockeus,
                    execution: execution,
                    scheduledExecution: se
                )
            )
        then:
            RuntimeException e = thrown()
            e.message.contains("failed")
    }

    def testSaveStateNoJob() {
        given:
            def job = new ExecutionJob()
            job.finalizeRetryMax = 1
            job.finalizeRetryDelay = 0
            def execution = setupExecution(null, new Date(), new Date())
            def mockes = Mock(ExecutionService)

            def expectresult = [
                status        : 'succeeded',
                cancelled     : false,
                failedNodes   : null,
                failedNodesMap: null,
            ]
            def execMap = new ExecutionService.AsyncStarted()

        when:
            job.saveState(null, mockes, execution, true, false, false, true, null, -1, null, execMap)
        then:

            1 * mockes.saveExecutionState(
                null, execution.id, {
                it.subMap(expectresult.keySet()) == expectresult
            }, execMap, _
            )
    }


    /**
     * Closure always succeeds
     */
    def successClos = {
        true
    }
    /**
     * Closure always throws exception
     */
    def alwaysThrowClos = {
        throw new Exception("test failure")
    }
    /**
     * Always return false
     */
    def failedClos = {
        false
    }

    /**
     * Return a closure that throws an exception the first X times it is called
     */
    def failXTimes(int max) {
        int count = 0
        return {
            if (max > count) {
                count++
                return false
            }
            return true
        }
    }


    def throwXTimes(int max) {
        int count = 0
        return {
            if (max > count) {
                count++
                throw new Exception("test failure number ${count}")
            }
            true
        }
    }

    ScheduledExecution setupJob() {
        def se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec(
                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                )]
            ),
            options: [
                new Option(
                    name: 'env',
                    required: true,
                    enforced: false
                )
            ],
            notifyAvgDurationThreshold: '0s',
            averageDuration: 1,
            totalTime: 1,
            execCount: 1
        )
        se.save(
            flush: true,
            failOnError: true
        )
        se
    }

    Execution setupExecution(ScheduledExecution se, Date startDate, Date finishDate) {
        Execution e = new Execution(
            project: "AProject",
            user: 'bob',
            dateStarted: startDate,
            dateCompleted: finishDate,
            scheduledExecution: se,
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec(
                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                )]
            )
        )
        se?.addToExecutions(e)
        e.save(
            flush: true,
            failOnError: true
        )
        return e
    }
}
