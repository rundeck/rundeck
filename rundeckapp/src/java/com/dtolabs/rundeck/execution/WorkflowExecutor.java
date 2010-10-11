/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* WorkflowExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 16, 2010 9:42:03 AM
* $Id$
*/
package com.dtolabs.rundeck.execution;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.*;

/**
 * WorkflowExecutor implements {@link com.dtolabs.rundeck.core.execution.Executor} and is registered to the {@link
 * com.dtolabs.rundeck.core.execution.ExecutionServiceFactory} to handle {@link com.dtolabs.rundeck.execution.WorkflowExecutionItem}
 * instances.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class WorkflowExecutor implements Executor {

    /**
     * Constructor used by BaseExecutionService
     *
     * @param framework        framework
     * @param executionService execservice
     */
    public WorkflowExecutor() {
    }

    public ExecutionResult executeItem(final ExecutionItem executionItem,
                                       final ExecutionListener executionListener,
                                       final ExecutionService executionService,final Framework framework) throws
        ExecutionException {
        if (!(executionItem instanceof WorkflowExecutionItem)) {
            throw new ExecutionException("Incorrect item type: " + executionItem.getClass().getName());
        }
        final WorkflowExecutionItem item = (WorkflowExecutionItem) executionItem;
        return executeWorkflow(item, executionListener, executionService, framework);
    }

    /**
     * Execute the workflow item with the listener
     *
     * @param item     item
     * @param listener listener
     *
     * @return execution result
     */
    public ExecutionResult executeWorkflow(final WorkflowExecutionItem item, final ExecutionListener listener,
                                           final ExecutionService executionService, final Framework framework) throws
        WorkflowAction.WorkflowFailureException {

        final WorkflowAction workflowAction = new WorkflowAction(framework, executionService, item, listener);
        return workflowAction.executeWorkflow();
    }


}
