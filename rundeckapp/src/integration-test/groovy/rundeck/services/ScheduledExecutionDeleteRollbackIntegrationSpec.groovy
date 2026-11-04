package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import grails.testing.mixin.integration.Integration
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.data.providers.GormJobStatsDataProvider
import org.rundeck.app.data.providers.GormReferencedExecutionDataProvider
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.Workflow
import spock.lang.Specification

@Integration
class ScheduledExecutionDeleteRollbackIntegrationSpec extends Specification {

    def "bulk cleanup failure rolls back earlier execution and metadata deletion"() {
        given:
        def records = ScheduledExecution.withNewTransaction {
            def workflow = new Workflow(
                commands: [new CommandExec(adhocRemoteString: 'test').save(flush: true, failOnError: true)]
            ).save(flush: true, failOnError: true)
            def job = new ScheduledExecution(
                jobName: 'rollback-test',
                groupPath: 'delete',
                uuid: UUID.randomUUID().toString(),
                project: 'rollback-project',
                workflow: workflow,
                scheduled: false
            ).save(flush: true, failOnError: true)
            def first = new Execution(
                scheduledExecution: job,
                status: 'succeeded',
                dateStarted: new Date(100),
                dateCompleted: new Date(1000),
                project: job.project,
                user: 'test',
                workflow: new Workflow(commands: [])
            ).save(flush: true, failOnError: true)
            def second = new Execution(
                scheduledExecution: job,
                status: 'failed',
                dateStarted: new Date(200),
                dateCompleted: new Date(2000),
                project: job.project,
                user: 'test',
                workflow: new Workflow(commands: [])
            ).save(flush: true, failOnError: true)
            def stats = new ScheduledExecutionStats(
                jobUuid: job.uuid,
                content: '{}'
            ).save(flush: true, failOnError: true)
            def reference = new ReferencedExecution(
                jobUuid: job.uuid,
                status: 'failed',
                execution: second
            ).save(flush: true, failOnError: true)
            [job: job.id, first: first.id, second: second.id, stats: stats.id, reference: reference.id]
        }
        def authContext = Mock(AuthContext)
        def service = new ScheduledExecutionService(
            executionServiceBean: Mock(ExecutionService) {
                deleteBulkExecutionIds([records.first, records.second], authContext, 'test', false) >> {
                    Execution.get(records.first).delete(flush: true)
                    ReferencedExecution.get(records.reference).delete(flush: true)
                    [success: false, failures: [[message: 'forced failure']]]
                }
            },
            fileUploadService: Mock(FileUploadService),
            rundeckJobDefinitionManager: Mock(RundeckJobDefinitionManager),
            jobSchedulerService: Mock(JobSchedulerService),
            referencedExecutionDataProvider: new GormReferencedExecutionDataProvider(),
            jobStatsDataProvider: new GormJobStatsDataProvider()
        )

        when:
        def result = ScheduledExecution.withNewSession {
            service.deleteScheduledExecution(
                ScheduledExecution.get(records.job),
                true,
                authContext,
                'test'
            )
        }

        then:
        !result.success
        result.error.contains('forced failure')
        ScheduledExecution.withNewTransaction {
            assert ScheduledExecution.get(records.job)
            assert Execution.get(records.first)
            assert Execution.get(records.second)
            assert ScheduledExecutionStats.get(records.stats)
            assert ReferencedExecution.get(records.reference)
            true
        }

        cleanup:
        ScheduledExecution.withNewTransaction {
            ReferencedExecution.get(records?.reference)?.delete(flush: true)
            Execution.get(records?.first)?.delete(flush: true)
            Execution.get(records?.second)?.delete(flush: true)
            ScheduledExecution.get(records?.job)?.delete(flush: true)
        }
    }

    def "outer rollback keeps the restored job schedule"() {
        given:
        def jobId = ScheduledExecution.withNewTransaction {
            def workflow = new Workflow(
                commands: [new CommandExec(adhocRemoteString: 'test').save(flush: true, failOnError: true)]
            ).save(flush: true, failOnError: true)
            new ScheduledExecution(
                jobName: 'scheduled-rollback-test',
                groupPath: 'delete',
                uuid: UUID.randomUUID().toString(),
                project: 'rollback-project',
                workflow: workflow,
                scheduled: true
            ).save(flush: true, failOnError: true).id
        }
        def authContext = Mock(AuthContext)
        def scheduler = Mock(JobSchedulerService)
        def definitionManager = Mock(RundeckJobDefinitionManager)
        def service = new ScheduledExecutionService(
            executionServiceBean: Mock(ExecutionService),
            fileUploadService: Mock(FileUploadService),
            rundeckJobDefinitionManager: definitionManager,
            jobSchedulerService: scheduler,
            referencedExecutionDataProvider: new GormReferencedExecutionDataProvider(),
            jobStatsDataProvider: new GormJobStatsDataProvider()
        )

        when:
        def result = ScheduledExecution.withNewSession {
            ScheduledExecution.withNewTransaction { status ->
                def deleted = service.deleteScheduledExecution(
                    ScheduledExecution.get(jobId),
                    true,
                    authContext,
                    'test'
                )
                status.setRollbackOnly()
                deleted
            }
        }

        then:
        result.success
        ScheduledExecution.withNewTransaction { ScheduledExecution.get(jobId) }
        1 * definitionManager.beforeDelete(_, authContext)
        0 * definitionManager.afterDelete(_, _)
        0 * scheduler.deleteJobSchedule(_, _)

        cleanup:
        ScheduledExecution.withNewTransaction {
            ScheduledExecution.get(jobId)?.delete(flush: true)
        }
    }
}
