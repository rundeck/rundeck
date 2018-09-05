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

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import spock.lang.Specification

/**
 * Created by greg on 11/6/15.
 */
class NodeFirstWorkflowStrategySpec extends Specification {
    public static final String TEST_PROJ = 'NodeFirstWorkflowStrategySpec'
    Framework framework
    FrameworkProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
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
        }
        framework.getStepExecutionService().registerClass('typeA', TestFailStepExecutor)
        framework.getStepExecutionService().registerClass('typeB', TestSuccessNodeExecutor)


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
