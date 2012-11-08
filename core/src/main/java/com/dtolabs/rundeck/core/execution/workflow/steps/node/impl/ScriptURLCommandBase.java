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
* ScriptURLCommandBase.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 5/2/12 2:58 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.execution.HasFailureHandler;
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;


/**
 * ScriptURLCommandBase base implementation of ScriptURLCommandExecutionItem that defines the getType() method
 * to return the correct service type.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class ScriptURLCommandBase implements ScriptURLCommandExecutionItem,HasFailureHandler,NodeStepExecutionItem {
    public String getNodeStepType() {
        return ScriptURLNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME;
    }

    public String getType() {
        return NodeDispatchStepExecutor.STEP_EXECUTION_TYPE;
    }
}
