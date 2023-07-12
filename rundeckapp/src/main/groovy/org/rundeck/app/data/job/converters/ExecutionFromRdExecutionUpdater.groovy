package org.rundeck.app.data.job.converters

import rundeck.Execution
import rundeck.LogFileStorageRequest
import rundeck.Orchestrator
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.data.execution.RdExecution
import rundeck.data.job.RdNodeConfig
import rundeck.data.job.RdOrchestrator

class ExecutionFromRdExecutionUpdater {
    static void update(Execution e, RdExecution re) {
        e.uuid = re.uuid
        if(!e.id && !e.uuid) e.uuid = UUID.randomUUID().toString()
        if(re.jobUuid && (!e.scheduledExecution || e.scheduledExecution?.uuid != re.jobUuid)) {
            e.scheduledExecution = ScheduledExecution.findByUuid(re.jobUuid)
        }
        e.jobUuid = re.jobUuid
        e.status = re.status
        e.dateStarted = re.dateStarted
        e.dateCompleted = re.dateCompleted
        e.executionType = re.executionType
        e.project = re.project
        e.argString = re.argString
        e.user = re.user
        e.timeout = re.timeout
        e.retry = re.retry
        e.retryDelay = re.retryDelay
        e.setUserRoles(re.userRoles)
        e.outputfilepath = re.outputfilepath
        e.failedNodeList = re.failedNodeList
        e.succeededNodeList = re.succeededNodeList
        e.serverNodeUUID = re.serverNodeUUID
        e.nodeThreadcount = re.nodeThreadcount
        if(re.logFileStorageRequestId && e.logFileStorageRequestId != re.logFileStorageRequestId) {
            e.logFileStorageRequest = LogFileStorageRequest.get(re.logFileStorageRequestId)
        }
        if(re.retryExecutionId && e.retryExecutionId != re.retryExecutionId) {
            e.retryExecution = Execution.get(re.retryExecutionId)
        }
        e.retryOriginalId = re.retryOriginalId
        e.retryPrevId = re.retryPrevId
        e.setExtraMetadataMap(re.extraMetadataMap)
        e.abortedby = re.abortedby
        e.cancelled = re.cancelled
        e.timedOut = re.timedOut
        updateNodeConfig(e, re.nodeConfig)
        if(!e.workflow) e.workflow = new Workflow(commands:[])
        WorkflowUpdater.updateWorkflow(e.workflow, re.workflow)
        updateOrchestrator(e, re.orchestrator)
    }

    static updateOrchestrator(Execution e, RdOrchestrator rdo) {
        if(!e.orchestrator && !rdo) return
        if(e.orchestrator && !rdo) {
            e.orchestrator = null
            return
        }
        if(!e.orchestrator) e.orchestrator = new Orchestrator()
        OrchestratorUpdater.updateOrchestrator(e.orchestrator, rdo)
    }

    static void updateNodeConfig(Execution e, RdNodeConfig nodeConfig) {
        if(!nodeConfig) return
        e.nodeInclude = nodeConfig.nodeInclude
        e.nodeIncludeName = nodeConfig.nodeIncludeName
        e.nodeIncludeTags = nodeConfig.nodeIncludeTags
        e.nodeIncludeOsName = nodeConfig.nodeIncludeOsName
        e.nodeIncludeOsArch = nodeConfig.nodeIncludeOsArch
        e.nodeIncludeOsFamily = nodeConfig.nodeIncludeOsFamily
        e.nodeIncludeOsVersion = nodeConfig.nodeIncludeOsVersion
        e.nodeExclude = nodeConfig.nodeExclude
        e.nodeExcludeName = nodeConfig.nodeExcludeName
        e.nodeExcludeTags = nodeConfig.nodeExcludeTags
        e.nodeExcludeOsName = nodeConfig.nodeExcludeOsName
        e.nodeExcludeOsArch = nodeConfig.nodeExcludeOsArch
        e.nodeExcludeOsFamily = nodeConfig.nodeExcludeOsFamily
        e.nodeExcludeOsVersion = nodeConfig.nodeExcludeOsVersion
        e.nodeExcludePrecedence = nodeConfig.nodeExcludePrecedence
        e.successOnEmptyNodeFilter = nodeConfig.successOnEmptyNodeFilter
        e.nodeKeepgoing = nodeConfig.nodeKeepgoing
        e.doNodedispatch = nodeConfig.doNodedispatch
        e.nodeRankAttribute = nodeConfig.nodeRankAttribute
        e.nodeRankOrderAscending = nodeConfig.nodeRankOrderAscending
        e.nodeFilterEditable = nodeConfig.nodeFilterEditable
        e.nodeThreadcount = nodeConfig.nodeThreadcount
        e.filter = nodeConfig.filter
        e.filterExclude = nodeConfig.filterExclude
        e.excludeFilterUncheck = nodeConfig.excludeFilterUncheck
    }
}
