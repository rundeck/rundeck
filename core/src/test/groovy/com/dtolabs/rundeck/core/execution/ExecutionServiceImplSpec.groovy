/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution

import com.dtolabs.rundeck.core.common.IExecutionProviders
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.PluginControlService
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.ResourceMeta
import org.rundeck.storage.api.Resource
import spock.lang.Specification

class ExecutionServiceImplSpec extends Specification {

    /**
     * RUN-3651: when secureOption storagePath contains a node variable (resolved per-node),
     * data.* vars accumulated by log filter plugins in prior steps must not be lost.
     *
     * Before fix: WFSharedContext.with(ContextView.global(), ...) discarded all non-global views.
     * After fix:  new WFSharedContext(existing) + merge(global, dataContextObject) preserves them.
     */
    def "executeNodeStep preserves accumulated data vars when secureOption storagePath uses node variable"() {
        given: "a shared data context that already has data vars from a prior log filter step"
        def originalSharedCtx = new WFSharedContext()
        originalSharedCtx.merge(ContextView.global(), new BaseDataContext([
            data  : [variable: "accumulated-value"],
            option: ["myKey": "keys/project/foo/mynode"]
        ]))

        and: "mutable option / nodeDeferred maps backed by the same BaseDataContext"
        def optionMap = new HashMap<String, String>(["myKey": "keys/project/foo/mynode"])
        def nodeDeferredMap = new HashMap<String, String>(["myKey": "keys/project/foo/mynode"])
        def dataContext = new BaseDataContext([option: optionMap, nodeDeferred: nodeDeferredMap])

        and: "a storage tree that returns the resolved password"
        def storageTree = Mock(StorageTree)
        def resource = Mock(Resource)
        def resourceMeta = Mock(ResourceMeta)
        storageTree.getResource("keys/project/foo/mynode") >> resource
        resource.getContents() >> resourceMeta
        resourceMeta.getInputStream() >> new ByteArrayInputStream("secretpassword".bytes)

        and: "execution context mock"
        def context = Mock(StepExecutionContext)
        context.getSharedDataContext() >> originalSharedCtx
        context.getDataContext() >> dataContext
        context.getDataContextObject() >> dataContext
        context.getStorageTree() >> storageTree
        context.getPluginControlService() >> Mock(PluginControlService)
        context.getLoggingManager() >> null
        context.getWorkflowExecutionListener() >> null
        context.getExecutionListener() >> Mock(ExecutionListener)
        context.getFramework() >> null
        context.getFrameworkProject() >> "test-project"
        context.getExecution() >> null
        context.getUser() >> "admin"
        context.getNodeSelector() >> null
        context.getNodes() >> null
        context.getLoglevel() >> 0
        context.getCharsetEncoding() >> null
        context.getPrivateDataContextObject() >> null
        context.getExecutionLogger() >> null
        context.getUserAndRolesAuthContext() >> null
        context.getThreadCount() >> 1
        context.isKeepgoing() >> false
        context.getNodeRankAttribute() >> null
        context.isNodeRankOrderAscending() >> true
        context.getJobService() >> null
        context.getNodeService() >> null
        context.getOrchestrator() >> null
        context.getOutputContext() >> null
        context.getStepContext() >> null
        context.getComponentList() >> null
        context.getWorkflowData() >> null

        and: "an interpreter that captures the nodeContext it receives"
        StepExecutionContext capturedNodeContext = null
        def interpreter = Mock(NodeStepExecutor)
        interpreter.executeNodeStep(_, _, _) >> { StepExecutionContext ctx, item, node ->
            capturedNodeContext = ctx
            def result = Mock(NodeStepResult)
            result.isSuccess() >> true
            result
        }

        and: "providers returning the mock interpreter"
        def providers = Mock(IExecutionProviders)
        providers.getNodeStepExecutorForItem(_, _, _) >> interpreter

        and: "a node whose name is embedded in the storage path"
        def node = new NodeEntryImpl("mynode")
        def item = Mock(NodeStepExecutionItem)
        item.getNodeStepType() >> "command"

        def service = new ExecutionServiceImpl()
        service.setExecutionProviders(providers)

        when:
        service.executeNodeStep(context, item, node)

        then: "nodeContext was built and received by the interpreter"
        capturedNodeContext != null

        and: "consolidated view preserves data vars from prior log filter steps"
        def globalData = capturedNodeContext.getSharedDataContext().consolidate().getData(ContextView.global())
        globalData?.get("data")?.get("variable") == "accumulated-value"

        and: "the consolidated view also has the resolved secure option value"
        globalData?.get("option")?.get("myKey") == "secretpassword"
    }

    def "executeNodeStep does not modify sharedDataContext when no nodeDeferred options present"() {
        given: "a shared data context with accumulated data vars"
        def originalSharedCtx = new WFSharedContext()
        originalSharedCtx.merge(ContextView.global(), new BaseDataContext([
            data: [variable: "accumulated-value"]
        ]))

        and: "context with no nodeDeferred options"
        def dataContext = new BaseDataContext([option: ["myKey": "somevalue"]])
        def context = Mock(StepExecutionContext)
        context.getSharedDataContext() >> originalSharedCtx
        context.getDataContext() >> dataContext
        context.getDataContextObject() >> dataContext
        context.getStorageTree() >> Mock(StorageTree)
        context.getPluginControlService() >> Mock(PluginControlService)
        context.getLoggingManager() >> null
        context.getWorkflowExecutionListener() >> null
        context.getExecutionListener() >> Mock(ExecutionListener)
        context.getFramework() >> null
        context.getFrameworkProject() >> "test-project"
        context.getExecution() >> null
        context.getUser() >> "admin"
        context.getNodeSelector() >> null
        context.getNodes() >> null
        context.getLoglevel() >> 0
        context.getCharsetEncoding() >> null
        context.getPrivateDataContextObject() >> null
        context.getExecutionLogger() >> null
        context.getUserAndRolesAuthContext() >> null
        context.getThreadCount() >> 1
        context.isKeepgoing() >> false
        context.getNodeRankAttribute() >> null
        context.isNodeRankOrderAscending() >> true
        context.getJobService() >> null
        context.getNodeService() >> null
        context.getOrchestrator() >> null
        context.getOutputContext() >> null
        context.getStepContext() >> null
        context.getComponentList() >> null
        context.getWorkflowData() >> null

        and: "an interpreter that captures the nodeContext"
        StepExecutionContext capturedNodeContext = null
        def interpreter = Mock(NodeStepExecutor)
        interpreter.executeNodeStep(_, _, _) >> { StepExecutionContext ctx, item, node ->
            capturedNodeContext = ctx
            def result = Mock(NodeStepResult)
            result.isSuccess() >> true
            result
        }

        def providers = Mock(IExecutionProviders)
        providers.getNodeStepExecutorForItem(_, _, _) >> interpreter

        def node = new NodeEntryImpl("mynode")
        def item = Mock(NodeStepExecutionItem)
        item.getNodeStepType() >> "command"

        def service = new ExecutionServiceImpl()
        service.setExecutionProviders(providers)

        when:
        service.executeNodeStep(context, item, node)

        then: "data vars are present in the node context (no secureOptionReplaced path taken)"
        capturedNodeContext != null
        def globalData = capturedNodeContext.getSharedDataContext().consolidate().getData(ContextView.global())
        globalData?.get("data")?.get("variable") == "accumulated-value"
    }
}
