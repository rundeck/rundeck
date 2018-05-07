/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
