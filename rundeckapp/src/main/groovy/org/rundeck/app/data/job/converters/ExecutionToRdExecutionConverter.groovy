package org.rundeck.app.data.job.converters

import rundeck.Execution
import rundeck.data.execution.RdExecution

class ExecutionToRdExecutionConverter {
    static RdExecution convert(Execution e) {
        if(!e) return null
        RdExecution re = new RdExecution()
        re.uuid = e.uuid
        re.jobUuid = e.jobUuid
        re.status = e.status
        re.dateStarted = e.dateStarted
        re.dateCompleted = e.dateCompleted
        re.executionType = e.executionType
        re.project = e.project
        re.argString = e.argString
        re.user = e.user
        re.timeout = e.timeout
        re.retry = e.retry
        re.retryDelay = e.retryDelay
        re.userRoles = e.userRoles
        re.outputfilepath = e.outputfilepath
        re.failedNodeList = e.failedNodeList
        re.succeededNodeList = e.succeededNodeList
        re.serverNodeUUID = e.serverNodeUUID
        re.nodeThreadcount = e.nodeThreadcount
        re.logFileStorageRequestId = e.logFileStorageRequestId as Long
        re.retryExecutionId = e.retryExecutionId as Long
        re.retryOriginalId = e.retryOriginalId
        re.retryPrevId = e.retryPrevId
        re.extraMetadata = e.extraMetadata
        re.abortedby = e.abortedby
        re.cancelled = e.cancelled
        re.timedOut = e.timedOut
        re.nodeConfig = e.nodeConfig
        re.workflow = WorkflowToRdWorkflowConverter.convertWorkflow(e.workflow)
        re.orchestrator = OrchestratorToRdOrchestratorConverter.convertOrchestrator(e.orchestrator)
        return re

    }
}
