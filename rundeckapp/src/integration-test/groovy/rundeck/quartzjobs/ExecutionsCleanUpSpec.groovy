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

    def "execute cleaner job"() {
        setup:
        Date executionCompletionDate = ExecutionQuery.parseRelativeDate("${daysForExecutionCompletion}d", new Date())
        Date execDate = new Date(2015 - 1900, 02, 03)

        def se = createJob(projectName)
        def exec = createExecution(se, execDate, executionCompletionDate, projectName)

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
        def remainingExecs = Execution.findAllByProject(projectName)


        then:
        expectedRemainingExecs == remainingExecs.size()


        where:
        daysToKeep  | expectedRemainingExecs    | daysForExecutionCompletion    | projectName
        10          | 1                         | 9                             | 'cleanupA'
    }

    def "execute cleaner job with delete"() {
        setup:
        Date executionCompletionDate = ExecutionQuery.parseRelativeDate("${daysForExecutionCompletion}d", new Date())
        Date execDate = new Date(2015 - 1900, 02, 03)

        def se = createJob(projectName)
        def exec = createExecution(se, execDate, executionCompletionDate, projectName)

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
                reportService: reportService,
                fromCleanupB: true
        ])

        ExecutionsCleanUp job = new ExecutionsCleanUp()
        def context = Mock(JobExecutionContext) {
            getJobDetail() >> Mock(JobDetail) {
                getJobDataMap() >> datamap
            }
        }

        when:
        job.execute(context)
        def remainingExecs = Execution.findAllByProject(projectName)

        then:
        expectedRemainingExecs == remainingExecs.size()

        where:
        daysToKeep  | expectedRemainingExecs    | daysForExecutionCompletion    | projectName
        4           | 0                         | 6                             | 'cleanupB'
    }


    def "execute cleaner job cluster with null on the list of deadMembers"() {
        given:
        def daysToKeep = 10
        def projectName = "someProjectName"

        and:
        Date startDate = new Date(2015 - 1900, 2, 8)
        Date endDate = ExecutionQuery.parseRelativeDate("${daysToKeep}d", startDate)
        Date execDate = new Date(2015 - 1900, 02, 03)
        ExecutionQuery query = new ExecutionQuery(projFilter: projectName)
        if(null != endDate){
            query.endbeforeFilter = endDate
            query.doendbeforeFilter = true
        }
        def se = createJob(projectName)
        def exec = createExecution(se, execDate, execDate, projectName)

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
        def remainingExecs = Execution.withNewTransaction {Execution.findAll()}

        then:
        1 * referencedExecutionDataProvider.deleteByExecutionId(_)
        1 * logFileStorageService.getExecutionFiles(*_)
        1 * fileUploadService.deleteRecordsForExecution(_)
    }


    def "num execution to remove"() {
        given:
        Date execDate = new Date(2015 - 1900, 02, 03)
        def se = createJob(projectName)
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
            def exec = createExecution(se, execDate, execDate, projectName)
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
        numExecutions | minimumExecutionToKeep | maximumDeletionSize | executionsToRemove   | projectName
        50            | 5                      | 15                  | 15                   | 'w'
        50            | 40                     | 100                 | 10                   | 'x'
        30            | 50                     | 200                 | 0                    | 'y'
        20            | 20                     | 20                  | 0                    | 'z'
    }


    def createJob(String projectName){
        ScheduledExecution.withNewTransaction {
            def se = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: UUID.randomUUID().toString(),
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save(flush: true)

            return se
        }
    }


    def createExecution(ScheduledExecution se , def dateStarted, def dateCompleted, String projectName){
        Execution.withNewTransaction {
            def exec = new Execution(
                    scheduledExecution: se,
                    dateStarted: dateStarted,
                    dateCompleted: dateCompleted,
                    user:'user',
                    status: 'success',
                    project: projectName,
                    scheduleNodeUUID: "aaaa"
            ).save(flush: true)

            return exec
        }
    }
}

