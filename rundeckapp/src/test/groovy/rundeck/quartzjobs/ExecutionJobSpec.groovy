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

import grails.test.mixin.Mock
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.quartz.JobKey
import org.quartz.Scheduler
import rundeck.CommandExec
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.Execution
import rundeck.services.ExecutionService
import rundeck.services.ExecutionUtilService
import rundeck.services.FrameworkService
import spock.lang.Specification

/**
 * Created by greg on 4/12/16.
 */
@Mock([ScheduledExecution, Workflow, CommandExec, Execution])
class ExecutionJobSpec extends Specification {
    def "execute missing job"() {
        given:
        def datamap = new JobDataMap([scheduledExecutionId: 123L])
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

    def "scheduled job was already claimed by another cluster node, so should be deleted from quartz scheduler"() {
        given:
        def serverUUID = UUID.randomUUID().toString()
        def jobUUID = UUID.randomUUID().toString()
        def es = Mock(ExecutionService)
        def eus = Mock(ExecutionUtilService)
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
                scheduleEnabled: true
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
        def datamap = new JobDataMap(
                [
                        scheduledExecutionId: se.id,
                        executionService    : es,
                        executionUtilService: eus,
                        frameworkService    : fs,
                        bySchedule          : true,
                        serverUUID          : serverUUID,
                        execution           : e
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
                scheduleEnabled: true
        )
        se.save(flush:true)
        def datamap = new JobDataMap(
                [
                        scheduledExecutionId: se.id,
                        executionService    : es,
                        executionUtilService: eus,
                        frameworkService    : fs,
                        bySchedule          : true,
                        serverUUID          : serverUUID
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
                scheduleEnabled: isScheduleEnabled
        )
        se.save(flush:true)
        def datamap = new JobDataMap(
                [
                        scheduledExecutionId: se.id,
                        executionService    : es,
                        executionUtilService: eus,
                        frameworkService    : fs,
                        bySchedule          : true,
                        serverUUID          : serverUUID
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

    def "average notification threshold from options"() {
        given:
        ScheduledExecution se = new ScheduledExecution(
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
                        new Option(name: 'threshold',  required: true)
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
                                                              se.averageDuration,dataContext)

        then:

        result == 60000


    }

    def "average notification threshold add time to avg"() {
        given:
        ScheduledExecution se = new ScheduledExecution(
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
                se.averageDuration,dataContext)

        then:

        result == 60000

    }

    def "average notification threshold fixed time"() {
        given:
        ScheduledExecution se = new ScheduledExecution(
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
                se.averageDuration,dataContext)

        then:

        result == 20000

    }

    def "average notification threshold perc time"() {
        given:
        ScheduledExecution se = new ScheduledExecution(
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
                se.averageDuration,dataContext)

        then:

        result == 33000

    }

    def "average notification threshold bad value"() {
        given:
        ScheduledExecution se = new ScheduledExecution(
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
                se.averageDuration,dataContext)

        then:

        result == 30000

    }
}
