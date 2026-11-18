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


    def "per job retention keeps the most recent completed executions of each job"() {
        given:
        def projectName = "perJobKeep-${UUID.randomUUID()}"
        def jobA = createJob(projectName)
        def jobB = createJob(projectName)
        ExecutionsCleanUp job = new ExecutionsCleanUp()

        //created newest first, so the ids are in the opposite order of the completion dates
        def execsA = (5..1).collect { day -> createJobExecution(jobA, jobA.uuid, oldDate(day), projectName) }
        def execsB = (2..1).collect { day -> createJobExecution(jobB, jobB.uuid, oldDate(day), projectName) }
        def running = createJobExecution(jobA, jobA.uuid, null, projectName)

        when:
        def result = job.searchExecutions(null, null, null, projectName, 60, 0, 500, 3)

        then:
        result.sort() == [execsA[3].id, execsA[4].id].sort()
        !result.contains(running.id)
        execsB.every { !result.contains(it.id) }
    }

    def "per job retention keeps executions of a deleted job by job uuid"() {
        given:
        def projectName = "perJobDeleted-${UUID.randomUUID()}"
        def deletedJobUuid = UUID.randomUUID().toString()
        ExecutionsCleanUp job = new ExecutionsCleanUp()
        def execs = (1..4).collect { day -> createJobExecution(null, deletedJobUuid, oldDate(day), projectName) }

        when:
        def result = job.searchExecutions(null, null, null, projectName, 60, 0, 500, 2)

        then:
        result.sort() == [execs[0].id, execs[1].id].sort()
    }

    def "per job retention does not stall when the batch would be filled with protected executions"() {
        given:
        def projectName = "perJobStarve-${UUID.randomUUID()}"
        ExecutionsCleanUp job = new ExecutionsCleanUp()
        (1..10).each {
            def protectedJob = createJob(projectName)
            (1..3).each { day -> createJobExecution(protectedJob, protectedJob.uuid, oldDate(day), projectName) }
        }
        def jobWithOld = createJob(projectName)
        def execs = (1..4).collect { day -> createJobExecution(jobWithOld, jobWithOld.uuid, oldDate(day), projectName) }

        when:
        def result = job.searchExecutions(null, null, null, projectName, 60, 0, 1, 3)

        then:
        result == [execs[0].id]
    }

    def "per job retention leaves ad hoc executions to the project-wide rules"() {
        given:
        def projectName = "perJobAdhoc-${UUID.randomUUID()}"
        ExecutionsCleanUp job = new ExecutionsCleanUp()
        def adhoc = (1..2).collect { day -> createJobExecution(null, null, oldDate(day), projectName) }

        when:
        def result = job.searchExecutions(null, null, null, projectName, 60, minimum, 500, 5)

        then:
        result.size() == expected
        result.every { id -> adhoc*.id.contains(id) }

        where:
        minimum | expected
        0       | 2
        2       | 0
    }

    def "per job retention disabled by default keeps the project-wide behavior"() {
        given:
        def projectName = "perJobDefault-${UUID.randomUUID()}"
        def se = createJob(projectName)
        ExecutionsCleanUp job = new ExecutionsCleanUp()
        def execs = (1..5).collect { day -> createJobExecution(se, se.uuid, oldDate(day), projectName) }

        when:
        def result = perJob == null ?
                     job.searchExecutions(null, null, null, projectName, 60, 0, 500) :
                     job.searchExecutions(null, null, null, projectName, 60, 0, 500, perJob)

        then:
        result.sort() == execs*.id.sort()

        where:
        perJob << [null, 0]
    }

    def "execute cleaner job reads executions to keep per job from the job data"() {
        given:
        def projectName = "perJobExecute-${UUID.randomUUID()}"
        def se = createJob(projectName)
        def execs = (1..3).collect { day -> createJobExecution(se, se.uuid, oldDate(day), projectName) }

        def datamap = new JobDataMap([
                project: projectName,
                maxDaysToKeep: 60,
                executionsToKeepPerJob: 2,
                executionService : Mock(ExecutionService),
                frameworkService : Mock(FrameworkService),
                fileUploadService: Mock(FileUploadService),
                logFileStorageService: Mock(LogFileStorageService) {
                    getExecutionFiles(*_) >> [:]
                },
                jobSchedulerService: Mock(JobSchedulerService),
                referencedExecutionDataProvider: Mock(ReferencedExecutionDataProvider),
                reportService: Mock(ReportService)
        ])
        ExecutionsCleanUp job = new ExecutionsCleanUp()
        def context = Mock(JobExecutionContext) {
            getJobDetail() >> Mock(JobDetail) {
                getJobDataMap() >> datamap
            }
        }

        when:
        job.execute(context)
        def remaining = Execution.findAllByProject(projectName)*.id

        then:
        remaining.sort() == [execs[1].id, execs[2].id].sort()
    }

    /**
     * @param day day of march 2015
     * @return a completion date older than any retention period used in these tests
     */
    private static Date oldDate(int day) {
        new Date(2015 - 1900, 2, day)
    }

    /**
     * Create a completed execution with an explicit job uuid
     * @param se job, or null for an ad hoc execution or an execution of a deleted job
     * @param jobUuid job uuid, or null for an ad hoc execution
     * @param dateCompleted completion date, or null for a running execution
     * @param projectName project
     */
    def createJobExecution(ScheduledExecution se, String jobUuid, Date dateCompleted, String projectName) {
        Execution.withNewTransaction {
            new Execution(
                    scheduledExecution: se,
                    jobUuid: jobUuid,
                    dateStarted: dateCompleted ?: new Date(),
                    dateCompleted: dateCompleted,
                    user: 'user',
                    status: dateCompleted ? 'success' : null,
                    project: projectName,
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'echo hi')])
            ).save(flush: true, failOnError: true)
        }
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

