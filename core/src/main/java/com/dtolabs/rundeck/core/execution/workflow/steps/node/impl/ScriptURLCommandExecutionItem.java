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
* ScriptURLCommandExecutionItem.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 5/2/12 2:40 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.execution.HandlerExecutionItem;


/**
 * ScriptURLCommandExecutionItem is used by the {@link ScriptURLNodeStepExecutor} and represents
 * an execution item to run a script downloaded from a provided URL.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ScriptURLCommandExecutionItem extends HandlerExecutionItem {
    /**
     * @return the URL to get the script from, which may include data context references
     */
    public String getURLString();
    /**
     * @return arguments to the script
     */
    public String[] getArgs();

    /**
     * Get the server-local script path
     *
     * @return server-side script path
     */
    public abstract String getScriptInterpreter();

    /**
     * Get the temp file extension
     *
     * @return file extension
     */
    public abstract String getFileExtension();

    /**
     * Get the server-local script path
     *
     * @return server-side script path
     */
    public abstract boolean getInterpreterArgsQuoted();
}
