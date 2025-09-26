package rundeck

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
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
        def report1 = new ExecReport(
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

        def report2 = new ExecReport(
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

        def report3 = new ExecReport(
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

        when: "searching with single option filter"
        def query1 = new ExecQuery()
        query1.projFilter = 'testproject'
        query1.optionFilter = '-APPLICATION_NAME myapp'
        def result1 = reportService.getExecutionReports(query1, true)

        then: "should find only executions with matching APPLICATION_NAME"
        result1.reports.size() == 1
        result1.reports[0].executionId == exec1.id

        when: "searching with multiple option filter"
        def query2 = new ExecQuery()
        query2.projFilter = 'testproject'
        query2.optionFilter = '-APPLICATION_NAME myapp -ENV production'
        def result2 = reportService.getExecutionReports(query2, true)

        then: "should find only executions matching both options"
        result2.reports.size() == 1
        result2.reports[0].executionId == exec1.id

        when: "searching with different option"
        def query3 = new ExecQuery()
        query3.projFilter = 'testproject'
        query3.optionFilter = '-SERVICE_NAME backend'
        def result3 = reportService.getExecutionReports(query3, true)

        then: "should find execution with different job"
        result3.reports.size() == 1
        result3.reports[0].executionId == exec3.id

        when: "searching with non-existent option"
        def query4 = new ExecQuery()
        query4.projFilter = 'testproject'
        query4.optionFilter = '-NONEXISTENT value'
        def result4 = reportService.getExecutionReports(query4, true)

        then: "should find no results"
        result4.reports.size() == 0

        when: "searching with case insensitive match"
        def query5 = new ExecQuery()
        query5.projFilter = 'testproject'
        query5.optionFilter = '-APPLICATION_NAME MYAPP'  // uppercase
        def result5 = reportService.getExecutionReports(query5, true)

        then: "should find case insensitive match"
        result5.reports.size() == 1
        result5.reports[0].executionId == exec1.id

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

    def "optionFilter handles edge cases gracefully"() {
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

        def report = new ExecReport(
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

        when: "optionFilter is empty string"
        def query1 = new ExecQuery()
        query1.projFilter = 'testproject2'
        query1.optionFilter = ''
        def result1 = reportService.getExecutionReports(query1, true)

        then: "should return all results"
        result1.reports.size() >= 1  // At least our test execution

        when: "optionFilter is null"
        def query2 = new ExecQuery()
        query2.projFilter = 'testproject2'
        query2.optionFilter = null
        def result2 = reportService.getExecutionReports(query2, true)

        then: "should return all results without error"
        result2.reports.size() >= 1

        when: "optionFilter is whitespace only"
        def query3 = new ExecQuery()
        query3.projFilter = 'testproject2'
        query3.optionFilter = '   '
        def result3 = reportService.getExecutionReports(query3, true)

        then: "should return all results without error"
        result3.reports.size() >= 1
    }
}