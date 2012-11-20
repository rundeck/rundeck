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
* ScriptGeneratorNodeStepPluginService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/19/12 6:03 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.AdapterService;
import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.step.ScriptGeneratorNodeStepPlugin;

import java.util.*;


/**
 * ScriptGeneratorNodeStepPluginService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptGeneratorNodeStepPluginService extends BasePluggableProviderService<ScriptGeneratorNodeStepPlugin>
    implements DescribableService {

    public static final String SERVICE_NAME = "GeneratorNodeStepExecutor";

    public ScriptGeneratorNodeStepPluginService(final Framework framework) {
        super(framework);
    }

    @Override
    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this);
    }

    @Override
    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }


    @Override
    public boolean isValidProviderClass(Class clazz) {
        return ScriptGeneratorNodeStepPlugin.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    @Override
    public ScriptGeneratorNodeStepPlugin createProviderInstance(Class<ScriptGeneratorNodeStepPlugin> clazz,
                                                                String name)
        throws PluginException, ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    @Override
    public boolean isScriptPluggable() {
        return false;
    }

    @Override
    public ScriptGeneratorNodeStepPlugin createScriptProviderInstance(ScriptPluginProvider provider)
        throws PluginException {
        //TODO: implement
        return null;
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }


}
