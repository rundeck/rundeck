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
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider
import rundeck.*
import rundeck.services.ExecutionService
import rundeck.services.FileUploadService
import rundeck.services.FrameworkService
import rundeck.services.JobSchedulerService
import rundeck.services.LogFileStorageService
import rundeck.services.ReportService
import spock.lang.Specification

/**
 * Integration test for ExecutionsCleanUp
 */
@Integration
@Rollback
class ExecutionsCleanUpSpec extends Specification {
    def jobName = 'abc'
    def groupPath = 'elf'
    def projectName = 'projectTest'
    def jobUuid = '123'

    def cleanup() {
        // Clean up all test data after each test
        Execution.deleteAll(Execution.list())
        ScheduledExecution.deleteAll(ScheduledExecution.list())
    }

    def "execute cleaner job"() {
        setup:
        Date startDate = new Date(2015 - 1900, 2, 8)
        Date executionCompletionDate = ExecutionQuery.parseRelativeDate("${daysForExecutionCompletion}d", new Date())
        Date execDate = new Date(2015 - 1900, 02, 03)

        def se = createJob()
        def exec = createExecution(se, execDate, executionCompletionDate)

        def executionService = Mock(ExecutionService)

        def frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> {
                false
            }
            getServerUUID() >> {
                "test-server-uuid"
            }
        }
        def fileUploadService = Mock(FileUploadService)
        def logFileStorageService = Mock(LogFileStorageService) {
            getExecutionFiles(*_) >> [:]
        }
        def jobSchedulerService = Mock(JobSchedulerService)
        def reportService = Mock(ReportService)
        def referencedExecutionDataProvider = Mock(ReferencedExecutionDataProvider)

        def datamap = new JobDataMap([
                project: projectName,
                maxDaysToKeep: daysToKeep,
                executionService : executionService,
                frameworkService : frameworkService,
                fileUploadService: fileUploadService,
                logFileStorageService: logFileStorageService,
                jobSchedulerService: jobSchedulerService,
                referencedExecutionDataProvider: referencedExecutionDataProvider,
                reportService: reportService
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
        daysToKeep  | executionsRemoved | daysForExecutionCompletion
        10          | 1                 | 9
        4           | 0                 | 6
    }


    def "execute cleaner job cluster with null on the list of deadMembers"() {
        given:
        def daysToKeep = 10

        and:
        Date startDate = new Date(2015 - 1900, 2, 8)
        Date endDate = ExecutionQuery.parseRelativeDate("${daysToKeep}d", startDate)
        Date execDate = new Date(2015 - 1900, 02, 03)
        ExecutionQuery query = new ExecutionQuery(projFilter: projectName)
        if(null != endDate){
            query.endbeforeFilter = endDate
            query.doendbeforeFilter = true
        }
        def se = createJob()
        def exec = createExecution(se, execDate, execDate)

        def frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> {
                true
            }
            getServerUUID() >> {
                "aaaa"
            }
        }
        def fileUploadService = Mock(FileUploadService)
        def logFileStorageService = Mock(LogFileStorageService)
        def jobSchedulerService = Mock(JobSchedulerService)
        def reportService = Mock(ReportService)
        def referencedExecutionDataProvider = Mock(ReferencedExecutionDataProvider)
        def executionService = Mock(ExecutionService)

        def datamap = new JobDataMap([
                project: projectName,
                maxDaysToKeep: daysToKeep,
                executionService : executionService,
                frameworkService : frameworkService,
                fileUploadService: fileUploadService,
                logFileStorageService: logFileStorageService,
                jobSchedulerService: jobSchedulerService,
                referencedExecutionDataProvider: referencedExecutionDataProvider,
                reportService: reportService
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
        1 * referencedExecutionDataProvider.deleteByExecutionId(_)
        1 * logFileStorageService.getExecutionFiles(*_)
        1 * fileUploadService.deleteRecordsForExecution(_)
    }


    def "num execution to remove"() {
        given:
        Date execDate = new Date(2015 - 1900, 02, 03)
        def se = createJob()
        def frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> {
                false
            }
        }
        def jobSchedulerService = Mock(JobSchedulerService)

        ExecutionsCleanUp job = new ExecutionsCleanUp()
        def daysToKeep = 60

        when:
        def executionList = []
        def totalExecutions = []

        for(int i=0;  i < numExecutions; i++){
            def exec = createExecution(se, execDate, execDate)
            if(i < maximumDeletionSize){
                executionList.add(exec.id)
            }
            totalExecutions.add(exec.id)
        }

        def executionService = Mock(ExecutionService) {
            queryExecutions(*_) >> {
                return [result: executionList, total: totalExecutions.size()]
            }
        }

        def result = job.searchExecutions(frameworkService, executionService, jobSchedulerService, projectName, daysToKeep, minimumExecutionToKeep , maximumDeletionSize)

        then:
        executionsToRemove == result.size()

        where:
        numExecutions | minimumExecutionToKeep | maximumDeletionSize | executionsToRemove
        50            | 5                      | 15                  | 15
        50            | 40                     | 100                 | 10
        30            | 50                     | 200                 | 0
        20            | 20                     | 20                  | 0
    }


    def createJob(){
        def se = new ScheduledExecution(
                jobName: jobName,
                groupPath: groupPath,
                project: projectName,
                uuid: jobUuid,
                workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
        ).save(flush: true)

        se
    }


    def createExecution(ScheduledExecution se , def dateStarted, def dateCompleted ){
        def exec = new Execution(
                scheduledExecution: se,
                dateStarted: dateStarted,
                dateCompleted: dateCompleted,
                user:'user',
                status: 'success',
                project: projectName,
                scheduleNodeUUID: "aaaa"
        ).save(flush: true)

        exec
    }
}

