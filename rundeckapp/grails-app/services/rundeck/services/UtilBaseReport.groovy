package rundeck.services

import org.rundeck.app.data.model.v1.report.dto.SaveReportRequest
import org.rundeck.app.data.model.v1.report.dto.SaveReportRequestImpl
import rundeck.CommandExec
import rundeck.Execution

class UtilBaseReport {
    static SaveReportRequest createSaveReportRequestFromExecution(Execution exec) {
        def failedCount = exec.failedNodeList ?exec.failedNodeList.split(',').size():0
        def successCount=exec.succeededNodeList? exec.succeededNodeList.split(',').size():0;
        def failedList = exec.failedNodeList ?exec.failedNodeList:''
        def succeededList=exec.succeededNodeList? exec.succeededNodeList:'';
        def totalCount = failedCount+successCount;
        def adhocScript = null
        if(
                null == exec.scheduledExecution
                        && exec.workflow.commands
                        && exec.workflow.commands.size()==1
                        && exec.workflow.commands[0] instanceof CommandExec
        ){
            adhocScript=exec.workflow.commands[0].adhocRemoteString
        }
        def summary = "[${exec.workflow.commands?exec.workflow.commands.size():0} steps]"
        def issuccess = exec.statusSucceeded()
        def iscancelled = exec.cancelled
        def istimedout = exec.timedOut
        def ismissed = exec.status == "missed"
        def status = issuccess ? "succeed" : iscancelled ? "cancel" : exec.willRetry ? "retry" : istimedout ?
                "timedout" : ismissed ? "missed" : "fail"

        SaveReportRequest saveReportRequest =  new SaveReportRequestImpl()
        saveReportRequest.setExecutionId(exec.id)
        saveReportRequest.setJobId(exec.scheduledExecution?.id.toString())
        saveReportRequest.setAdhocExecution(null==exec.scheduledExecution)
        saveReportRequest.setAdhocScript(adhocScript)
        saveReportRequest.setAbortedByUser(iscancelled? exec.abortedby ?: exec.user:null)
        saveReportRequest.setNode("${successCount}/${failedCount}/${totalCount}")
        saveReportRequest.setTitle(adhocScript?adhocScript:summary)
        saveReportRequest.setStatus(status)
        saveReportRequest.setProject(exec.project)
        saveReportRequest.setReportId(exec.scheduledExecution?( exec.scheduledExecution.groupPath ? exec.scheduledExecution.generateFullName() : exec.scheduledExecution.jobName): 'adhoc')
        saveReportRequest.setAuthor(exec.user)
        saveReportRequest.setMessage(issuccess ? 'Job completed successfully' : iscancelled ? ('Job killed by: ' + (exec.abortedby ?: exec.user)) : ismissed ? "Job missed execution at: ${exec.dateStarted}" : 'Job failed')
        saveReportRequest.setDateStarted(exec.dateStarted)
        saveReportRequest.setDateCompleted(exec.dateCompleted)
        saveReportRequest.setFailedNodeList(failedList)
        saveReportRequest.setSucceededNodeList(succeededList)
        saveReportRequest.setFilterApplied(exec.filter)
        saveReportRequest.setJobUuid(exec.scheduledExecution?.uuid)
        saveReportRequest
    }

    static SaveReportRequest findJobUuidForBaseReport(SaveReportRequestImpl saveReportRequest) {
        Execution execution = Execution.get(saveReportRequest.executionId)
        if (execution) {
            saveReportRequest.jobUuid = execution.scheduledExecution?.uuid
        }
        saveReportRequest
    }
}
