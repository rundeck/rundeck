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
    private InheritableThreadLocal<WFStepContext> localStep = new InheritableThreadLocal<WFStepContext>();
    private InheritableThreadLocal<INodeEntry> localNode = new InheritableThreadLocal<INodeEntry>();
    private InheritableThreadLocal<String> contextPrefix = new InheritableThreadLocal<String>();


    public WorkflowExecutionListenerImpl(final FailedNodesListener failedNodesListener,
                                         final ContextLogger logger, final boolean terse, final String logFormat) {
        super(failedNodesListener, logger, terse, logFormat);
    }

    @Override
    public void beginInterpretCommand(final ExecutionContext context, final ExecutionItem item, final INodeEntry node) {
        super.beginInterpretCommand(context, item, node);
        localNode.set(node);
        context.getExecutionListener().log(Constants.DEBUG_LEVEL,
            "beginInterpretCommand(" + node.getNodename() + "): " + item.getType() + ": " + item);
    }

    @Override
    public void finishInterpretCommand(final InterpreterResult result, final ExecutionContext context,
                                       final ExecutionItem item, final INodeEntry node) {
        super.finishInterpretCommand(result, context, item, node);
        localNode.set(null);
        log(Constants.DEBUG_LEVEL,
            "finishInterpretCommand(" + node.getNodename() + "): " + item.getType() + ": " + result);
    }


    @Override
    public Map<String, String> getLoggingContext() {

        if (null != localStep.get() || null != localNode.get()) {
            final HashMap<String, String> loggingContext = new HashMap<String, String>();
            if (null != localNode.get()) {
                final INodeEntry node = localNode.get();
                loggingContext.put("node", node.getNodename());
                loggingContext.put("user", node.extractUserName());
            }
            if (null != localStep.get()) {
                final WFStepContext wfStepInfo = localStep.get();
                final int step = wfStepInfo.step;
                final String s = makePrefix(wfStepInfo);
                if(null!= contextPrefix.get()) {
                    loggingContext.put("command", contextPrefix.get() + ":" + s);
                }else{
                    loggingContext.put("command", s);
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

    private String makePrefix(WFStepContext wfStepInfo) {
        return wfStepInfo.step + "-" + wfStepInfo.stepItem.getType();
    }


    public void beginWorkflowExecution(final ExecutionContext executionContext, final WorkflowExecutionItem item) {
        if(null!=localStep.get()) {
            String prefix = makePrefix(localStep.get());
            if(null!= contextPrefix.get()) {
                contextPrefix.set(contextPrefix.get() + ":" + prefix);
            }else {
                contextPrefix.set(prefix);
            }
        }
        localStep.set(null);
        localNode.set(null);
        log(Constants.DEBUG_LEVEL,
            "[workflow] Begin execution: " + item.getType()
        );
    }


    public void finishWorkflowExecution(final WorkflowExecutionResult result, final ExecutionContext executionContext,
                                        final WorkflowExecutionItem item) {
        localStep.set(null);
        localNode.set(null);
        log(Constants.DEBUG_LEVEL,
            "[workflow] Finish execution:  " + item.getType() + ": " + result
        );
    }

    public void beginWorkflowItem(final int step, final ExecutionItem item) {
        localStep.set(new WFStepContext(item, step));
        log(Constants.DEBUG_LEVEL,
            "[workflow] Begin step: " + step + "," + item.getType()
        );
    }

    public void finishWorkflowItem(final int step, final ExecutionItem item) {
        localStep.set(null);
        log(Constants.DEBUG_LEVEL,
            "[workflow] Finish step: " + step + "," + item.getType()
        );
    }
}
