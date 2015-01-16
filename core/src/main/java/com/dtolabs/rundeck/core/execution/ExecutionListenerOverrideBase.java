/*
 * Copyright 2012 DTO Solutions, Inc. (http://dtosolutions.com)
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
* ExecutionListenerOverrideBase.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 6/12/12 2:42 PM
* 
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * ExecutionListenerOverrideBase is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class ExecutionListenerOverrideBase implements ExecutionListenerOverride {
    private FailedNodesListener failedNodesListener;
    private boolean terse;
    private String logFormat;
    private ExecutionListenerOverrideBase delegate;

    protected ExecutionListenerOverrideBase(ExecutionListenerOverrideBase delegate) {
        this.delegate = delegate;
    }

    public ExecutionListenerOverrideBase(
        final FailedNodesListener failedNodesListener,
        final boolean terse,
        final String logFormat
    ) {

        this.failedNodesListener = failedNodesListener;
        this.terse = terse;
        this.logFormat = logFormat;
    }

    /**
     * Method should be overridden
     * @return appropriate logging context data
     */
    public Map<String, String> getLoggingContext() {
        if (null != delegate) {
            return delegate.getLoggingContext();
        }
        return null;
    }


    public void beginStepExecution(StepExecutor executor,StepExecutionContext context, StepExecutionItem item) {
        if (null != delegate) {
            delegate.beginStepExecution(executor,context, item);
        }
    }

    public void finishStepExecution(StepExecutor executor, StatusResult result, StepExecutionContext context, StepExecutionItem item) {
        if (null != delegate) {
            delegate.finishStepExecution(executor,result, context, item);
        }
    }

    public void beginNodeExecution(ExecutionContext context, String[] command, INodeEntry node) {
        if (null != delegate) {
            delegate.beginNodeExecution(context, command, node);
        }
    }

    public void finishNodeExecution(NodeExecutorResult result, ExecutionContext context, String[] command,
                                    INodeEntry node) {
        if (null != delegate) {
            delegate.finishNodeExecution(result, context, command, node);
        }
    }

    public void beginNodeDispatch(ExecutionContext context, StepExecutionItem item) {
        if (null != delegate) {
            delegate.beginNodeDispatch(context, item);
        }
    }

    public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, StepExecutionItem item) {
        if (null != delegate) {
            delegate.finishNodeDispatch(result, context, item);
        }
    }

    public void beginNodeDispatch(ExecutionContext context, Dispatchable item) {
        if (null != delegate) {
            delegate.beginNodeDispatch(context, item);
        }
    }

    public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, Dispatchable item) {
        if (null != delegate) {
            delegate.finishNodeDispatch(result, context, item);
        }
    }

    public void beginFileCopyFileStream(ExecutionContext context, InputStream input, INodeEntry node) {
        if (null != delegate) {
            delegate.beginFileCopyFileStream(context, input, node);
        }
    }

    public void beginFileCopyFile(ExecutionContext context, File input, INodeEntry node) {
        if (null != delegate) {
            delegate.beginFileCopyFile(context, input, node);
        }
    }

    public void beginFileCopyScriptContent(ExecutionContext context, String input, INodeEntry node) {
        if (null != delegate) {
            delegate.beginFileCopyScriptContent(context, input, node);
        }
    }

    public void finishFileCopy(String result, ExecutionContext context, INodeEntry node) {
        if (null != delegate) {
            delegate.finishFileCopy(result, context, node);
        }
    }

    public void beginExecuteNodeStep(ExecutionContext context, NodeStepExecutionItem item, INodeEntry node) {
        if (null != delegate) {
            delegate.beginExecuteNodeStep(context, item, node);
        }
    }

    public void finishExecuteNodeStep(NodeStepResult result, ExecutionContext context, StepExecutionItem item,
                                      INodeEntry node) {
        if (null != delegate) {
            delegate.finishExecuteNodeStep(result, context, item, node);
        }
    }

    public FailedNodesListener getFailedNodesListener() {
        if (null != failedNodesListener) {
            return failedNodesListener;
        } else if (null != delegate) {
            return delegate.getFailedNodesListener();
        }
        return failedNodesListener;
    }


    public boolean isTerse() {
        if (null != delegate) {
            return delegate.isTerse();
        }
        return terse;
    }

    public String getLogFormat() {
        if (null != logFormat) {
            return logFormat;
        }
        if (null != delegate) {
            return delegate.getLogFormat();
        }
        return logFormat;
    }

    public void setFailedNodesListener(FailedNodesListener failedNodesListener) {
        this.failedNodesListener = failedNodesListener;
    }

}
