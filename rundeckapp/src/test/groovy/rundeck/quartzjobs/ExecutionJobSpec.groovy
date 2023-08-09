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

package rundeck.quartzjobs

import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import grails.testing.gorm.DataTest
import org.quartz.*
import org.rundeck.app.data.providers.GormJobStatsDataProvider
import rundeck.*
import rundeck.services.ExecutionService
import rundeck.services.ExecutionUtilService
import rundeck.services.FrameworkService
import rundeck.services.JobSchedulerService
import rundeck.services.JobSchedulesService
import spock.lang.Specification

import java.sql.Timestamp
import java.util.concurrent.CountDownLatch

/**
 * Created by greg on 4/12/16.
 */
class ExecutionJobSpec extends Specification implements DataTest {

    def setupSpec() { mockDomains ScheduledExecution, Workflow, CommandExec, Execution,ScheduledExecutionStats }

    def "execute missing job"() {
        given:
        def datamap = new JobDataMap([scheduledExecutionId: '123'])
        ExecutionJob job = new ExecutionJob()
        def context = Mock(JobExecutionContext) {
            getJobDetail() >> Mock(JobDetail) {
                getJobDataMap() >> datamap
            }
        }

        when:
        job.execute(context)

        then:
        RuntimeException e = thrown()
        e.message == 'failed to lookup scheduledException object from job data map: id: 123'
    }

    def "execute retrieves execution id"() {
        given:
            ScheduledExecution se = createJob()
            Execution e = createExecution(se)
            ExecutionService es = Mock(ExecutionService)
            ExecutionUtilService eus = Mock(ExecutionUtilService)
            FrameworkService fwk = Mock(FrameworkService)
            JobSchedulesService jobSchedulesService = Mock(JobSchedulesService)
            JobSchedulerService jobSchedulerService = Mock(JobSchedulerService)
            AuthContextProvider authContextProvider = Mock(AuthContextProvider)
            def datamap = new JobDataMap([
                project: se.project,
                executionId: e.id.toString(),
                scheduledExecutionId: se.uuid,
                executionService:es,
                executionUtilService: eus,
                frameworkService: fwk,
                jobSchedulerService: jobSchedulerService,
                jobSchedulesService: jobSchedulesService,
                authContext:Mock(UserAndRolesAuthContext),
                authContextProvider:authContextProvider
            ])
            ExecutionJob job = new ExecutionJob()
            def context = Mock(JobExecutionContext) {
                getJobDetail() >> Mock(JobDetail) {
                    getJobDataMap() >> datamap
                }
            }

        when:
            job.execute(context)

        then:
            1 * jobSchedulerService.beforeExecution(_, _, _) >> JobScheduleManager.BeforeExecutionBehavior.skip
            job.executionId == e.id
    }
    def "execute catches exception during executeAsyncBegin"() {
        given:
            ScheduledExecution se = createJob()
            Execution e = createExecution(se)
            ExecutionService es = Mock(ExecutionService)
            ExecutionUtilService eus = Mock(ExecutionUtilService)
            FrameworkService fwk = Mock(FrameworkService)
            JobSchedulesService jobSchedulesService = Mock(JobSchedulesService)
            JobSchedulerService jobSchedulerService = Mock(JobSchedulerService)
            AuthContextProvider authContextProvider = Mock(AuthContextProvider)
            def datamap = new JobDataMap([
                project: se.project,
                executionId: e.id.toString(),
                scheduledExecutionId: se.uuid,
                executionService:es,
                executionUtilService: eus,
                frameworkService: fwk,
                jobSchedulerService: jobSchedulerService,
                jobSchedulesService: jobSchedulesService,
                authContext:Mock(UserAndRolesAuthContext),
                authContextProvider:authContextProvider
            ])
            ExecutionJob job = new ExecutionJob()
            def context = Mock(JobExecutionContext) {
                getJobDetail() >> Mock(JobDetail) {
                    getJobDataMap() >> datamap
                }
            }
            1 * es.executeAsyncBegin(*_)>>{
                throw new Exception("failed to start execution")
            }

        when:
            job.execute(context)

        then:
            1 * jobSchedulerService.beforeExecution(_, _, _) >> JobScheduleManager.BeforeExecutionBehavior.proceed
            1 * jobSchedulerService.afterExecution(_, _, _)
            1 * es.saveExecutionState(se.uuid, e.id, { it.status == 'failed' }, null, !null)
            job.executionId == e.id
    }

    public Execution createExecution(ScheduledExecution se) {
        new Execution(
            scheduledExecution: se,
            dateStarted: new Date(),
            dateCompleted: null,
            project: se.project,
            user: 'bob',
            workflow: new Workflow(
                commands: [new CommandExec(
                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                )]
            )
        ).save(flush: true)
    }

    ScheduledExecution createJob(){
        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            uuid: UUID.randomUUID().toString(),
            serverNodeUUID: UUID.randomUUID().toString(),
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec(
                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                )]
            ),
            scheduled: true,
            executionEnabled: true,
            scheduleEnabled: true
        )
        se.save(flush:true)
        se
    }

    def "scheduled job was already claimed by another cluster node, so should be deleted from quartz scheduler"() {
        given:
        def serverUUID = UUID.randomUUID().toString()
        def jobUUID = UUID.randomUUID().toString()
        def es = Mock(ExecutionService)
        def eus = Mock(ExecutionUtilService)
        def jobSchedulesService = Mock(JobSchedulesService)
        def jobSchedulerService = Mock(JobSchedulerService)
        def fs = Mock(FrameworkService) {
            getServerUUID() >> serverUUID
        }
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                serverNodeUUID: jobUUID,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                scheduled: true,
                executionEnabled: true,
                scheduleEnabled: true,
                uuid: jobUUID
        )
        se.save(flush:true)
        Execution e = new Execution(
                scheduledExecution: se,
                dateStarted: new Date(),
                dateCompleted: null,
                project: se.project,
                user: 'bob',
                workflow: new Workflow(commands: [new CommandExec(
                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                )]
                )
        ).save(flush: true)
        AuthContextProvider authContextProvider = Mock(AuthContextProvider)
        def datamap = new JobDataMap(
                [
                        project             : se.project,
                        scheduledExecutionId: se.uuid,
                        executionService    : es,
                        executionUtilService: eus,
                        frameworkService    : fs,
                        bySchedule          : true,
                        serverUUID          : serverUUID,
                        execution           : e,
                        jobSchedulesService : jobSchedulesService,
                        jobSchedulerService : jobSchedulerService,
                        authContextProvider : authContextProvider,
                ]
        )
        ExecutionJob job = new ExecutionJob()
        def ajobKey = JobKey.jobKey('jobname', 'jobgroup')

        def quartzScheduler = Mock(Scheduler)
        def context = Mock(JobExecutionContext) {
            getJobDetail() >> Mock(JobDetail) {
                getJobDataMap() >> datamap
                getKey() >> ajobKey
            }

            getScheduler() >> quartzScheduler
        }

        when:
        job.execute(context)

        then:
        1 * quartzScheduler.deleteJob(ajobKey)


    }
    def "scheduled job was unscheduled by another cluster node, so should be deleted from quartz scheduler"() {
        given:
        def serverUUID = UUID.randomUUID().toString()
        def jobUUID = UUID.randomUUID().toString()
        def es = Mock(ExecutionService)
        def eus = Mock(ExecutionUtilService)
        def jobSchedulesService = Mock(JobSchedulesService)
            def jobSchedulerService = Mock(JobSchedulerService)
        def fs = Mock(FrameworkService) {
            getServerUUID() >> serverUUID
        }
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                serverNodeUUID: jobUUID,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                scheduled: false,
                executionEnabled: true,
                scheduleEnabled: true,
                uuid: jobUUID
        )
        se.save(flush:true)
        AuthContextProvider authContextProvider = Mock(AuthContextProvider)
        def datamap = new JobDataMap(
                [
                        project             : se.project,
                        scheduledExecutionId: se.uuid,
                        executionService    : es,
                        executionUtilService: eus,
                        frameworkService    : fs,
                        bySchedule          : true,
                        serverUUID          : serverUUID,
                        jobSchedulesService : jobSchedulesService,
                        jobSchedulerService : jobSchedulerService,
                        authContextProvider : authContextProvider,
                ]
        )
        ExecutionJob job = new ExecutionJob()
        def ajobKey = JobKey.jobKey('jobname', 'jobgroup')

        def quartzScheduler = Mock(Scheduler)
        def context = Mock(JobExecutionContext) {
            getJobDetail() >> Mock(JobDetail) {
                getJobDataMap() >> datamap
                getKey() >> ajobKey
            }

            getScheduler() >> quartzScheduler
        }

        when:
        job.execute(context)

        then:
        1 * quartzScheduler.deleteJob(ajobKey)


    }
    def "scheduled job was stoped by another cluster node, so should be deleted from quartz scheduler"() {
        given:
        def serverUUID = UUID.randomUUID().toString()
        def jobUUID = UUID.randomUUID().toString()
        def es = Mock(ExecutionService)
        def eus = Mock(ExecutionUtilService)
        def jobSchedulesService = Mock(JobSchedulesService)
            def jobSchedulerService = Mock(JobSchedulerService)
        def fs = Mock(FrameworkService) {
            getServerUUID() >> serverUUID
        }
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                serverNodeUUID: serverUUID,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                scheduled: isScheduled,
                executionEnabled: isExecEnabled,
                scheduleEnabled: isScheduleEnabled,
                uuid: jobUUID
        )
        se.save(flush:true)
            AuthContextProvider authContextProvider = Mock(AuthContextProvider)
        def datamap = new JobDataMap(
                [
                        project             : se.project,
                        scheduledExecutionId: se.uuid,
                        executionService    : es,
                        executionUtilService: eus,
                        frameworkService    : fs,
                        bySchedule          : true,
                        serverUUID          : serverUUID,
                        jobSchedulesService : jobSchedulesService,
                        jobSchedulerService : jobSchedulerService,
                        authContextProvider : authContextProvider
                ]
        )
        ExecutionJob job = new ExecutionJob()
        def ajobKey = JobKey.jobKey('jobname', 'jobgroup')

        def quartzScheduler = Mock(Scheduler)
        def context = Mock(JobExecutionContext) {
            getJobDetail() >> Mock(JobDetail) {
                getJobDataMap() >> datamap
                getKey() >> ajobKey
            }

            getScheduler() >> quartzScheduler
        }

        when:
        job.execute(context)

        then:
        1 * quartzScheduler.deleteJob(ajobKey)

        where:
        isScheduled | isExecEnabled | isScheduleEnabled
        false       | true          | true
        true        | false         | true
        true        | true          | false


    }

    def "scheduled job quartz trigger context with scheduleArgs sets execution argString"() {

        def serverUUID = UUID.randomUUID().toString()
        def jobUUID = UUID.randomUUID().toString()
        def es = Mock(ExecutionService)
        def eus = Mock(ExecutionUtilService)
        def jobSchedulesService = Mock(JobSchedulesService)
        def jobSchedulerService = Mock(JobSchedulerService)
        def fs = Mock(FrameworkService) {
            getServerUUID() >> serverUUID
            getProjectConfigReloaded('AProject') >> Mock(IRundeckProject) {
                getProjectProperties() >> [:]
            }
        }
        AuthContextProvider authContextProvider = Mock(AuthContextProvider) {
            getAuthContextForUserAndRolesAndProject(_, _, _) >> Mock(UserAndRolesAuthContext)
        }
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                serverNodeUUID: jobUUID,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                scheduled: true,
                executionEnabled: true,
                scheduleEnabled: true,
                uuid: jobUUID
        )
        se.save(flush: true)
        Execution e = new Execution(
                scheduledExecution: se,
                dateStarted: new Date(),
                dateCompleted: null,
                project: se.project,
                user: 'bob',
                workflow: new Workflow(commands: [new CommandExec(
                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                )]
                )
        ).save(flush: true)
        def datamap = new JobDataMap(
                [
                        project             : se.project,
                        scheduledExecutionId: se.uuid,
                        executionService    : es,
                        executionUtilService: eus,
                        frameworkService    : fs,
                        bySchedule          : true,
                        jobSchedulesService : jobSchedulesService,
                        jobSchedulerService : jobSchedulerService,
                        authContextProvider : authContextProvider,
                ]
        )
        ExecutionJob job = new ExecutionJob()
        def ajobKey = JobKey.jobKey('jobname', 'jobgroup')

        def quartzScheduler = Mock(Scheduler)
        def trigger = Mock(Trigger)
        def context = Mock(JobExecutionContext) {
            getJobDetail() >> Mock(JobDetail) {
                getJobDataMap() >> datamap
                getKey() >> ajobKey
            }

            getScheduler() >> quartzScheduler
            getTrigger() >> trigger
        }
        1 * es.executeAsyncBegin(_, _, e, se, _, _) >>
                new ExecutionService.AsyncStarted(thread: new WorkflowExecutionServiceThread(null, null, null, null, null))
        given: "trigger has scheduleArgs"
        1 * trigger.getJobDataMap() >> [scheduleArgs: '-opt1 test1']

        when: "job is executed"
        job.execute(context)

        then: "execution args are set"
        0 * quartzScheduler.deleteJob(ajobKey)
        1 * es.createExecution(_, _, null, { it.argString == '-opt1 test1' }) >> e
    }

    def "average notification threshold from options"() {
        given:
        def es = new ExecutionService()
        es.jobStatsDataProvider = new GormJobStatsDataProvider()
        def jobUuid = UUID.randomUUID().toString()
        ScheduledExecution se = new ScheduledExecution(
                uuid: jobUuid,
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
                options:[
                        new Option(name: 'threshold',  required: true, enforced: false)
                ],
                notifyAvgDurationThreshold:'${option.threshold}',
                totalTime: 60000,
                execCount: 2
        )
        se.save(flush:true)
        def dataContext = [option: [threshold: '+30s']]
        ExecutionJob executionJob = new ExecutionJob()


        when:

        def result=executionJob.getNotifyAvgDurationThreshold(se.notifyAvgDurationThreshold,
                                                              es.getAverageDuration(jobUuid),dataContext)

        then:

        result == 60000


    }

    def "average notification threshold add time to avg"() {
        given:
        def es = new ExecutionService()
        es.jobStatsDataProvider = new GormJobStatsDataProvider()
        def jobUuid = UUID.randomUUID().toString()
        ScheduledExecution se = new ScheduledExecution(
                uuid: jobUuid,
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
                notifyAvgDurationThreshold:'+30s',
                totalTime: 60000,
                execCount: 2
        )
        se.save(flush:true)
        def dataContext = [:]
        ExecutionJob executionJob = new ExecutionJob()


        when:

        def result=executionJob.getNotifyAvgDurationThreshold(se.notifyAvgDurationThreshold,
                es.getAverageDuration(jobUuid),dataContext)

        then:

        result == 60000

    }

    def "average notification threshold fixed time"() {
        given:
        def es = new ExecutionService()
        es.jobStatsDataProvider = new GormJobStatsDataProvider()
        def jobUuid = UUID.randomUUID().toString()
        ScheduledExecution se = new ScheduledExecution(
                uuid: jobUuid,
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
                notifyAvgDurationThreshold:'20s',
                totalTime: 60000,
                execCount: 2
        )
        se.save(flush:true)
        def dataContext = [:]
        ExecutionJob executionJob = new ExecutionJob()


        when:

        def result=executionJob.getNotifyAvgDurationThreshold(se.notifyAvgDurationThreshold,
                es.getAverageDuration(jobUuid),dataContext)

        then:

        result == 20000

    }

    def "average notification threshold perc time"() {
        given:
        def es = new ExecutionService()
        es.jobStatsDataProvider = new GormJobStatsDataProvider()
        def jobUuid = UUID.randomUUID().toString()
        ScheduledExecution se = new ScheduledExecution(
                uuid: jobUuid,
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
                notifyAvgDurationThreshold:'10%',
                totalTime: 60000,
                execCount: 2
        )
        se.save(flush:true)
        def dataContext = [:]
        ExecutionJob executionJob = new ExecutionJob()


        when:

        def result=executionJob.getNotifyAvgDurationThreshold(se.notifyAvgDurationThreshold,
                es.getAverageDuration(jobUuid),dataContext)

        then:

        result == 33000

    }

    def "average notification threshold bad value"() {
        given:
        def es = new ExecutionService()
        es.jobStatsDataProvider = new GormJobStatsDataProvider()
        def jobUuid = UUID.randomUUID().toString()
        ScheduledExecution se = new ScheduledExecution(
                uuid: jobUuid,
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
                notifyAvgDurationThreshold:'somethingbad',
                totalTime: 60000,
                execCount: 2
        )
        se.save(flush:true)
        def dataContext = [:]
        ExecutionJob executionJob = new ExecutionJob()


        when:

        def result=executionJob.getNotifyAvgDurationThreshold(se.notifyAvgDurationThreshold,
                es.getAverageDuration(jobUuid),dataContext)

        then:

        result == 30000

    }

    def "average notification context"() {
        given:
        ScheduledExecution se = new ScheduledExecution(
                uuid: UUID.randomUUID().toString(),
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
                notifyAvgDurationThreshold:'0s',
                averageDuration:1,
                totalTime: 1,
                execCount: 1
        )
        se.save(flush:true)

        Execution e = new Execution(
                scheduledExecution: se,
                dateStarted: new Date(),
                dateCompleted: null,
                project: se.project,
                user: 'bob',
                workflow: new Workflow(commands: [new CommandExec(
                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                )]
                )
        ).save(flush: true)

        def secureOption = [:]
        def secureOptsExposed = [:]
        def datacontext = [option:[env:true]]
        def eus = Mock(ExecutionUtilService)
        def auth = Mock(UserAndRolesAuthContext)
        def framework = Mock(Framework)

        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getFramework() >> framework

        }

        CountDownLatch latch = new CountDownLatch(1)

        //simulate a workflow thread
        def testThread=new WorkflowExecutionServiceThread(null,null,origContext,null,null){
            void run(){
                //keep "running" until notification sent
                latch.await()
            }
        }
        def execmap = [
                execution         : e,
                scheduledExecution: se,
                thread : testThread
        ]

        def es = Mock(ExecutionService){
            1 * executeAsyncBegin(framework, auth, e, se, secureOption, secureOptsExposed) >> {
                testThread.start()
                execmap
            }
            getAverageDuration(_) >> 1
        }

        ExecutionJob executionJob = new ExecutionJob()
        Map content = [
            execution: e,
            context  : origContext
        ]
        ExecutionJob.RunContext runContext=new ExecutionJob.RunContext(
            executionService: es,
            executionUtilService: eus,
            execution:e,
            framework:framework,
            authContext: auth,
            scheduledExecution: se,
            timeout: 0,
            secureOpts: secureOption,
            secureOptsExposed:secureOptsExposed
        )
        when:

        def result=executionJob.executeCommand(runContext)

        then:
        1 * es.avgDurationExceeded(_, content) >> {
            latch.countDown()
        }
        result != null
        se.executions.status.get(0) == ExecutionService.AVERAGE_DURATION_EXCEEDED
    }

    def "scheduled job quartz checking the same format of dates"() {
        def serverUUID = UUID.randomUUID().toString()
        def jobUUID = UUID.randomUUID().toString()
        def es = Mock(ExecutionService)
        def eus = Mock(ExecutionUtilService)
        def jobSchedulesService = Mock(JobSchedulesService)
        def jobSchedulerService = Mock(JobSchedulerService)
        def fs = Mock(FrameworkService) {
            getServerUUID() >> serverUUID
            getProjectConfigReloaded('AProject')>>Mock(IRundeckProject){
                getProjectProperties()>>[:]
            }
        }
        AuthContextProvider authContextProvider = Mock(AuthContextProvider){
            getAuthContextForUserAndRolesAndProject(_, _, _)>>Mock(UserAndRolesAuthContext)
        }
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                serverNodeUUID: jobUUID,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                scheduled: true,
                executionEnabled: true,
                scheduleEnabled: true,
                uuid: jobUUID
        )
        se.save(flush:true)
        Execution e = new Execution(
                scheduledExecution: se,
                dateStarted: new Timestamp(new Date().time),
                dateCompleted: new Timestamp(new Date().time),
                project: se.project,
                user: 'bob',
                workflow: new Workflow(commands: [new CommandExec(
                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                )]
                )
        ).save(flush: true)
        def dataMap = new JobDataMap(
                [
                        project             : se.project,
                        scheduledExecutionId: se.uuid,
                        executionService    : es,
                        executionUtilService: eus,
                        frameworkService    : fs,
                        bySchedule          : true,
                        jobSchedulesService : jobSchedulesService,
                        jobSchedulerService : jobSchedulerService,
                        authContextProvider : authContextProvider,
                ]
        )
        ExecutionJob job = new ExecutionJob()
        def aJobKey = JobKey.jobKey('jobname', 'jobgroup')

        def quartzScheduler = Mock(Scheduler)
        def trigger=Mock(Trigger)
        def context = Mock(JobExecutionContext) {
            getJobDetail() >> Mock(JobDetail) {
                getJobDataMap() >> dataMap
                getKey() >> aJobKey
            }

            getScheduler() >> quartzScheduler
            getTrigger() >> trigger
        }
        1 * es.executeAsyncBegin(_, _, e, se, _, _) >>
                new ExecutionService.AsyncStarted(thread: new WorkflowExecutionServiceThread(null, null, null, null, null))
        given: "trigger has scheduleArgs"
        1 * trigger.getJobDataMap() >> [scheduleArgs: '-opt1 test1']

        when: "job is executed"
        job.execute(context)

        then: "execution args are set"
        0 * quartzScheduler.deleteJob(aJobKey)
        1 * es.createExecution(_, _, null, { it.argString == '-opt1 test1' }) >> e

        then: "verifying date format"
        Execution newEx = job.fetchExecution(e.id)
        checkFormatDate(newEx.dateStarted) && checkFormatDate(newEx.dateCompleted)
    }

    boolean checkFormatDate(Object dateVerify) {
        if (dateVerify instanceof Timestamp) {
            return true;
        } else {
            false
        }
    }
}
