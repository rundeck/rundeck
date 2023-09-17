/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig;
import com.dtolabs.rundeck.core.common.IServicesRegistration;
import com.dtolabs.rundeck.core.execution.impl.jsch.JschScpFileCopier;
import com.dtolabs.rundeck.core.execution.impl.local.LocalFileCopier;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.util.*;

/**
 * FileCopierService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class FileCopierService
    extends NodeSpecifiedService<FileCopier>
    implements DescribableService,
               PluggableProviderService<FileCopier>,
               JavaClassProviderLoadable<FileCopier>,
               ScriptPluginProviderLoadable<FileCopier>
{
    private static final String SERVICE_NAME = ServiceNameConstants.FileCopier;
    public static final String SERVICE_DEFAULT_PROVIDER_PROPERTY = "service." + SERVICE_NAME + ".default.provider";
    private static final String SERVICE_DEFAULT_LOCAL_PROVIDER_PROPERTY =
        "service." + SERVICE_NAME + ".default.local.provider";
    public static final String REMOTE_NODE_SERVICE_SPECIFIER_ATTRIBUTE = "file-copier";
    public static final String LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE = "local-file-copier";
    public static final String DEFAULT_REMOTE_PROVIDER = "sshj-scp";
    public static final String DEFAULT_LOCAL_PROVIDER = LocalFileCopier.SERVICE_PROVIDER_TYPE;
    private static final Map<String, Class<? extends FileCopier>> PRESET_PROVIDERS ;

    static {
        Map<String, Class<? extends FileCopier>> map = new HashMap<>();
        map.put(JschScpFileCopier.SERVICE_PROVIDER_TYPE, JschScpFileCopier.class);
        map.put(LocalFileCopier.SERVICE_PROVIDER_TYPE, LocalFileCopier.class);
        PRESET_PROVIDERS = Collections.unmodifiableMap(map);
    }

    public String getName() {
        return SERVICE_NAME;
    }

    public List<String> getBundledProviderNames() {
        return Collections.unmodifiableList(new ArrayList<>(registry.keySet()));
    }

    public FileCopierService(Framework framework) {
        super(framework, true);

        registry.putAll(PRESET_PROVIDERS);
    }

    public static boolean isRegistered(String name){
        return PRESET_PROVIDERS.containsKey(name);
    }
    @Override
    public String getDefaultProviderNameForNodeAndProject(INodeEntry node, String project) {
        return getProviderNameForNode(
                framework.isLocalNode(node),
                framework.getProjectManager().loadProjectConfig(project)
        );
    }

    public static String getProviderNameForNode(
            final boolean isLocal, final IRundeckProjectConfig iRundeckProjectConfig
    ) {
        if (isLocal) {
            final String value = iRundeckProjectConfig.getProperty( SERVICE_DEFAULT_LOCAL_PROVIDER_PROPERTY);
            return value != null ? value : DEFAULT_LOCAL_PROVIDER;
        } else {
            final String value = iRundeckProjectConfig.getProperty( SERVICE_DEFAULT_PROVIDER_PROPERTY);
            return value != null ? value : DEFAULT_REMOTE_PROVIDER;
        }
    }

    public static FileCopierService getInstanceForFramework(Framework framework,
                                                            final IServicesRegistration registration) {
        if (null == registration.getService(SERVICE_NAME)) {
            final FileCopierService service = new FileCopierService(framework);
            registration.setService(SERVICE_NAME, service);
            return service;
        }
        return (FileCopierService) registration.getService(SERVICE_NAME);
    }

    @Override
    public String getServiceProviderNodeAttributeForNode(INodeEntry node) {
        return getNodeAttributeForProvider(framework.isLocalNode(node));
    }

    public static String getNodeAttributeForProvider(final boolean isLocal) {
        if (isLocal) {
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
