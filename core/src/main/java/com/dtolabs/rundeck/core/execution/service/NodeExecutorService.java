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
* CommandExecutorFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 3:28 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig;
import com.dtolabs.rundeck.core.common.IServicesRegistration;
import com.dtolabs.rundeck.core.execution.impl.jsch.JschNodeExecutor;
import com.dtolabs.rundeck.core.execution.impl.local.LocalNodeExecutor;
import com.dtolabs.rundeck.core.execution.impl.local.NewLocalNodeExecutor;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.util.*;

/**
 * CommandExecutorFactory is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeExecutorService
    extends NodeSpecifiedService<NodeExecutor>
    implements DescribableService,
               PluggableProviderService<NodeExecutor>,
               JavaClassProviderLoadable<NodeExecutor>,
               ScriptPluginProviderLoadable<NodeExecutor>
{
    private static final String SERVICE_NAME = ServiceNameConstants.NodeExecutor;
    public static final String SERVICE_DEFAULT_PROVIDER_PROPERTY = "service." + SERVICE_NAME + ".default.provider";
    private static final String SERVICE_DEFAULT_LOCAL_PROVIDER_PROPERTY =
        "service." + SERVICE_NAME + ".default.local.provider";
    public static final String NODE_SERVICE_SPECIFIER_ATTRIBUTE = "node-executor";
    public static final String LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE = "local-node-executor";
    public static final String DEFAULT_LOCAL_PROVIDER = LocalNodeExecutor.SERVICE_PROVIDER_TYPE;
    public static final String DEFAULT_REMOTE_PROVIDER = JschNodeExecutor.SERVICE_PROVIDER_TYPE;
    private static final Map<String, Class<? extends NodeExecutor>> PRESET_PROVIDERS ;

    static {
        Map<String, Class<? extends NodeExecutor>> map = new HashMap<>();
        map.put(JschNodeExecutor.SERVICE_PROVIDER_TYPE, JschNodeExecutor.class);
        map.put(LocalNodeExecutor.SERVICE_PROVIDER_TYPE, LocalNodeExecutor.class);
        map.put(NewLocalNodeExecutor.SERVICE_PROVIDER_TYPE, NewLocalNodeExecutor.class);
        PRESET_PROVIDERS = Collections.unmodifiableMap(map);
    }

    public String getName() {
        return SERVICE_NAME;
    }

    public List<String> getBundledProviderNames() {
        return Collections.unmodifiableList(new ArrayList<>(registry.keySet()));
    }
    public NodeExecutorService(Framework framework) {
        super(framework,true);

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

    public static NodeExecutorService getInstanceForFramework(final Framework framework,
                                                              final IServicesRegistration registration) {
        if (null == registration.getService(SERVICE_NAME)) {
            final NodeExecutorService service = new NodeExecutorService(framework);
            registration.setService(SERVICE_NAME, service);
            return service;
        }
        return (NodeExecutorService) registration.getService(SERVICE_NAME);
    }

    @Override
    public String getServiceProviderNodeAttributeForNode(INodeEntry node) {
        return getNodeAttributeForProvider(framework.isLocalNode(node));
    }
    public static String getProviderNameForNode(
            final boolean localNode,
            final IRundeckProjectConfig loadProjectConfig
    )
    {
        if (localNode) {
            final String value = loadProjectConfig.getProperty( SERVICE_DEFAULT_LOCAL_PROVIDER_PROPERTY);
            return null != value ? value : DEFAULT_LOCAL_PROVIDER;
        }
        final String value = loadProjectConfig.getProperty( SERVICE_DEFAULT_PROVIDER_PROPERTY);
        return null != value ? value : DEFAULT_REMOTE_PROVIDER;
    }

    public static String getNodeAttributeForProvider(final boolean localNode) {
        if (localNode) {
            return LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE;
        }
        return NODE_SERVICE_SPECIFIER_ATTRIBUTE;
    }

    public boolean isValidProviderClass(final Class clazz) {
        return NodeExecutor.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    @Override
    public <X extends NodeExecutor> NodeExecutor createProviderInstance(Class<X> clazz, String name) throws PluginException, ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    public NodeExecutor createScriptProviderInstance(final ScriptPluginProvider provider) throws PluginException {
        ScriptPluginNodeExecutor.validateScriptPlugin(provider);
        return new ScriptPluginNodeExecutor(provider,framework);
    }

    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this, false);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }
}
