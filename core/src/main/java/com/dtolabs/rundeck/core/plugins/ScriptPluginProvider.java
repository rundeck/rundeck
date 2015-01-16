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
* ScriptPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/4/11 6:25 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import java.io.File;
import java.util.Map;

/**
 * ScriptPluginProvider defines scripted plugin provider details
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ScriptPluginProvider {
    /**
     * @return provider name
     */
    public String getName();

    /**
     * @return service name
     */
    public String getService();

    /**
     * @return jar file containing the plugin
     */
    public File getArchiveFile();

    /**
     * @return directory containing the expanded contents
     */
    public File getContentsBasedir();

    /**
     * @return script args to pass to the file
     */
    public String getScriptArgs();

    /**
     * @return script file to execute
     */
    public File getScriptFile();
    /**
     * @return any interpreter specification to run the script
     */
    public String getScriptInterpreter();
    /**
     * @return true if the script file and args should be passed as a single argument to the interpreter, default false.
     */
    public boolean getInterpreterArgsQuoted();
    /**
     * @return any interpreter specification to run the script
     */
    public Map<String,Object> getMetadata();
}
