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
* FileCopierService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:05 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.impl.jsch.JschScpFileCopier;
import com.dtolabs.rundeck.core.execution.impl.local.LocalFileCopier;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FileCopierService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class FileCopierService extends NodeSpecifiedService<FileCopier> implements DescribableService {
    private static final String SERVICE_NAME = ServiceNameConstants.FileCopier;
    public static final String SERVICE_DEFAULT_PROVIDER_PROPERTY = "service." + SERVICE_NAME + ".default.provider";
    private static final String SERVICE_DEFAULT_LOCAL_PROVIDER_PROPERTY =
        "service." + SERVICE_NAME + ".default.local.provider";
    public static final String REMOTE_NODE_SERVICE_SPECIFIER_ATTRIBUTE = "file-copier";
    public static final String LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE = "local-file-copier";
    public static final String DEFAULT_REMOTE_PROVIDER = JschScpFileCopier.SERVICE_PROVIDER_TYPE;
    public static final String DEFAULT_LOCAL_PROVIDER = LocalFileCopier.SERVICE_PROVIDER_TYPE;

    public String getName() {
        return SERVICE_NAME;
    }

    public List<String> getBundledProviderNames() {
        return Collections.unmodifiableList(new ArrayList<String>(registry.keySet()));
    }

    FileCopierService(Framework framework) {
        super(framework);

        //TODO: use plugin framework to configure available FileCopier implementations.
        registry.put(JschScpFileCopier.SERVICE_PROVIDER_TYPE, JschScpFileCopier.class);
        registry.put(LocalFileCopier.SERVICE_PROVIDER_TYPE, LocalFileCopier.class);

    }

    @Override
    protected String getDefaultProviderNameForNodeAndProject(INodeEntry node, String project) {
        if (framework.isLocalNode(node)) {
            final String value = framework.getProjectProperty(project, SERVICE_DEFAULT_LOCAL_PROVIDER_PROPERTY);
            return value != null ? value : DEFAULT_LOCAL_PROVIDER;
        } else {
            final String value = framework.getProjectProperty(project, SERVICE_DEFAULT_PROVIDER_PROPERTY);
            return value != null ? value : DEFAULT_REMOTE_PROVIDER;
        }
    }

    public static FileCopierService getInstanceForFramework(Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final FileCopierService service = new FileCopierService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (FileCopierService) framework.getService(SERVICE_NAME);
    }

    @Override
    protected String getServiceProviderNodeAttributeForNode(INodeEntry node) {
        if (framework.isLocalNode(node)) {
            return LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE;
        }
        return REMOTE_NODE_SERVICE_SPECIFIER_ATTRIBUTE;
    }

    public boolean isValidProviderClass(final Class clazz) {
        return FileCopier.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    @Override
    public <X extends FileCopier> FileCopier createProviderInstance(Class<X> clazz, String name) throws PluginException, ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    public boolean isScriptPluggable() {
        return true;
    }

    public FileCopier createScriptProviderInstance(final ScriptPluginProvider provider) throws PluginException {
        ScriptPluginFileCopier.validateScriptPlugin(provider);
        return new ScriptPluginFileCopier(provider, framework);
    }

    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this, false);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }
}
