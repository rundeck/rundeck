package com.dtolabs.rundeck.app.internal.framework

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IFrameworkNodes
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.common.INodeDesc
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeFileParserException
import com.dtolabs.rundeck.core.common.NodesSelector
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.execution.service.FileCopier
import com.dtolabs.rundeck.core.execution.service.FileCopierService
import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService
import com.dtolabs.rundeck.core.utils.IPropertyLookup

/**
 * Created by greg on 2/20/15.
 */
class RundeckFramework implements IFramework {

    @Delegate
    IFrameworkNodes frameworkNodes

    ProjectManager frameworkProjectMgr

    IPropertyLookup propertyLookup

    @Delegate
    IFrameworkServices frameworkServices

}
