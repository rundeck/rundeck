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
* NodeStepPluginService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/12/12 4:59 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;


/**
 * NodeStepPluginService can load NodeStepPlugin providers, and also supports script-based plugin providers.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class NodeStepPluginService extends FrameworkPluggableProviderService<NodeStepPlugin> implements DescribableService {

    /**
     * Create the service with a given name
     */
    public NodeStepPluginService(final String name, final Framework framework) {
        super(name, framework, NodeStepPlugin.class);
    }

    public boolean isScriptPluggable() {
        return true;
    }

    public NodeStepPlugin createScriptProviderInstance(ScriptPluginProvider provider) throws PluginException {
        ScriptPluginNodeStepPlugin.validateScriptPlugin(provider);
        return new ScriptPluginNodeStepPlugin(provider, getFramework());
    }

}
