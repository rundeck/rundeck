package rundeck.data.util

import org.rundeck.app.data.model.v1.execution.ExecutionData
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.report.dto.SaveReportRequest
import rundeck.data.constants.WorkflowStepConstants
import rundeck.data.report.SaveReportRequestImpl

class ExecReportUtil {
    static SaveReportRequest buildSaveReportRequest(ExecutionData exec, JobData job = null) {
        def failedCount = exec.failedNodeList ? exec.failedNodeList.split(',').size() : 0
        def successCount = exec.succeededNodeList ? exec.succeededNodeList.split(',').size() : 0;
        def failedList = exec.failedNodeList ? exec.failedNodeList : ''
        def succeededList = exec.succeededNodeList ? exec.succeededNodeList : '';
        def totalCount = failedCount + successCount;
        def adhocScript = null
        if (
                null == job
                        && exec.workflow.steps
                        && exec.workflow.steps.size() == 1
                        && exec.workflow.steps[0].pluginType == WorkflowStepConstants.TYPE_COMMAND
        ) {
            adhocScript = exec.workflow.steps[0].configuration.exec
        }
        def summary = summarizeJob(exec)
        def issuccess = exec.statusSucceeded()
        def iscancelled = exec.cancelled
        def istimedout = exec.timedOut
        def ismissed = exec.status == "missed"
        def status = issuccess ? "succeed" : iscancelled ? "cancel" : exec.willRetry ? "retry" : istimedout ?
                "timedout" : ismissed ? "missed" : "fail"

        return new SaveReportRequestImpl(
                executionId: (exec.getInternalId() as String).toLong(),
                jobId: job?.uuid,
                adhocExecution: null == job,
                adhocScript: adhocScript,
                abortedByUser: iscancelled ? exec.abortedby ?: exec.user : null,
                node: "${successCount}/${failedCount}/${totalCount}",
                title: adhocScript ? adhocScript : summary,
                status: status,
                project: exec.project,
                reportId: job ? (job.groupPath ? [job.groupPath, job.jobName].join("/") : job.jobName) : 'adhoc',
                author: exec.user,
                message: (issuccess ? 'Job completed successfully' : iscancelled ? ('Job killed by: ' + (exec.abortedby ?: exec.user)) : ismissed ? "Job missed execution at: ${exec.dateStarted}" : 'Job failed'),
                dateStarted: exec.dateStarted,
                dateCompleted: exec.dateCompleted,
                failedNodeList: failedList,
                succeededNodeList: succeededList,
                filterApplied: exec.nodeConfig?.filter,
                jobUuid: job?.uuid,
                executionUuid: exec.uuid)

    }

    static String summarizeJob(ExecutionData exec){
        StringBuffer sb = new StringBuffer()
        final def wfsize = exec?.workflow?.steps?.size() ?: 0

        if(wfsize>0){
            sb<<WorkflowStepUtil.summarize(exec.workflow.steps[0])
        }else{
            sb<< "[Empty workflow]"
        }
        if(wfsize>1){
            sb << " [... ${wfsize} steps]"
        }
        return sb.toString()
    }
}
