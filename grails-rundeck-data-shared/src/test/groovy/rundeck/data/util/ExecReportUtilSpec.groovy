package rundeck.data.util

import org.rundeck.app.data.model.v1.execution.ExecutionData
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import spock.lang.Specification

class ExecReportUtilSpec extends Specification {

    def "test buildSaveReportRequest method"() {
        given:
        def exec = Mock(ExecutionData){
            internalId >> 1
            succeededNodeList >> "localhost"
            status >> "succeeded"
            dateStarted >> new Date()
            dateCompleted >> new Date()
            statusSucceeded() >> true
            cancelled >> false
            timedOut >> false
            project >> "test"
            user >> "test"
            uuid >> "uuid"
            workflow >> Mock(WorkflowData){
                steps >> [Mock(WorkflowStepData) {
                    pluginType >> "builtin-script"
                    nodeStep >> true
                }, Mock(WorkflowStepData)]
            }
        }
        def job = Mock(JobData){
            uuid >> "uuidJob"
            groupPath >> "path"
            jobName >> "jobName"
        }
        job.metaClass.getId = { -> 1 }
        when:
        def request = ExecReportUtil.buildSaveReportRequest(exec, job)
        then:
        request.executionId == 1
        request.jobId == "uuidJob"
        request.adhocExecution == false
        request.adhocScript == null
        request.abortedByUser == null
        request.node == "1/0/1"
        request.title == "Plugin[builtin-script, nodeStep: true] [... 2 steps]"
        request.status == "succeed"
        request.project == "test"
        request.reportId == "path/jobName"
        request.author == "test"
        request.message == "Job completed successfully"
        request.dateStarted == exec.dateStarted
        request.dateCompleted == exec.dateCompleted
        request.failedNodeList == ""
        request.succeededNodeList == "localhost"
        request.filterApplied == null
        request.jobUuid == "uuidJob"
        request.executionUuid == "uuid"

    }

    def "test summarize method"() {
        given:
        def exec = Mock(ExecutionData){
            workflow >> Mock(WorkflowData){
                steps >> [Mock(WorkflowStepData) {
                    pluginType >> "builtin-script"
                    nodeStep >> true
                }, Mock(WorkflowStepData), Mock(WorkflowStepData)]
            }
        }
        when:
        def summary = ExecReportUtil.summarizeJob(exec)
        then:
        summary == "Plugin[builtin-script, nodeStep: true] [... 3 steps]"
    }
}
