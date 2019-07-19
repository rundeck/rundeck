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

import com.dtolabs.rundeck.app.support.ExecutionQuery
import grails.test.mixin.Mock
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import rundeck.*
import rundeck.services.ExecutionService
import rundeck.services.FileUploadService
import rundeck.services.FrameworkService
import rundeck.services.JobSchedulerService
import rundeck.services.LogFileStorageService
import spock.lang.Specification

/**
 * Created by greg on 4/12/16.
 */
@Mock([Execution, ScheduledExecution, ReferencedExecution, ExecReport])
class ExecutionsCleanUpSpec extends Specification {

    def "execute cleaner job"() {
        def jobName = 'abc'
        def groupPath = 'elf'
        def projectName = 'projectTest'
        def jobUuid = '123'

        setup:
        Date startDate = new Date(2015 - 1900, 2, 8)
        Date endDate = ExecutionQuery.parseRelativeDate("${daysToKeep}d", startDate)
        Date execDate = new Date(2015 - 1900, 02, 03)
        ExecutionQuery query = new ExecutionQuery(projFilter: projectName)
        if(null != endDate){
            query.endbeforeFilter = endDate
            query.doendbeforeFilter = true
        }
        def se = new ScheduledExecution(
                jobName: jobName,
                groupPath: groupPath,
                project: projectName,
                uuid: jobUuid,
                workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
        ).save()
        def exec = new Execution(
                scheduledExecution: se,
                dateStarted: execDate,
                dateCompleted: execDate,
                user:'user',
                status: 'success',
                project: projectName
        ).save()
        def executionService = Mock(ExecutionService) {
            queryExecutions(*_) >> {
                if(execDate.before(query.endbeforeFilter)){
                    return [result: [exec.id], total: 1]
                }

                [result: [], total: 0]
            }
        }

        def frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> {
                false
            }
        }
        def fileUploadService = Mock(FileUploadService)
        def logFileStorageService = Mock(LogFileStorageService)
        def jobSchedulerService = Mock(JobSchedulerService)

        def datamap = new JobDataMap([
                project: 'projectTest',
                maxDaysToKeep: daysToKeep,
                executionService : executionService,
                frameworkService : frameworkService,
                fileUploadService: fileUploadService,
                logFileStorageService: logFileStorageService,
                jobSchedulerService: jobSchedulerService
        ])

        ExecutionsCleanUp job = new ExecutionsCleanUp()
        def context = Mock(JobExecutionContext) {
            getJobDetail() >> Mock(JobDetail) {
                getJobDataMap() >> datamap
            }
        }

        when:
        job.execute(context)

        then:
        executionsRemoved == Execution.findAll()?.size()

        where:
        daysToKeep              | executionsRemoved
        10                      | 1
        4                       | 0
    }


    def "execute cleaner job cluster with null on the list of deadMembers"() {
        def jobName = 'abc'
        def groupPath = 'elf'
        def projectName = 'projectTest'
        def jobUuid = '123'
        def daysToKeep = 10

        setup:
        Date startDate = new Date(2015 - 1900, 2, 8)
        Date endDate = ExecutionQuery.parseRelativeDate("${daysToKeep}d", startDate)
        Date execDate = new Date(2015 - 1900, 02, 03)
        ExecutionQuery query = new ExecutionQuery(projFilter: projectName)
        if(null != endDate){
            query.endbeforeFilter = endDate
            query.doendbeforeFilter = true
        }
        def se = new ScheduledExecution(
                jobName: jobName,
                groupPath: groupPath,
                project: projectName,
                uuid: jobUuid,
                workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
        ).save()
        def exec = new Execution(
                scheduledExecution: se,
                dateStarted: execDate,
                dateCompleted: execDate,
                user:'user',
                status: 'success',
                project: projectName,
                scheduleNodeUUID: "aaaa"
        ).save()
        def executionService = Mock(ExecutionService) {
            queryExecutions(*_) >> {
                if(execDate.before(query.endbeforeFilter)){
                    return [result: [exec], total: 1]
                }

                [result: [], total: 0]
            }
        }

        def frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> {
                true
            }
            getServerUUID()>>{
                "aaaa"
            }
        }
        def fileUploadService = Mock(FileUploadService)
        def logFileStorageService = Mock(LogFileStorageService)
        def jobSchedulerService = Mock(JobSchedulerService){
            getDeadMembers(_)>>{
                ["bbbb","null"]
            }
        }

        def datamap = new JobDataMap([
                project: 'projectTest',
                maxDaysToKeep: daysToKeep,
                executionService : executionService,
                frameworkService : frameworkService,
                fileUploadService: fileUploadService,
                logFileStorageService: logFileStorageService,
                jobSchedulerService: jobSchedulerService
        ])

        ExecutionsCleanUp job = new ExecutionsCleanUp()
        def context = Mock(JobExecutionContext) {
            getJobDetail() >> Mock(JobDetail) {
                getJobDataMap() >> datamap
            }
        }

        when:
        job.execute(context)

        then:
        1*jobSchedulerService.getDeadMembers(_)
        1*executionService.queryExecutions(_)

    }
}
