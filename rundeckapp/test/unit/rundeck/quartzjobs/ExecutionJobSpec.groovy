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
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ExecutionService
import rundeck.services.ExecutionUtilService
import rundeck.services.FrameworkService
import spock.lang.Specification

/**
 * Created by greg on 4/12/16.
 */
@Mock([ScheduledExecution, Workflow, CommandExec])
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
}
