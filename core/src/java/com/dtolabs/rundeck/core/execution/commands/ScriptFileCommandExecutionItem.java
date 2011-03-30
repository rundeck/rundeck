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
* ScriptFileCommandExecutionItem.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/22/11 5:43 PM
* 
*/
package com.dtolabs.rundeck.core.execution.commands;

import com.dtolabs.rundeck.core.execution.ExecutionItem;

import java.io.InputStream;

/**
 * ScriptFileCommandExecutionItem is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ScriptFileCommandExecutionItem extends ExecutionItem {

    /**
     * Get the full script
     *
     * @return the script string
     */
    public abstract String getScript();

    /**
     * Get an InputStream that can provide the full script
     *
     * @return the inputstream
     *
     * @throws java.io.IOException if an error occurs reading or getting the input stream
     */
    public abstract InputStream getScriptAsStream();

    /**
     * Get the server-local script path
     *
     * @return server-side script path
     */
    public abstract String getServerScriptFilePath();
    
    /**
     * Return arguments to the script
     */
    public abstract String[] getArgs();
}
