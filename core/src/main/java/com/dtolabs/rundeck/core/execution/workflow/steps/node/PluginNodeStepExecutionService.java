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
* PluginNodeStepExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/12/12 4:59 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;

import java.util.*;


/**
 * PluginNodeStepExecutionService can load NodeStepPlugin providers
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class PluginNodeStepExecutionService extends BasePluggableProviderService<NodeStepPlugin>
    implements DescribableService {

    public PluginNodeStepExecutionService(final Framework framework) {
        super(framework);
    }

    public boolean isValidProviderClass(Class clazz) {

        return NodeStepPlugin.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    public NodeStepPlugin createProviderInstance(Class<NodeStepPlugin> clazz, String name) throws
                                                                                           PluginException,
                                                                                           ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    public boolean isScriptPluggable() {
        //TODO: add script plugins
        return false;
    }

    public NodeStepPlugin createScriptProviderInstance(ScriptPluginProvider provider) throws PluginException {
        return null;
    }

    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }


    public String getName() {
        return NodeStepExecutionService.SERVICE_NAME;
    }
}
