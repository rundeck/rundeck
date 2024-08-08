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
import com.dtolabs.rundeck.core.execution.impl.local.LocalNodeExecutor;
import com.dtolabs.rundeck.core.execution.impl.local.NewLocalNodeExecutor;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import lombok.Getter;
import lombok.Setter;

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

    /**
     * Legacy profile for old behavior
     */
    @Getter
    static class LegacyProfile
            implements NodeExecutorServiceProfile
    {
        public static final String DEFAULT_REMOTE_PROVIDER = "sshj-ssh";
        private static final Map<String, Class<? extends NodeExecutor>> PRESET_PROVIDERS;

        static {
            PRESET_PROVIDERS =
                    Map.of(
                            LocalNodeExecutor.SERVICE_PROVIDER_TYPE,
                            LocalNodeExecutor.class,
                            NewLocalNodeExecutor.SERVICE_PROVIDER_TYPE,
                            NewLocalNodeExecutor.class
                    );
        }

        private final String defaultLocalProvider = LocalNodeExecutor.SERVICE_PROVIDER_TYPE;
        private final String defaultRemoteProvider = DEFAULT_REMOTE_PROVIDER;
        private final Map<String, Class<? extends NodeExecutor>> localRegistry = PRESET_PROVIDERS;
    }

    static final NodeExecutorServiceProfile LEGACY_PROFILE = new LegacyProfile();

    @Getter @Setter private NodeExecutorServiceProfile serviceProfile;

    public String getName() {
        return SERVICE_NAME;
    }

    public List<String> getBundledProviderNames() {
        return List.copyOf(getRegistryMap().keySet());
    }

    public NodeExecutorService(Framework framework, NodeExecutorServiceProfile serviceProfile) {
        super(framework, true);
        this.serviceProfile = serviceProfile;
    }

    public NodeExecutorService(Framework framework) {
        this(framework, LEGACY_PROFILE);
    }

    @Override
    protected Map<String, Class<? extends NodeExecutor>> getRegistryMap() {
        return serviceProfile.getLocalRegistry();
    }

    @Override
    public String getDefaultProviderNameForNodeAndProject(INodeEntry node, String project) {
        return getProviderNameForNode(
                framework.isLocalNode(node),
                framework.getProjectManager().loadProjectConfig(project)
        );
    }


    @Override
    public String getServiceProviderNodeAttributeForNode(INodeEntry node) {
        return getNodeAttributeForProvider(framework.isLocalNode(node));
    }
    public String getProviderNameForNode(
            final boolean localNode,
            final IRundeckProjectConfig loadProjectConfig
    )
    {
        if (localNode) {
            final String value = loadProjectConfig.getProperty(SERVICE_DEFAULT_LOCAL_PROVIDER_PROPERTY);
            return null != value ? value : serviceProfile.getDefaultLocalProvider();
        }
        final String value = loadProjectConfig.getProperty(SERVICE_DEFAULT_PROVIDER_PROPERTY);
        return null != value ? value : serviceProfile.getDefaultRemoteProvider();
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
        return DescribableServiceUtil.listDescriptions(this, true);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }
}
