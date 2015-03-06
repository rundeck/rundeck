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
* WorkflowExecutionServiceThread.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/29/11 12:00 PM
* 
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutor;

/**
 * WorkflowExecutionServiceThread is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class WorkflowExecutionServiceThread extends ServiceThreadBase {
    WorkflowExecutionService weservice;
    WorkflowExecutionItem weitem;
    private StepExecutionContext context;
    private WorkflowExecutionResult result;

    public WorkflowExecutionServiceThread(WorkflowExecutionService eservice, WorkflowExecutionItem eitem, StepExecutionContext econtext) {
        this.weservice = eservice;
        this.weitem = eitem;
        this.context = econtext;
    }

    public void run() {
        if (null == this.weservice || null == this.weitem || null == context) {
            throw new IllegalStateException("project or execution detail not instantiated");
        }
        try {
            final WorkflowExecutor executorForItem = weservice.getExecutorForItem(weitem);
            setResult(executorForItem.executeWorkflow(context,weitem));
            success = getResult().isSuccess();
            if (null != getResult().getException()) {
                thrown = getResult().getException();
            }
            resultObject = getResult();
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            thrown = e;
        }
    }

    public StepExecutionContext getContext() {
        return context;
    }

    public WorkflowExecutionResult getResult() {
        return result;
    }

    public void setResult(final WorkflowExecutionResult result) {
        this.result = result;
    }
}
