/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.ExecutionListenerOverride
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.FailedNodesListener
import com.dtolabs.rundeck.core.execution.HandlerExecutionItem
import com.dtolabs.rundeck.core.execution.HasFailureHandler
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommand
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandBase
import spock.lang.Specification
import spock.lang.Unroll

class BaseWorkflowExecutorSpec extends Specification {
    static class TestHaltWorkflowExecutor extends BaseWorkflowExecutor {
        StepExecutionResult originStepResult
        boolean haltSuccess
        String haltString
        boolean doHalt
        boolean doContinue

        TestHaltWorkflowExecutor(
            final IFramework framework,
            final StepExecutionResult originStepResult,
            final boolean haltSuccess,
            final String haltString,
            final boolean doHalt,
            final boolean doContinue
        ) {
            super(framework)
            this.originStepResult = originStepResult
            this.haltSuccess = haltSuccess
            this.haltString = haltString
            this.doHalt = doHalt
            this.doContinue = doContinue
        }

        @Override
        WorkflowExecutionResult executeWorkflowImpl(
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item
        ) {
            return null
        }

        protected StepExecutionResult executeWFItem(
            final StepExecutionContext executionContext,
            final Map<Integer, StepExecutionResult> failedMap,
            final int c,
            final StepExecutionItem cmd
        ) {
            if (doHalt) {
                if (haltString) {
                    executionContext.flowControl.Halt(haltString)
                } else {
                    executionContext.flowControl.Halt(haltSuccess)
                }
            } else if (doContinue) {
                executionContext.flowControl.Continue()
            }
            originStepResult
        }
    }

    static enum TestFailureReason implements FailureReason {
        TestFailure
    }

    @Unroll
    def "execute workflow step flow control halt status is preserved"() {
        given:
            def testFramework = Mock(IFramework)
            def originStepResult = srcSuccess ? new StepExecutionResultImpl() :
                                   new StepExecutionResultImpl(null, TestFailureReason.TestFailure, 'a message')
            BaseWorkflowExecutor instance = new TestHaltWorkflowExecutor(
                testFramework,
                originStepResult,
                haltSuccess,
                haltString,
                doHalt,
                doContinue
            )
            def listener = Mock(WorkflowExecutionListener)
            def cmd = Mock(StepExecutionItem)
            def context = Mock(StepExecutionContext) {
                getExecutionListener() >> Mock(ExecutionListener)
            }
        when:
            def result = instance.executeWorkflowStep(context, [:], [], false, listener, 1, cmd)

        then:
            result.success == expectSuccess
            result.statusString == expectString
            result.controlBehavior == (doHalt ? ControlBehavior.Halt : doContinue ? ControlBehavior.Continue : null)
            result.stepResult.failureReason?.toString() == expectReason

        where:
            doHalt | doContinue | haltSuccess | haltString   | srcSuccess | expectSuccess | expectString | expectReason
            //no control
            false  | false      | false       | null         | true       | true          | null         | null
            false  | false      | false       | null         | false      | false         | null         | 'TestFailure'
            //halt success=true
            true   | false      | true        | null         | false      | true          | null         | null
            true   | false      | true        | null         | true       | true          | null         | null
            //halt status string
            true   | false      | false       | 'testStatus' | false      | false         | 'testStatus' | 'FlowControlHalted'
            true   | false      | false       | 'testStatus' | true       | false         | 'testStatus' | 'FlowControlHalted'
            //halt success=false
            true   | false      | false       | null         | false      | false         | null         | 'FlowControlHalted'
            true   | false      | false       | null         | true       | false         | null         | 'FlowControlHalted'
            //continue
            false  | true       | false       | null         | true       | true          | null         | null
            false  | true       | false       | null         | false      | false         | null         | 'TestFailure'
    }


    def testIgnoreErrorOnErrorHandlerWithKeepgoing() throws Exception {
        given:
            String testProject = 'atest'
            def testFramework = Mock(IFramework){
                getExecutionService()>>Mock(ExecutionService){
                    executeStep(_,_)>>Mock(StepExecutionResult){
                        isSuccess()>>true
                    }
                }
            }
            BaseWorkflowExecutor instance = new BaseWorkflowExecutor(testFramework) {
                @Override
                public WorkflowExecutionResult executeWorkflowImpl(
                    StepExecutionContext executionContext,
                    WorkflowExecutionItem item
                ) {
                    return null;
                }
            };
            ExecutionListener listener = Mock(ExecutionListener);
            final StepExecutionContext context = ExecutionContextImpl.builder()
                                                                     .frameworkProject(testProject)
                                                                     .framework(testFramework)
                                                                     .executionListener(listener)
                                                                     .user("blah")
                                                                     .threadCount(1)
                                                                     .build();
            final String[] strings = ["test", "command"].toArray();
            def handlerItem=!hasHandler?null:Mock(HandlerExecutionItem){
                _*isKeepgoingOnSuccess()>>handlerKeepgoing
            }
            ExecCommand command = new ExecCommandBase() {
                public String[] getCommand() {

                    return strings;
                }

                @Override
                public StepExecutionItem getFailureHandler() {
                    return handlerItem
                }
            };
        when:

            instance.executeWFItem(context, new HashMap<Integer, StepExecutionResult>(), 0, command);
        then:
            1 * listener.ignoreErrors(shouldIgnore)
        where:
            hasHandler | handlerKeepgoing | shouldIgnore
            true       | true             | true
            true       | false            | false
            false      | false            | false
    }


    def "multiple nodes should execute error handler only at node with error"(){
        given:
            String testProject = 'atest'
            NodeSetImpl allNodes = new NodeSetImpl();

            def testnode1 = new NodeEntryImpl("testnode1")
            def testnode2 = new NodeEntryImpl("testnode2")
            allNodes.putNode(testnode1);
            allNodes.putNode(testnode2);
            def ehItem = Mock(NodeStepExecutionItem) {
                getType() >> NodeDispatchStepExecutor.STEP_EXECUTION_TYPE
                getNodeStepType() >> "dtest2"
            }
            def item = Mock(HandlerTestItem) {
                getType() >> NodeDispatchStepExecutor.STEP_EXECUTION_TYPE
                getNodeStepType() >> "dtest1"

                getFailureHandler() >> ehItem
            }
            def dispatcherResults=[
                testnode1: new NodeStepResultImpl(
                    new Exception("expected error"),
                    NodeStepFailureReason.ConnectionFailure,
                    "failed",
                    null,
                    testnode1
                ),
                testnode2: new NodeStepResultImpl(testnode2)
            ]
            def testFramework = Mock(IFramework){
                getExecutionService()>>Mock(ExecutionService){

                    1 * executeStep(_, item)>> {
                        return NodeDispatchStepExecutor.wrapDispatcherResult(
                            Mock(DispatcherResult) {
                                isSuccess() >> false
                                getResults() >> dispatcherResults
                            }
                        )
                    }
                    1 * executeStep(_, ehItem)>> {
                        StepExecutionContext ctx = (StepExecutionContext)it[0]
                        assert !ctx.nodeSelector.acceptNode(testnode2)
                        assert ctx.nodeSelector.acceptNode(testnode1)
                        return NodeDispatchStepExecutor.wrapDispatcherResult(
                            Mock(DispatcherResult) {
                                isSuccess() >> true
                                getResults() >> [
                                    testnode1: new NodeStepResultImpl(testnode2)
                                ]
                            }
                        )
                    }
                }
            }
            final BaseWorkflowExecutor strategy = new BaseWorkflowExecutor(testFramework) {
                @Override
                WorkflowExecutionResult executeWorkflowImpl(
                    final StepExecutionContext executionContext,
                    final WorkflowExecutionItem item2
                ) {
                    return null
                }
            }
            def override = Mock(ExecutionListenerOverride) {
                1*setFailedNodesListener(_) >> {
                    FailedNodesListener fnl = (FailedNodesListener)it[0]
                    fnl.matchedNodes(dispatcherResults.keySet())
                    fnl.nodesFailed(dispatcherResults.subMap(['testnode1']))
                }

            }
            def override2 = Mock(ExecutionListenerOverride) {
                1*setFailedNodesListener(_) >> {
                    FailedNodesListener fnl = (FailedNodesListener)it[0]
                    fnl.nodesSucceeded()
                }

            }
            def overrides=[override,override2]
            def listener=Mock(ExecutionListener) {
                createOverride() >>{
                    return overrides.remove(0)
                }
            }
            def context = Mock(StepExecutionContext){
                getNodes()>>allNodes
                getExecutionListener()>>listener
            }
        when:
            final BaseWorkflowExecutor.StepResultCapture result = strategy.executeWorkflowStep(
                context,
                new HashMap<>(),
                new ArrayList<>(),
                false,
                Mock(WorkflowExecutionListener),
                1,
                item);

        then:
            !result.success

        where:
            threadCount<<[1,2]
    }
    static interface HandlerTestItem extends NodeStepExecutionItem, HasFailureHandler{
    }
}
