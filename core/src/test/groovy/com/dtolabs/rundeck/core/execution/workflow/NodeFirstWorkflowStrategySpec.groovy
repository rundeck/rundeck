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

package com.dtolabs.rundeck.core.execution.workflow

import com.dtolabs.rundeck.core.common.BaseFrameworkExecutionServices
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.IExecutionProviders
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.ServiceSupport
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.ExecutionServiceImpl
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResultImpl
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher
import com.dtolabs.rundeck.core.execution.dispatch.SequentialNodeDispatcher
import com.dtolabs.rundeck.core.execution.service.FileCopier
import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import spock.lang.Specification

/**
 * Created by greg on 11/6/15.
 */
class NodeFirstWorkflowStrategySpec extends Specification {
    public static final String TEST_PROJ = 'NodeFirstWorkflowStrategySpec'
    Framework framework
    FrameworkProject testProject
    ServiceSupport serviceSupport
    ExecutionServiceImpl executionServiceImpl

    def setup() {
        serviceSupport=new ServiceSupport()
        def execServices=new BaseFrameworkExecutionServices()
        serviceSupport.setExecutionServices(execServices)

        executionServiceImpl = new ExecutionServiceImpl()

        IExecutionProviders frameworkPlugins = Mock(IExecutionProviders) {
            _ * getStepExecutorForItem(_, _) >> Mock(StepExecutor)
            _ * getFileCopierForNodeAndProject(_, _) >> Mock(FileCopier)
            _ * getNodeDispatcherForContext(_) >> Mock(NodeDispatcher)
            _ * getNodeExecutorForNodeAndProject(_, _) >> Mock(NodeExecutor)
            _ * getNodeStepExecutorForItem(_, _) >> Mock(NodeStepExecutor)
        }
        executionServiceImpl.setExecutionProviders(frameworkPlugins)
        serviceSupport.executionProviders = frameworkPlugins
        serviceSupport.executionService = executionServiceImpl
        framework = AbstractBaseTest.createTestFramework(serviceSupport)
        execServices.framework=framework
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(TEST_PROJ)
    }

    def cleanup() {
        framework.getFrameworkProjectMgr().removeFrameworkProject(TEST_PROJ)
    }
    static class TestFailStepExecutor implements StepExecutor{

        @Override
        boolean isNodeDispatchStep(final StepExecutionItem item) {
            true
        }

        @Override
        StepExecutionResult executeWorkflowStep(
            final StepExecutionContext executionContext,
            final StepExecutionItem item
        ) throws StepException {
            System.err.println("failure StepExecutor")
            return NodeDispatchStepExecutor.wrapDispatcherException(new DispatcherException(
                "bad node",
                new NodeStepException("bad", NodeStepFailureReason.NonZeroResultCode, "node1"),
                new NodeEntryImpl('node1')
            )
            )
        }
    }
    static class TestSuccessNodeExecutor implements StepExecutor{
        @Override
        boolean isNodeDispatchStep(final StepExecutionItem item) {
            false
        }

        @Override
        StepExecutionResult executeWorkflowStep(
            final StepExecutionContext executionContext,
            final StepExecutionItem item
        ) throws StepException {
            System.err.println("success StepExecutor")
            new StepExecutionResultImpl()
        }
    }
    def "node step fails then workflow step success should fail"() {
        given:
        def strategy = new NodeFirstWorkflowExecutor(framework)
        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)
        def dataContext= new BaseDataContext()
        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Mock(ExecutionListener) {
                log(*_) >> { args ->
                    System.err.println(args[1])
                }
            }
            getNodes() >> nodeSet
            getFrameworkProject()>>TEST_PROJ
            getDataContext()>>dataContext
            getDataContextObject()>>dataContext
            getFramework() >> framework
            componentForType(_) >> Optional.empty()
            componentsForType(_) >> []
            getComponentList()>>[]
        }


        def execPluginsMock = Mock(IExecutionProviders) {
            _ * getStepExecutorForItem({ it.type == 'typeA' }, _) >> new TestFailStepExecutor()
            _ * getStepExecutorForItem({ it.type == 'typeB' }, _) >> new TestSuccessNodeExecutor()
            _ * getNodeDispatcherForContext(_) >> new SequentialNodeDispatcher(framework)
        }
        serviceSupport.setExecutionProviders(execPluginsMock)
        executionServiceImpl.setExecutionProviders(execPluginsMock)


        def item = Mock(WorkflowExecutionItem) {
            getWorkflow() >> Mock(IWorkflow) {
                isKeepgoing() >> true
                getCommands() >> [
                        Mock(StepExecutionItem) {
                            getType() >> 'typeA'
                        },
                        Mock(StepExecutionItem) {
                            getType() >> 'typeB'
                        }
                ]
            }
        }


        when:
        def result = strategy.executeWorkflowImpl(context, item)


        then:
        !result.isSuccess()
        result.getNodeFailures()
        result.getNodeFailures()['node1']
        NodeStepFailureReason.NonZeroResultCode == result.nodeFailures['node1'][0].failureReason

    }

    /**
     * Regression test for RUN-4561:
     * When node-keepgoing is true and a workflow-step (e.g. Job Reference) splits the section list,
     * surviving nodes must continue executing subsequent sections instead of being halted by the
     * outer section-loop break that fires when workflow-keepgoing is false.
     */
    def "node-keepgoing with workflow step between node steps: surviving nodes complete all sections"() {
        given: "a two-node workflow: [node-step(fails on nodeA), workflow-step, node-step]"
        def strategy = new NodeFirstWorkflowExecutor(framework)

        def nodeA = new NodeEntryImpl('nodeA')
        def nodeB = new NodeEntryImpl('nodeB')
        def nodeSet = new NodeSetImpl()
        nodeSet.putNode(nodeA)
        nodeSet.putNode(nodeB)

        def wfStepNodes   = []  // tracks nodes visible to the workflow step (step 2)
        def finalStepNodes = [] // tracks nodes the final node step (step 3) ran on

        def dataContext = new BaseDataContext()
        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Mock(ExecutionListener) {
                log(*_)         >> { args -> System.err.println(args[1]) }
                ignoreErrors(_) >> {}
                getFailedNodesListener() >> null
            }
            getNodes()          >> nodeSet
            isKeepgoing()       >> true   // node-keepgoing: continue on other nodes after one fails
            getFrameworkProject() >> TEST_PROJ
            getDataContext()      >> dataContext
            getDataContextObject() >> dataContext
            getFramework()        >> framework
            componentForType(_)   >> Optional.empty()
            componentsForType(_)  >> []
            getComponentList()    >> []
            getStepContext()      >> []
        }

        // Step 1: node-dispatch step — fails on nodeA, succeeds on nodeB
        def step1Executor = new StepExecutor() {
            boolean isNodeDispatchStep(StepExecutionItem item) { true }
            StepExecutionResult executeWorkflowStep(StepExecutionContext ctx, StepExecutionItem item) {
                def nodeNames = ctx.getNodes()?.getNodeNames() ?: []
                if (nodeNames.contains('nodeA')) {
                    return NodeDispatchStepExecutor.wrapDispatcherException(new DispatcherException(
                        "nodeA failed",
                        new NodeStepException("bad", NodeStepFailureReason.NonZeroResultCode, "nodeA"),
                        nodeA
                    ))
                }
                return NodeDispatchStepExecutor.wrapDispatcherResult(
                    new DispatcherResultImpl(['nodeB': new NodeStepResultImpl(nodeB)], true)
                )
            }
        }

        // Step 2: workflow step (like a Job Reference) — NOT a node-dispatch step
        def step2Executor = new StepExecutor() {
            boolean isNodeDispatchStep(StepExecutionItem item) { false }
            StepExecutionResult executeWorkflowStep(StepExecutionContext ctx, StepExecutionItem item) {
                wfStepNodes.addAll(ctx.getNodes()?.getNodeNames() ?: [])
                return new StepExecutionResultImpl()
            }
        }

        // Step 3: node-dispatch step — always succeeds, records which nodes it ran on
        def step3Executor = new StepExecutor() {
            boolean isNodeDispatchStep(StepExecutionItem item) { true }
            StepExecutionResult executeWorkflowStep(StepExecutionContext ctx, StepExecutionItem item) {
                def nodeNames = ctx.getNodes()?.getNodeNames() ?: []
                def nodeName = nodeNames.isEmpty() ? null : nodeNames.first()
                if (nodeName) {
                    finalStepNodes.add(nodeName)
                    def node = nodeName == 'nodeA' ? nodeA : nodeB
                    return NodeDispatchStepExecutor.wrapDispatcherResult(
                        new DispatcherResultImpl([(nodeName): new NodeStepResultImpl(node)], true)
                    )
                }
                return new StepExecutionResultImpl()
            }
        }

        def execPluginsMock = Mock(IExecutionProviders) {
            _ * getStepExecutorForItem({ it.type == 'step1' }, _) >> step1Executor
            _ * getStepExecutorForItem({ it.type == 'step2' }, _) >> step2Executor
            _ * getStepExecutorForItem({ it.type == 'step3' }, _) >> step3Executor
            _ * getNodeDispatcherForContext(_) >> new SequentialNodeDispatcher(framework)
        }
        serviceSupport.setExecutionProviders(execPluginsMock)
        executionServiceImpl.setExecutionProviders(execPluginsMock)

        def item = Mock(WorkflowExecutionItem) {
            getWorkflow() >> Mock(IWorkflow) {
                isKeepgoing()   >> false  // stop at failed step (workflow-level)
                getThreadcount() >> 1
                getStrategy()    >> 'node-first'
                getPluginConfig() >> [:]
                getCommands() >> [
                    Mock(StepExecutionItem) { getType() >> 'step1' },
                    Mock(StepExecutionItem) { getType() >> 'step2' },
                    Mock(StepExecutionItem) { getType() >> 'step3' }
                ]
            }
        }

        when:
        def result = strategy.executeWorkflowImpl(context, item)

        then: "overall workflow failed because nodeA failed step 1"
        !result.isSuccess()

        and: "nodeA failure is recorded"
        result.getNodeFailures().containsKey('nodeA')

        and: "the workflow step (step 2) ran only on surviving nodeB, not on failed nodeA"
        wfStepNodes.contains('nodeB')
        !wfStepNodes.contains('nodeA')

        and: "the final node step (step 3) ran on nodeB but not on nodeA"
        finalStepNodes.contains('nodeB')
        !finalStepNodes.contains('nodeA')
    }

    def "node step fails on empty node list"() {
        given:
        def strategy = new NodeFirstWorkflowExecutor(framework)
        def nodeSet = new NodeSetImpl()
        Map<String,String> dataContext = new HashMap<>()
        Map<String,String> job = new HashMap<>()
        job.put("successOnEmptyNodeFilter",String.valueOf(success))
        dataContext.put("job",job)
        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Mock(ExecutionListener) {
                log(*_) >> { args ->
                    System.err.println(args[1])
                }
            }
            getNodes() >> nodeSet
            getFrameworkProject()>>TEST_PROJ
            getDataContext()>>new BaseDataContext(dataContext)
            getFramework() >> framework
        }
        def item = Mock(WorkflowExecutionItem) {
            getWorkflow() >> Mock(IWorkflow) {
                isKeepgoing() >> true
                getCommands() >> [
                        Mock(StepExecutionItem) {
                            getType() >> 'typeA'
                        },
                        Mock(StepExecutionItem) {
                            getType() >> 'typeB'
                        }
                ]
            }
        }

        when:
        def result = strategy.executeWorkflowImpl(context, item)


        then:
        result.isSuccess() == success


        where:
        success | _
        true    | _
        false   | _

    }


}
