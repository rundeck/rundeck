/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* CommandExecItem.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:35 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.execution.BaseExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor;

import java.util.Map;


/**
 * CommandExecItem is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class ExecCommand extends BaseExecutionItem implements ExecCommandExecutionItem {
    public static final String SERVICE_IMPLEMENTATION_NAME = "exec";
    public String getNodeStepType() {
        return SERVICE_IMPLEMENTATION_NAME;
    }

    public String getType() {
        return NodeDispatchStepExecutor.STEP_EXECUTION_TYPE;
    }
}
