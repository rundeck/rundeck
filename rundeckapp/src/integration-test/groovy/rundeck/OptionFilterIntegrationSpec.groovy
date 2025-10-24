package rundeck

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll
import rundeck.services.ReportService
import com.dtolabs.rundeck.app.support.ExecQuery

@Integration
@Rollback
class OptionFilterIntegrationSpec extends Specification {

    @Autowired
    ReportService reportService

    def "optionFilter integration test - full stack with real database"() {
        given: "executions with different option arguments"
        def job1 = new ScheduledExecution(
            jobName: 'test-job-1',
            project: 'testproject',
            groupPath: 'test',
            description: 'Test job',
            workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'echo test')]).save(flush: true, failOnError: true)
        ).save(flush: true, failOnError: true)

        def job2 = new ScheduledExecution(
            jobName: 'test-job-2',
            project: 'testproject',
            groupPath: 'test',
            description: 'Test job 2',
            workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'echo test2')]).save(flush: true, failOnError: true)
        ).save(flush: true, failOnError: true)

        // Create executions with different argStrings
        def exec1 = new Execution(
            uuid: UUID.randomUUID().toString(),
            project: 'testproject',
            user: 'testuser',
            status: 'succeeded',
            dateStarted: new Date(),
            dateCompleted: new Date(),
            scheduledExecution: job1,
            argString: '-APPLICATION_NAME myapp -ENV production -VERSION 1.2.3'
        ).save(flush: true, failOnError: true)

        def exec2 = new Execution(
            uuid: UUID.randomUUID().toString(),
            project: 'testproject',
            user: 'testuser',
            status: 'succeeded',
            dateStarted: new Date(),
            dateCompleted: new Date(),
            scheduledExecution: job1,
            argString: '-APPLICATION_NAME otherapp -ENV staging -DEBUG true'
        ).save(flush: true, failOnError: true)

        def exec3 = new Execution(
            uuid: UUID.randomUUID().toString(),
            project: 'testproject',
            user: 'testuser',
            status: 'succeeded',
            dateStarted: new Date(),
            dateCompleted: new Date(),
            scheduledExecution: job2,
            argString: '-SERVICE_NAME backend -ENV production -TIMEOUT 30'
        ).save(flush: true, failOnError: true)

        // Create corresponding ExecReports
        def report1 = new BaseReport(
            jcJobId: job1.uuid,
            jcExecId: exec1.id,
            executionUuid: exec1.uuid,
            executionId: exec1.id,
            status: 'succeed',
            actionType: 'job',
            project: 'testproject',
            reportId: job1.jobName,
            tags: 'test',
            author: 'testuser',
            title: 'Test execution 1',
            dateStarted: exec1.dateStarted,
            dateCompleted: exec1.dateCompleted,
            node: 'localhost',
            message: 'Test execution completed',
            maprefUri: exec1.argString
        ).save(flush: true, failOnError: true)

        def report2 = new BaseReport(
            jcJobId: job1.uuid,
            jcExecId: exec2.id,
            executionUuid: exec2.uuid,
            executionId: exec2.id,
            status: 'succeed',
            actionType: 'job',
            project: 'testproject',
            reportId: job1.jobName,
            tags: 'test',
            author: 'testuser',
            title: 'Test execution 2',
            dateStarted: exec2.dateStarted,
            dateCompleted: exec2.dateCompleted,
            node: 'localhost',
            message: 'Test execution completed',
            maprefUri: exec2.argString
        ).save(flush: true, failOnError: true)

        def report3 = new BaseReport(
            jcJobId: job2.uuid,
            jcExecId: exec3.id,
            executionUuid: exec3.uuid,
            executionId: exec3.id,
            status: 'succeed',
            actionType: 'job',
            project: 'testproject',
            reportId: job2.jobName,
            tags: 'test',
            author: 'testuser',
            title: 'Test execution 3',
            dateStarted: exec3.dateStarted,
            dateCompleted: exec3.dateCompleted,
            node: 'localhost',
            message: 'Test execution completed',
            maprefUri: exec3.argString
        ).save(flush: true, failOnError: true)

        expect: "optionFilter searches work as expected"
        def testScenarios = [
            [
                description: "single option exact match",
                searchTerm: "-APPLICATION_NAME myapp",
                expectedCount: 1,
                expectedExecutions: [exec1]
            ],
            [
                description: "multiple options with OR logic",
                searchTerm: "-APPLICATION_NAME myapp -ENV production",
                expectedCount: 2,
                expectedExecutions: [exec1, exec3]
            ],
            [
                description: "different job option match",
                searchTerm: "-SERVICE_NAME backend",
                expectedCount: 1,
                expectedExecutions: [exec3]
            ],
            [
                description: "non-existent option",
                searchTerm: "-NONEXISTENT value",
                expectedCount: 0,
                expectedExecutions: []
            ],
            [
                description: "case insensitive match",
                searchTerm: "-APPLICATION_NAME MYAPP",
                expectedCount: 1,
                expectedExecutions: [exec1]
            ]
        ]

        testScenarios.each { scenario ->
            def query = new ExecQuery()
            query.projFilter = 'testproject'
            query.optionFilter = scenario.searchTerm
            def result = reportService.getExecutionReports(query, true)

            assert result.reports.size() == scenario.expectedCount
            if (scenario.expectedExecutions) {
                assert result.reports.collect { it.executionId }.sort() == scenario.expectedExecutions.collect { it.id }.sort()
            }
        }

        when: "combining optionFilter with other filters"
        def query6 = new ExecQuery()
        query6.projFilter = 'testproject'
        query6.optionFilter = '-ENV production'
        query6.userFilter = 'testuser'
        def result6 = reportService.getExecutionReports(query6, true)

        then: "should apply both filters correctly"
        result6.reports.size() == 2  // Both exec1 and exec3 have ENV=production
        result6.reports.collect { it.executionId }.sort() == [exec1.id, exec3.id].sort()
    }

    @Unroll
    def "optionFilter handles edge cases gracefully - #description"() {
        given: "execution with options"
        def job = new ScheduledExecution(
            jobName: 'edge-case-job',
            project: 'testproject2',
            groupPath: 'test',
            description: 'Edge case test',
            workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'echo test')]).save(flush: true, failOnError: true)
        ).save(flush: true, failOnError: true)

        def exec = new Execution(
            uuid: UUID.randomUUID().toString(),
            project: 'testproject2',
            user: 'testuser',
            status: 'succeeded',
            dateStarted: new Date(),
            dateCompleted: new Date(),
            scheduledExecution: job,
            argString: '-TEST_OPTION value'
        ).save(flush: true, failOnError: true)

        def report = new BaseReport(
            jcJobId: job.uuid,
            jcExecId: exec.id,
            executionUuid: exec.uuid,
            executionId: exec.id,
            status: 'succeed',
            actionType: 'job',
            project: 'testproject2',
            reportId: job.jobName,
            tags: 'test',
            author: 'testuser',
            title: 'Edge case execution',
            dateStarted: exec.dateStarted,
            dateCompleted: exec.dateCompleted,
            node: 'localhost',
            message: 'Test execution completed',
            maprefUri: exec.argString
        ).save(flush: true, failOnError: true)

        when:
        def query = new ExecQuery()
        query.projFilter = 'testproject2'
        query.optionFilter = filterValue
        def result = reportService.getExecutionReports(query, true)

        then: "should return all results without error"
        result.reports.size() >= 1  // At least our test execution

        where:
        description        | filterValue
        "empty string"     | ''
        "null value"       | null
        "whitespace only"  | '   '
    }
}