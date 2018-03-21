/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.Contextual;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils;
import com.dtolabs.rundeck.core.execution.workflow.state.StepContextId;
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Listens to workflow events and manages the step context information, producing a Map of context data
 *
 * @author greg
 * @since 5/11/17
 */
public class ContextManager extends NoopWorkflowExecutionListener implements Contextual, WorkflowExecutionListener {
    /**
     * Uses a thread local context stack, inherited by sub threads.
     */
    private StepContextWorkflowExecutionListener<INodeEntry, StepContextId> stepContext = new
            StepContextWorkflowExecutionListener<>();


    @Override
    public void beginExecuteNodeStep(
            final ExecutionContext context,
            final NodeStepExecutionItem item,
            final INodeEntry node
    )
    {
        stepContext.beginNodeContext(node);
    }

    @Override
    public void finishExecuteNodeStep(
            final NodeStepResult result, final ExecutionContext context,
            final StepExecutionItem item, final INodeEntry node
    )
    {
        stepContext.finishNodeContext();
    }


    @Override
    public Map<String, String> getContext() {
        INodeEntry currentNode = stepContext.getCurrentNode();
        List<Pair<StepContextId, INodeEntry>> currentContext = stepContext.getCurrentContextPairs();
        if (null != currentContext || null != currentNode) {
            final HashMap<String, String> loggingContext = new HashMap<>();
            if (null != currentNode) {
                loggingContext.put("node", currentNode.getNodename());
                loggingContext.put("user", currentNode.extractUserName());
            }
            if (null != currentContext) {
                StepContextId last = currentContext.get(currentContext.size() - 1).getFirst();
                if (last.getStep() > -1) {
                    loggingContext.put("step", Integer.toString(last.getStep()));
                }
                loggingContext.put("stepctx", StateUtils.stepIdentifierToString(generateIdentifier(currentContext)));
            }
            return loggingContext;
        } else {
            return null;
        }
    }


    /**
     * @param stack stack
     *
     * @return Convert stack of context data into a StepIdentifier
     */
    private StepIdentifier generateIdentifier(List<Pair<StepContextId, INodeEntry>> stack) {
        List<StepContextId> ctxs = new ArrayList<>();
        int i = 0;
        for (Pair<StepContextId, INodeEntry> pair : stack) {
            Map<String, String> params = null;
            if (null != pair.getSecond() && i < stack.size() - 1) {
                //include node as a parameter to the context only when it is an intermediate step,
                // i.e. a parent in the sequence
                params = new HashMap<>();
                params.put("node", pair.getSecond().getNodename());
            }
            ctxs.add(StateUtils.stepContextId(pair.getFirst().getStep(), !pair.getFirst().getAspect().isMain(),
                                              params
            ));
            i++;
        }
        return StateUtils.stepIdentifier(ctxs);
    }

    public void beginWorkflowExecution(final StepExecutionContext executionContext, final WorkflowExecutionItem item) {
        stepContext.beginContext();
    }


    public void finishWorkflowExecution(
            final WorkflowExecutionResult result, final StepExecutionContext executionContext,
            final WorkflowExecutionItem item
    )
    {
        stepContext.finishContext();
    }

    public void beginWorkflowItem(final int step, final StepExecutionItem item) {
        stepContext.beginStepContext(StateUtils.stepContextId(step, false));
    }

    @Override
    public void beginWorkflowItemErrorHandler(int step, StepExecutionItem item) {
        stepContext.beginStepContext(StateUtils.stepContextId(step, true));
    }

    public void finishWorkflowItem(final int step, final StepExecutionItem item, StepExecutionResult result) {
        stepContext.finishStepContext();
    }

    @Override
    public void finishWorkflowItemErrorHandler(int step, StepExecutionItem item, StepExecutionResult result) {
        stepContext.finishStepContext();
    }
}
