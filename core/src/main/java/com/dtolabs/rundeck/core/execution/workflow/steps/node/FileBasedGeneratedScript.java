/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* FileBasedGeneratedScript.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/12/12 10:26 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.plugins.step.GeneratedScript;

import java.io.File;


/**
 * Used by script-based Remote Script Node Step plugin adapter
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
interface FileBasedGeneratedScript extends GeneratedScript{
    public File getScriptFile();
    public String getScriptInterpreter();
    public boolean isInterpreterArgsQuoted();
}
