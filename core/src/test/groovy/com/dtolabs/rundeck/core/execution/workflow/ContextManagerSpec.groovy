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
}
