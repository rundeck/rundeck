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
* WorkflowExecutionResult.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/23/11 2:06 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.commands.InterpreterResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * WorkflowExecutionResult contains a map of Node names to workflow item results, and
 * node names to failure messages.
 *
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface WorkflowExecutionResult extends StatusResult {
    /**
     * Return map of workflow item results, keyed by node name with a list of results ordered by workflow step
     */
    public Map<String,List<StatusResult>> getResultSet();
    /**
     * Return map of workflow item failures, keyed by node name
     */
    public Map<String, Collection<String>> getFailureMessages();
    public Exception getException();
}
