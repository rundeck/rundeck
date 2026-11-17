package rundeck.services

import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.rundeck.app.data.providers.GormJobQueryProvider
import org.springframework.beans.factory.annotation.Autowired
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

/**
 * Integration tests for the job UUID filter, matching a job and the jobs referencing it.
 */
@Integration
@Rollback
class JobUuidFilterIntegrationSpec extends Specification {
    static final String PROJECT = 'UuidFilterProject'
    static final String TARGET_UUID = '2020c78b-a228-4627-8015-3177ebaa00a7'

    @Autowired
    ScheduledExecutionService scheduledExecutionService

    private ScheduledExecution createJob(String name, List steps) {
        def workflow = new Workflow(keepgoing: false, commands: steps).save(flush: true, failOnError: true)
        new ScheduledExecution(
            jobName: name,
            project: PROJECT,
            uuid: UUID.randomUUID().toString(),
            workflow: workflow
        ).save(flush: true, failOnError: true)
    }

    private Map<String, ScheduledExecution> createJobs() {
        def target = createJob('target', [new CommandExec(adhocRemoteString: 'echo hi')])
        target.uuid = TARGET_UUID
        target.save(flush: true, failOnError: true)
        def refStep = createJob('refStep', [new JobExec(jobName: 'target', uuid: TARGET_UUID)])
        def refHandler = createJob(
            'refHandler',
            [new CommandExec(adhocRemoteString: 'false', errorHandler: new JobExec(jobName: 'target', uuid: TARGET_UUID))]
        )
        def other = createJob('other', [new JobExec(jobName: 'x', uuid: UUID.randomUUID().toString())])
        [target: target, refStep: refStep, refHandler: refHandler, other: other]
    }

    def "findJobIdsReferencingUuid finds jobs referencing the uuid in a step or error handler"() {
        given:
        def jobs = createJobs()

        when:
        def ids = GormJobQueryProvider.findJobIdsReferencingUuid(TARGET_UUID)

        then:
        ids as Set == [jobs.refStep.id, jobs.refHandler.id] as Set
    }

    def "findJobIdsReferencingUuid with no references"() {
        given:
        createJobs()

        expect:
        GormJobQueryProvider.findJobIdsReferencingUuid(UUID.randomUUID().toString()) == []
    }

    def "listWorkflows with jobUuidFilter returns the job and referencing jobs"() {
        given:
        createJobs()

        when:
        def result = scheduledExecutionService.listWorkflows(
            new ScheduledExecutionQuery(projFilter: PROJECT, jobUuidFilter: TARGET_UUID),
            [:]
        )

        then:
        result.schedlist*.jobName as Set == ['target', 'refStep', 'refHandler'] as Set
        result.total == 3
    }
}
