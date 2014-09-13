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
* WorkflowExecutionItem.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 16, 2010 9:43:26 AM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.StepExecutionItem;


/**
 * WorkflowExecutionItem is an execution item representing an entire workflow.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface WorkflowExecutionItem extends StepExecutionItem {
    /**
     * Node first strategy name
     */
    public static final String NODE_FIRST = "node-first";
    /**
     * Step first strategy nae
     */
    public static final String STEP_FIRST = "step-first";
    /**
     * Parallel strategy name
     */
    public static final String PARALLEL = "parallel";
    /**
     * Provider name for node first provider implementation
     */
    public static final String COMMAND_TYPE_NODE_FIRST = "rundeck-workflow-" + NODE_FIRST;
    /**
     * Provider name for step first provider implementation
     */
    public static final String COMMAND_TYPE_STEP_FIRST = "rundeck-workflow-" + STEP_FIRST;
    /**
     * Provider name for parallel provider implementation
     */
    public static final String COMMAND_TYPE_PARALLEL = "rundeck-workflow-" + PARALLEL;
    /**
     * Return the workflow definition
     * @return workflow
     */
    public IWorkflow getWorkflow();

}
