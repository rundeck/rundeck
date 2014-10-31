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
* ScriptFileCommandBase.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/22/11 5:40 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.HasFailureHandler;

import java.io.InputStream;

/**
 * ScriptFileCommandBase is a base implementation that returns null for all accessors.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptFileCommandBase extends ScriptFileCommand implements HasFailureHandler {
    public String getScript() {
        return null;
    }

    public InputStream getScriptAsStream() {
        return null;
    }

    public String getServerScriptFilePath() {
        return null;
    }

    public String[] getArgs() {
        return new String[0];
    }

    public StepExecutionItem getFailureHandler() {
        return null;
    }

    public boolean isKeepgoingOnSuccess() {
        return false;
    }

    @Override
    public String getFileExtension() {
        return null;
    }

    public String getScriptInterpreter() {
        return null;
    }

    public boolean getInterpreterArgsQuoted() {
        return false;
    }
}
