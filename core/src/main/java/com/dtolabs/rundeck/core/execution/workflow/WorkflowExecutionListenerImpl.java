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
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * WorkflowExecutionListenerImpl uses the {@link WorkflowExecutionListener} methods to maintain workflow execution
 * context data while executing workflows, allowing the ContextLogger to have proper context.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class WorkflowExecutionListenerImpl extends ContextualExecutionListener implements WorkflowExecutionListener {
    /**
     * Thread local context stack, inherited by sub threads.
     */
    private InheritableThreadLocal<WFStepContext> localStep = new InheritableThreadLocal<WFStepContext>();
    private InheritableThreadLocal<INodeEntry> localNode = new InheritableThreadLocal<INodeEntry>();
    private InheritableThreadLocal<ContextStack<WFStepContext>> contextStack = new InheritableThreadLocal<ContextStack<WFStepContext>>();

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
        localNode.set(node);
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
        localNode.set(null);
        log(Constants.DEBUG_LEVEL,
            "[workflow] finishExecuteNodeStep(" + node.getNodename() + "): " + item.getType() + ": " + result);
    }


    @Override
    public Map<String, String> getLoggingContext() {
        if (null != delegate) {
            return delegate.getLoggingContext();
        }

        if (null != localStep.get() || null != localNode.get()) {
            final HashMap<String, String> loggingContext = new HashMap<String, String>();
            if (null != localNode.get()) {
                final INodeEntry node = localNode.get();
                loggingContext.put("node", node.getNodename());
                loggingContext.put("user", node.extractUserName());
            }
            if (null != localStep.get()) {
                final WFStepContext wfStepInfo = localStep.get();
                final int step = wfStepInfo.getStep();
                if(null!= contextStack.get()) {
                    loggingContext.put("command", generateContextString(contextStack.get().copyPush(wfStepInfo)));
                }else {
                    loggingContext.put("command", generateContextString(ContextStack.create(wfStepInfo)));
                }

                if (step > -1) {
                    loggingContext.put("step", Integer.toString(step));
                }
            }
            return loggingContext;
        } else {
            return null;
        }
    }

    private String generateContextString(final ContextStack<WFStepContext> stack) {
        if (null != delegate) {
            return delegate.generateContextString(stack);
        }
        final String[] strings = new String[stack.size()];
        int i=0;
        for (final WFStepContext context : stack.stack()) {
            strings[i++] = makePrefix(context);
        }
        return StringUtils.join(strings, ":");
    }

    private String makePrefix(WFStepContext wfStepInfo) {
        if (null != delegate) {
            return delegate.makePrefix(wfStepInfo);
        }

        String type = wfStepInfo.getStepItem().getType();
        if (wfStepInfo.getStepItem() instanceof NodeStepExecutionItem) {
            NodeStepExecutionItem ns = (NodeStepExecutionItem) wfStepInfo.getStepItem();
            type += "-" + ns.getNodeStepType();
        }
        return wfStepInfo.getStep() + "-" + type;
    }


    public void beginWorkflowExecution(final StepExecutionContext executionContext, final WorkflowExecutionItem item) {
        if (null != delegate) {
            delegate.beginWorkflowExecution(executionContext, item);
            return;
        }
        if(null!=localStep.get()) {
            //within another workflow already, so push context onto stack
            WFStepContext info = localStep.get();
            if(null!= contextStack.get()) {
                contextStack.set(contextStack.get().copyPush(info));
            }else {
                contextStack.set(ContextStack.create(info));
            }
        }
        localStep.set(null);
        localNode.set(null);
        log(Constants.DEBUG_LEVEL,
            "[workflow] Begin execution: " + item.getType()+" context: "+contextStack.get()
        );
    }


    public void finishWorkflowExecution(final WorkflowExecutionResult result, final StepExecutionContext executionContext,
                                        final WorkflowExecutionItem item) {
        if (null != delegate) {
            delegate.finishWorkflowExecution(result, executionContext, item);
            return;
        }
        ContextStack<WFStepContext> stack = contextStack.get();
        if (null != stack ) {
            //pop any workflow context already on stack
            if (stack.size() > 0) {
                contextStack.set(stack.copyPop());
            } else {
                contextStack.set(null);
            }
        }
        localStep.set(null);
        localNode.set(null);
        log(Constants.DEBUG_LEVEL,
            "[workflow] Finish execution:  " + item.getType() + ": " + result
        );
    }

    public void beginWorkflowItem(final int step, final StepExecutionItem item) {
        if (null != delegate) {
            delegate.beginWorkflowItem(step, item);
            return;
        }
        localStep.set(new WFStepContext(item, step));
        log(Constants.DEBUG_LEVEL,
            "[workflow] Begin step: " + step + "," + item.getType()
        );
    }

    public void finishWorkflowItem(final int step, final StepExecutionItem item) {
        if (null != delegate) {
            delegate.finishWorkflowItem(step, item);
            return;
        }
        localStep.set(null);
        log(Constants.DEBUG_LEVEL,
            "[workflow] Finish step: " + step + "," + item.getType()
        );
    }

    @Override
    public ExecutionListenerOverride createOverride() {
        return new WorkflowExecutionListenerImpl(this);
    }
}
