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
import com.dtolabs.rundeck.core.execution.commands.InterpreterResult;

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
    private InheritableThreadLocal<Deque<WFExecContext>> localContextStack =
        new InheritableThreadLocal<Deque<WFExecContext>>();

    private Deque<WFExecContext> getLocalContextStack() {
        if (null == localContextStack.get()) {
            localContextStack.set(new ArrayDeque<WFExecContext>());
        }
        return localContextStack.get();
    }

    private void pushContext(final WFExecContext context) {
        getLocalContextStack().push(context);
    }

    private WFExecContext popContext() {
        return getLocalContextStack().pollFirst();
    }

    private WFExecContext peekContext() {
        return getLocalContextStack().peekFirst();
    }

    public WorkflowExecutionListenerImpl(final FailedNodesListener failedNodesListener,
                                         final ContextLogger logger, final boolean terse, final String logFormat) {
        super(failedNodesListener, logger, terse, logFormat);
    }

    @Override
    public void beginInterpretCommand(final ExecutionContext context, final ExecutionItem item, final INodeEntry node) {
        super.beginInterpretCommand(context, item, node);
        final WFExecContext wfExecContext = peekContext();
        if (null != wfExecContext) {
            wfExecContext.getStepContext().setNode(node);
        }
        context.getExecutionListener().log(Constants.DEBUG_LEVEL,
            "beginInterpretCommand(" + node.getNodename() + "): " + item.getType() + ": " + item);
    }

    @Override
    public void finishInterpretCommand(final InterpreterResult result, final ExecutionContext context,
                                       final ExecutionItem item, final INodeEntry node) {
        super.finishInterpretCommand(result, context, item, node);
        final WFExecContext wfExecContext = peekContext();
        if (null != wfExecContext) {
            wfExecContext.getStepContext().clearNode();
        }
        log(Constants.DEBUG_LEVEL,
            "finishInterpretCommand(" + node.getNodename() + "): " + item.getType() + ": " + result);
    }


    @Override
    public Map<String, String> getLoggingContext() {
        final WFExecContext wfExecContext = peekContext();
        if (null != wfExecContext) {
            return wfExecContext.getLoggingContext();
        } else {
            return null;
        }
    }

    public void beginWorkflowExecution(final ExecutionContext executionContext, final WorkflowExecutionItem item) {
        pushContext(new WFExecContext(executionContext, item));
        log(Constants.DEBUG_LEVEL,
            "beginWorkflowExecution(): " + item.getType() /*+ "; " + peekContext()*/);
    }


    public void finishWorkflowExecution(final WorkflowExecutionResult result, final ExecutionContext executionContext,
                                        final WorkflowExecutionItem item) {
        popContext();
        log(Constants.DEBUG_LEVEL,
            "finishWorkflowExecution(): " + item.getType() + ": " + result /* + "; " + peekContext()*/);
    }

    public void beginWorkflowItem(final int step, final ExecutionItem item) {
        final WFExecContext wfExecContext = peekContext();
        if (null != wfExecContext) {
            wfExecContext.getStepContext().setStep(step, item);
        }
        log(Constants.DEBUG_LEVEL,
            "beginWorkflowItem(" + step + "," + item.getType() + ")"/* + "; " + peekContext()*/);
    }

    public void finishWorkflowItem(final int step, final ExecutionItem item) {
        final WFExecContext wfExecContext = peekContext();
        if (null != wfExecContext) {
            wfExecContext.getStepContext().clearStep();
        }
        log(Constants.DEBUG_LEVEL,
            "finishWorkflowItem(" + step + "," + item.getType() + ")" /*+ "; " + peekContext()*/);
    }
}
