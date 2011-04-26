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
* AntSupport.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/28/11 1:57 PM
* 
*/
package com.dtolabs.rundeck.core.execution.impl.common;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.ExecutionListenerBuildLogger;
import com.dtolabs.rundeck.core.execution.commands.ExecCommandInterpreter;
import org.apache.tools.ant.Project;

/**
 * AntSupport is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class AntSupport {
    public static void addAntBuildListener(ExecutionListener listener, Project project) {
        final ExecutionListenerBuildLogger listener1 = new ExecutionListenerBuildLogger(listener);
        project.addBuildListener(listener1);
    }
}
