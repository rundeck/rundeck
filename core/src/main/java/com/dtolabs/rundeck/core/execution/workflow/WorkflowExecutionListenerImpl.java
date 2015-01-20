/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* WorkflowExecutionListenerImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/28/11 3:30 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils;
import com.dtolabs.rundeck.core.execution.workflow.state.StepContextId;
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.utils.Pair;

import java.util.*;

/**
 * WorkflowExecutionListenerImpl uses the {@link WorkflowExecutionListener} methods to maintain workflow execution
 * context data while executing workflows, allowing the ContextLogger to have proper context.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class WorkflowExecutionListenerImpl extends ContextualExecutionListener implements WorkflowExecutionListener,ExecutionListener {
    /**
     * Uses a thread local context stack, inherited by sub threads.
     */
    private StepContextWorkflowExecutionListener<INodeEntry, StepContextId> stepContext = new
            StepContextWorkflowExecutionListener<INodeEntry, StepContextId>();

    private WorkflowExecutionListenerImpl delegate;

    protected WorkflowExecutionListenerImpl(WorkflowExecutionListenerImpl delegate) {
        super(delegate);
        this.delegate=delegate;
    }

    public WorkflowExecutionListenerImpl(final FailedNodesListener failedNodesListener,
                                         final ContextLogger logger, final boolean terse, final String logFormat) {
        super(failedNodesListener, logger, terse, logFormat);
    }

    @Override
    public void beginExecuteNodeStep(final ExecutionContext context, final NodeStepExecutionItem item, final INodeEntry node) {
        if(null!=delegate) {
            delegate.beginExecuteNodeStep(context, item, node);
            return;
        }
        super.beginExecuteNodeStep(context, item, node);
        stepContext.beginNodeContext(node);
        log(Constants.DEBUG_LEVEL,
            "[workflow] beginExecuteNodeStep(" + node.getNodename() + "): " + item.getType() + ": " + item
        );
    }

    @Override
    public void finishExecuteNodeStep(final NodeStepResult result, final ExecutionContext context,
                                      final StepExecutionItem item, final INodeEntry node) {
        if (null != delegate) {
            delegate.finishExecuteNodeStep(result, context, item, node);
            return;
        }
        super.finishExecuteNodeStep(result, context, item, node);
        stepContext.finishNodeContext();
        log(Constants.DEBUG_LEVEL,
            "[workflow] finishExecuteNodeStep(" + node.getNodename() + "): " + item.getType() + ": " + result);
    }


    @Override
    public Map<String, String> getLoggingContext() {
        if (null != delegate) {
            return delegate.getLoggingContext();
        }
        INodeEntry currentNode = stepContext.getCurrentNode();
        List<Pair<StepContextId,INodeEntry>> currentContext = stepContext.getCurrentContextPairs();
        if (null != currentContext || null!=currentNode) {
            final HashMap<String, String> loggingContext = new HashMap<String, String>();
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
     * @return Convert stack of context data into a StepIdentifier
     * @param stack stack
     */
    private StepIdentifier generateIdentifier(List<Pair<StepContextId, INodeEntry>> stack) {
        List<StepContextId> ctxs = new ArrayList<StepContextId>();
        int i=0;
        for (Pair<StepContextId, INodeEntry> pair : stack) {
            Map<String, String> params = null;
            if (null != pair.getSecond() && i < stack.size() - 1) {
                //include node as a parameter to the context only when it is an intermediate step,
                // i.e. a parent in the sequence
                params = new HashMap<String, String>();
                params.put("node", pair.getSecond().getNodename());
            }
            ctxs.add(StateUtils.stepContextId(pair.getFirst().getStep(), !pair.getFirst().getAspect().isMain(),
                    params));
            i++;
        }
        return StateUtils.stepIdentifier(ctxs);
    }

    public void beginWorkflowExecution(final StepExecutionContext executionContext, final WorkflowExecutionItem item) {
        if (null != delegate) {
            delegate.beginWorkflowExecution(executionContext, item);
            return;
        }
        stepContext.beginContext();
        log(Constants.DEBUG_LEVEL,
                "[workflow] Begin execution: " + item.getType() + " context: " + stepContext.getCurrentContextPairs()
        );
    }


    public void finishWorkflowExecution(final WorkflowExecutionResult result, final StepExecutionContext executionContext,
                                        final WorkflowExecutionItem item) {
        if (null != delegate) {
            delegate.finishWorkflowExecution(result, executionContext, item);
            return;
        }
        stepContext.finishContext();
        log(Constants.DEBUG_LEVEL,
            "[workflow] Finish execution:  " + item.getType() + ": " + result
        );
    }

    public void beginWorkflowItem(final int step, final StepExecutionItem item) {
        if (null != delegate) {
            delegate.beginWorkflowItem(step, item);
            return;
        }
        stepContext.beginStepContext(StateUtils.stepContextId(step,false));
        log(Constants.DEBUG_LEVEL,
            "[workflow] Begin step: " + step + "," + item.getType()
        );
    }

    @Override
    public void beginWorkflowItemErrorHandler(int step, StepExecutionItem item) {
        if (null != delegate) {
            delegate.beginWorkflowItemErrorHandler(step, item);
            return;
        }
        stepContext.beginStepContext(StateUtils.stepContextId(step, true));
        log(Constants.DEBUG_LEVEL,
                "[workflow] Begin error handler: " + step + "," + item.getType()
        );
    }

    public void finishWorkflowItem(final int step, final StepExecutionItem item, StepExecutionResult result) {
        if (null != delegate) {
            delegate.finishWorkflowItem(step, item,result);
            return;
        }
        stepContext.finishStepContext();
        log(Constants.DEBUG_LEVEL,
            "[workflow] Finish step: " + step + "," + item.getType()
        );
    }

    @Override
    public void finishWorkflowItemErrorHandler(int step, StepExecutionItem item, StepExecutionResult result) {
        if (null != delegate) {
            delegate.finishWorkflowItemErrorHandler(step, item, result);
            return;
        }
        stepContext.finishStepContext();
        log(Constants.DEBUG_LEVEL,
                "[workflow] Finish error handler: " + step + "," + item.getType()
        );
    }

    @Override
    public ExecutionListenerOverride createOverride() {
        return new WorkflowExecutionListenerImpl(this);
    }
}
