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
package com.dtolabs.rundeck.core.execution.commands;

import java.io.InputStream;
import java.util.*;

/**
 * ScriptFileCommandBase is a base implementation that returns null for all accessors.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptFileCommandBase extends ScriptFileCommand {
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
}
