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
* WFExecContext.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 9:03 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.ExecutionContext;

import java.util.*;

/**
 * WFExecContext is used by WorkflowExecutionListenerImpl to store workflow context.  It should be used as an
 * InheritableThreadLocal.  Internally it maintains an InheritableThreadLocal object for step-specific context
 * information.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class WFExecContext {
    private ExecutionContext context;
    private WorkflowExecutionItem workflowItem;
    private InheritableThreadLocal<WFStepContext> threadStepContext = new InheritableThreadLocal<WFStepContext>();

    WFExecContext(final ExecutionContext context, final WorkflowExecutionItem workflowItem) {
        this.context = context;
        this.workflowItem = workflowItem;
    }

    public WFStepContext getStepContext() {
        if (null == threadStepContext.get()) {
            threadStepContext.set(new WFStepContext());
        }
        return threadStepContext.get();
    }

    private void clearStep() {
        threadStepContext.set(null);
    }

    public Map<String, String> getLoggingContext() {
        final WFStepContext stepContext = getStepContext();
        return stepContext.getContext();
    }

    @Override
    public String toString() {
        return "WFExecContext{" +
               "workflowItem=" + workflowItem +
               ", context=" + context +
               ", stepContext=" + getStepContext() +
               '}';
    }
}
