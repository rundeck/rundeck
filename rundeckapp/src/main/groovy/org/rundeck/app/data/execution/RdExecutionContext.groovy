package org.rundeck.app.data.execution

import com.dtolabs.rundeck.app.support.ExecutionContext
import org.rundeck.app.data.model.v1.job.JobData

class RdExecutionContext extends ExecutionContext {

    static RdExecutionContext fromJobData(JobData job) {
        RdExecutionContext ctx = new RdExecutionContext()
        //execution context
        ctx.project = job.project
        ctx.argString = job.argString
        ctx.user = job.user
        ctx.loglevel = job.logConfig?.loglevel
        ctx.serverNodeUUID = job.serverNodeUUID
        ctx.timeout = job.timeout
        ctx.retry = job.retry
        ctx.retryDelay = job.retryDelay
        ctx.nodeRankAttribute = job.nodeConfig?.nodeRankAttribute
        ctx.nodeKeepgoing=job.nodeConfig?.nodeKeepgoing
        ctx.doNodedispatch=job.nodeConfig?.doNodedispatch
        ctx.nodeRankOrderAscending=job.nodeConfig?.nodeRankOrderAscending
        ctx.nodeFilterEditable = job.nodeConfig?.nodeFilterEditable
        //base node filter
        ctx.nodeInclude = job.nodeConfig?.nodeInclude
        ctx.nodeExclude = job.nodeConfig?.nodeInclude
        ctx.nodeIncludeName = job.nodeConfig?.nodeIncludeName
        ctx.nodeExcludeName = job.nodeConfig?.nodeExcludeName
        ctx.nodeIncludeTags = job.nodeConfig?.nodeIncludeTags
        ctx.nodeExcludeTags = job.nodeConfig?.nodeExcludeTags
        ctx.nodeIncludeOsName = job.nodeConfig?.nodeIncludeOsName
        ctx.nodeExcludeOsName = job.nodeConfig?.nodeExcludeOsName
        ctx.nodeIncludeOsFamily = job.nodeConfig?.nodeIncludeOsFamily
        ctx.nodeExcludeOsFamily = job.nodeConfig?.nodeExcludeOsFamily
        ctx.nodeIncludeOsArch = job.nodeConfig?.nodeIncludeOsArch
        ctx.nodeExcludeOsArch = job.nodeConfig?.nodeExcludeOsArch
        ctx.nodeIncludeOsVersion = job.nodeConfig?.nodeIncludeOsVersion
        ctx.nodeExcludeOsVersion = job.nodeConfig?.nodeExcludeOsVersion
        ctx.nodeExcludePrecedence= job.nodeConfig?.nodeExcludePrecedence
        ctx.successOnEmptyNodeFilter= job.nodeConfig?.successOnEmptyNodeFilter
        ctx.filter = job.nodeConfig?.filter
        ctx.filterExclude = job.nodeConfig?.filterExclude
        ctx.excludeFilterUncheck = job.nodeConfig?.excludeFilterUncheck
        ctx
    }
}
