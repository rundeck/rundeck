/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import spock.lang.Specification

class ContextManagerSpec extends Specification {
    def "begin eh should set stepctx"() {
        given:
            def cm = new ContextManager()
        when:
            cm.beginWorkflowExecution(Mock(StepExecutionContext), Mock(WorkflowExecutionItem))
        then:
            cm.getContext() == null
        when:
            cm.beginWorkflowItem(1, Mock(StepExecutionItem))
        then:
            cm.getContext() == [step: '1', stepctx: '1']
        when:
            cm.beginWorkflowItemErrorHandler(1, Mock(StepExecutionItem))
        then:
            cm.getContext() == [step: '1', stepctx: '1e']
        when:
            cm.finishWorkflowItemErrorHandler(1, Mock(StepExecutionItem), Mock(StepExecutionResult))
        then:
            cm.getContext() == [step: '1', stepctx: '1']
        when:
            cm.finishWorkflowItem(1, Mock(StepExecutionItem), Mock(StepExecutionResult))
        then:
            cm.getContext() == null
    }

    def "finish eh should reset stepctx"() {
        given:
            def cm = new ContextManager()
        when:
            cm.beginWorkflowExecution(Mock(StepExecutionContext), Mock(WorkflowExecutionItem))
            cm.beginWorkflowItem(1, Mock(StepExecutionItem))
            cm.beginWorkflowItemErrorHandler(1, Mock(StepExecutionItem))
            cm.finishWorkflowItemErrorHandler(1, Mock(StepExecutionItem), Mock(StepExecutionResult))
        then:
            cm.getContext() == [step: '1', stepctx: '1']
    }

    def "beginWorkflowItem should add node context when runner is #scenario"() {
        given: "a context manager"
            def cm = new ContextManager()
            cm.beginWorkflowExecution(Mock(StepExecutionContext), Mock(WorkflowExecutionItem))

        and: "a step execution item with runner: #hasRunner"
            def item = Mock(StepExecutionItem) {
                getRunner() >> runnerNode
            }

        when: "beginWorkflowItem is called"
            cm.beginWorkflowItem(1, item)

        then: "context includes step information and node info based on runner presence"
            def context = cm.getContext()
            context != null
            context.step == '1'
            context.stepctx == '1'
            context.node == expectedNode
            context.user == expectedUser

        where: "runner node scenarios"
            scenario      | hasRunner | runnerNode                                                                            || expectedNode    | expectedUser
            "present"     | true      | Mock(INodeEntry) { getNodename() >> 'runner-node-1'; extractUserName() >> 'runner-user' } || 'runner-node-1' | 'runner-user'
            "not present" | false     | null                                                                                   || null            | null
    }

    def "beginWorkflowItem with runner node context persists through error handler"() {
        given: "a context manager"
            def cm = new ContextManager()
            cm.beginWorkflowExecution(Mock(StepExecutionContext), Mock(WorkflowExecutionItem))

        and: "a step execution item with a runner node"
            def runnerNode = Mock(INodeEntry) {
                getNodename() >> 'runner-node-1'
                extractUserName() >> 'runner-user'
            }
            def item = Mock(StepExecutionItem) {
                getRunner() >> runnerNode
            }

        when: "beginWorkflowItem is called with a runner"
            cm.beginWorkflowItem(1, item)

        then: "context includes node information"
            def context1 = cm.getContext()
            context1.node == 'runner-node-1'

        when: "error handler is begun"
            cm.beginWorkflowItemErrorHandler(1, item)

        then: "node context is still present"
            def context2 = cm.getContext()
            context2.node == 'runner-node-1'
            context2.stepctx == '1e'

        when: "error handler finishes"
            cm.finishWorkflowItemErrorHandler(1, item, Mock(StepExecutionResult))

        then: "node context is still present"
            def context3 = cm.getContext()
            context3.node == 'runner-node-1'
            context3.stepctx == '1'
    }

    def "multiple workflow items with different runner nodes maintain correct context"() {
        given: "a context manager"
            def cm = new ContextManager()
            cm.beginWorkflowExecution(Mock(StepExecutionContext), Mock(WorkflowExecutionItem))

        and: "first step with runner node"
            def runnerNode1 = Mock(INodeEntry) {
                getNodename() >> 'runner-1'
                extractUserName() >> 'user-1'
            }
            def item1 = Mock(StepExecutionItem) {
                getRunner() >> runnerNode1
            }

        when: "first step begins"
            cm.beginWorkflowItem(1, item1)

        then: "context has first runner node"
            def context1 = cm.getContext()
            context1.node == 'runner-1'
            context1.user == 'user-1'

        when: "first step finishes"
            cm.finishWorkflowItem(1, item1, Mock(StepExecutionResult))

        and: "second step with different runner node begins"
            def runnerNode2 = Mock(INodeEntry) {
                getNodename() >> 'runner-2'
                extractUserName() >> 'user-2'
            }
            def item2 = Mock(StepExecutionItem) {
                getRunner() >> runnerNode2
            }
            cm.beginWorkflowItem(2, item2)

        then: "context has second runner node"
            def context2 = cm.getContext()
            context2.node == 'runner-2'
            context2.user == 'user-2'
            context2.step == '2'
    }

    def "hierarchical stepctx for HasParentStepContext sub-step"() {
        given:
            def cm = new ContextManager()
            cm.beginWorkflowExecution(Mock(StepExecutionContext), Mock(WorkflowExecutionItem))

        and: "a flattened conditional sub-step (parent=2, sub=1)"
            def subItem = Mock(ConditionalSubStepFixture) {
                getParentStepNumber() >> 2
                getSubStepNumber() >> 1
            }

        when:
            cm.beginWorkflowItem(2, subItem)

        then: "stepctx reflects parent/sub hierarchy and step is the sub index"
            cm.getContext() == [step: '1', stepctx: '2/1']

        when:
            cm.finishWorkflowItem(2, subItem, Mock(StepExecutionResult))

        then: "context cleared back to workflow level"
            cm.getContext() == null
    }

    def "hierarchical stepctx remains correct between consecutive sub-steps"() {
        given:
            def cm = new ContextManager()
            cm.beginWorkflowExecution(Mock(StepExecutionContext), Mock(WorkflowExecutionItem))

        and:
            def subA = Mock(ConditionalSubStepFixture) {
                getParentStepNumber() >> 2
                getSubStepNumber() >> 1
            }
            def subB = Mock(ConditionalSubStepFixture) {
                getParentStepNumber() >> 2
                getSubStepNumber() >> 2
            }

        when: "first sub-step runs"
            cm.beginWorkflowItem(2, subA)
        then:
            cm.getContext() == [step: '1', stepctx: '2/1']

        when: "first sub-step finishes and second begins"
            cm.finishWorkflowItem(2, subA, Mock(StepExecutionResult))
            cm.beginWorkflowItem(3, subB)
        then:
            cm.getContext() == [step: '2', stepctx: '2/2']

        when:
            cm.finishWorkflowItem(3, subB, Mock(StepExecutionResult))
        then:
            cm.getContext() == null
    }

    def "flat step following a conditional sub-step uses flat index"() {
        given:
            def cm = new ContextManager()
            cm.beginWorkflowExecution(Mock(StepExecutionContext), Mock(WorkflowExecutionItem))

        and:
            def sub = Mock(ConditionalSubStepFixture) {
                getParentStepNumber() >> 2
                getSubStepNumber() >> 1
            }
            def flat = Mock(StepExecutionItem)

        when: "sub-step runs and finishes"
            cm.beginWorkflowItem(2, sub)
            cm.finishWorkflowItem(2, sub, Mock(StepExecutionResult))

        and: "next flat top-level step (engine index 3) begins"
            cm.beginWorkflowItem(3, flat)

        then: "stepctx is flat (no leftover parent context)"
            cm.getContext() == [step: '3', stepctx: '3']
    }

    def "error handler on hierarchical sub-step keeps parent context"() {
        given:
            def cm = new ContextManager()
            cm.beginWorkflowExecution(Mock(StepExecutionContext), Mock(WorkflowExecutionItem))

        and:
            def sub = Mock(ConditionalSubStepFixture) {
                getParentStepNumber() >> 2
                getSubStepNumber() >> 1
            }

        when:
            cm.beginWorkflowItem(2, sub)
        then:
            cm.getContext() == [step: '1', stepctx: '2/1']

        when:
            cm.beginWorkflowItemErrorHandler(2, sub)
        then: "error handler aspect applies only to the sub-step segment"
            cm.getContext() == [step: '1', stepctx: '2/1e']

        when:
            cm.finishWorkflowItemErrorHandler(2, sub, Mock(StepExecutionResult))
        then:
            cm.getContext() == [step: '1', stepctx: '2/1']

        when:
            cm.finishWorkflowItem(2, sub, Mock(StepExecutionResult))
        then:
            cm.getContext() == null
    }

    static interface ConditionalSubStepFixture extends StepExecutionItem, HasParentStepContext {}

    def "beginWorkflowItem with null runner followed by non-null runner works correctly"() {
        given: "a context manager"
            def cm = new ContextManager()
            cm.beginWorkflowExecution(Mock(StepExecutionContext), Mock(WorkflowExecutionItem))

        and: "first step without runner node"
            def item1 = Mock(StepExecutionItem) {
                getRunner() >> null
            }

        when: "first step begins"
            cm.beginWorkflowItem(1, item1)

        then: "context has no node information"
            def context1 = cm.getContext()
            context1.node == null

        when: "first step finishes"
            cm.finishWorkflowItem(1, item1, Mock(StepExecutionResult))

        and: "second step with runner node begins"
            def runnerNode = Mock(INodeEntry) {
                getNodename() >> 'runner-node'
                extractUserName() >> 'runner-user'
            }
            def item2 = Mock(StepExecutionItem) {
                getRunner() >> runnerNode
            }
            cm.beginWorkflowItem(2, item2)

        then: "context now has node information"
            def context2 = cm.getContext()
            context2.node == 'runner-node'
            context2.user == 'runner-user'
            context2.step == '2'
    }
}


