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
* RemoteScriptNodeStepPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/19/12 6:04 PM
* 
*/
package com.dtolabs.rundeck.plugins.step;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;

import java.util.Map;


/**
 * The plugin interface for a Remote Script Node Step Plugin.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface RemoteScriptNodeStepPlugin {
    /**
     * Generate a full script or command string to execute on the remote node
     *
     * @param context       the step context
     * @param configuration Any configuration property values not otherwise applied to the plugin
     * @param entry         the Node
     *
     * @return the generated script
     * @throws NodeStepException if an error occurs
     */
    public GeneratedScript generateScript(final PluginStepContext context,
                                          final Map<String, Object> configuration,
                                          final INodeEntry entry) throws NodeStepException;
}
