package rundeck.services

import com.dtolabs.rundeck.app.support.ExecQuery
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.rundeck.app.data.providers.GormReferencedExecutionDataProvider
import org.rundeck.app.data.providers.v1.report.ExecReportDataProvider
import rundeck.CommandExec
import rundeck.ExecReport
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Shared
import spock.lang.Specification

@Integration
@Rollback
class ReportServiceIntegrationSpec extends Specification {

    @Shared
    ReportService service = new ReportService()

    def "report should include referenced executions"() {
        given:
        String jobUuid = UUID.randomUUID().toString()
        service.referencedExecutionDataProvider = new GormReferencedExecutionDataProvider()
        service.execReportDataProvider = Mock(ExecReportDataProvider)

        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                uuid: jobUuid,
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job.save()
        ScheduledExecution job2 = new ScheduledExecution(
                uuid: UUID.randomUUID().toString(),
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job2.save()
        Execution e1 = new Execution(
                id:1,
                project: project,
                scheduledExecution: job2,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save()
        ReferencedExecution refexec = new ReferencedExecution(status: 'success', jobUuid: job.uuid, execution: e1)
        refexec.save()
        ExecQuery query = new ExecQuery()
        query.projFilter = "AProject"
        query.jobIdFilter = "${job.id}"
        query.execProjects = ["AProject"]
        query.excludeJobListFilter = []
        when:
        def result = service.getExecutionReports(query, true)
        then:
        result.reports.size() == 1
        1 * service.execReportDataProvider.getExecutionReports(_, _, _, [e1.id]) >> [new ExecReport(executionId: e1.id, dateCompleted: new Date())]
    }

}
