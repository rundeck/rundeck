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
* RemoteScriptNodeStepPluginService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/19/12 6:03 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.FrameworkPluggableProviderService;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin;


/**
 * RemoteScriptNodeStepPluginService manages provider plugins of type {@link RemoteScriptNodeStepPlugin}, and uses the
 * service name of {@link ServiceNameConstants#RemoteScriptNodeStep}
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class RemoteScriptNodeStepPluginService extends FrameworkPluggableProviderService<RemoteScriptNodeStepPlugin>
    implements DescribableService {

    RemoteScriptNodeStepPluginService(final String name, final Framework framework) {
        super(name, framework, RemoteScriptNodeStepPlugin.class);
    }

    @Override
    public boolean isScriptPluggable() {
        return true;
    }

    @Override
    public RemoteScriptNodeStepPlugin createScriptProviderInstance(ScriptPluginProvider provider)
        throws PluginException {
        ScriptBasedRemoteScriptNodeStepPlugin.validateScriptPlugin(provider);
        return new ScriptBasedRemoteScriptNodeStepPlugin(provider, getFramework());
    }
}
