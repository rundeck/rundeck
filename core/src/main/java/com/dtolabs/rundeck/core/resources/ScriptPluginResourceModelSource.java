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
* ScriptPluginResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/2/11 12:01 PM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.execution.proxy.ProxySecretBundleCreator;
import com.dtolabs.rundeck.core.execution.proxy.SecretBundle;
import com.dtolabs.rundeck.core.plugins.ScriptDataContextUtil;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.Configurable;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.storage.StorageTree;
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree;
import org.rundeck.app.spi.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * ScriptPluginResourceModelSource provides a ResourceModelSource from a ScriptPluginProvider.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptPluginResourceModelSource implements ResourceModelSource, Configurable, ProxySecretBundleCreator {
    static        Logger               logger = LoggerFactory.getLogger(ScriptPluginResourceModelSource.class.getName());
    final         ScriptPluginProvider provider;
    final private Framework            framework;
    Properties configuration;
    private String format;
    private String project;
    private ScriptPluginResourceModelSourceFactory factory;
    private Map<String, Map<String, String>> executionDataContext;
    Map<String, String> configData;

    public ScriptPluginResourceModelSource(final ScriptPluginProvider provider, final Framework framework, final
    ScriptPluginResourceModelSourceFactory factory) {
        this.provider = provider;
        this.framework = framework;
        this.factory = factory;
        final Object o = provider.getMetadata().get(ScriptPluginResourceModelSourceFactory.RESOURCE_FORMAT_PROP);
        if (o instanceof String) {
            this.format = (String) o;
        }
    }

    public INodeSet getNodes() throws ResourceModelSourceException {
        try {
            return ScriptResourceUtil.executeScript(
                provider.getScriptFile(),
                provider.getScriptArgs(),
                provider.getScriptArgsArray(),
                provider.getScriptInterpreter(),
                provider.getName(),
                executionDataContext,
                format,
                framework,
                project,
                logger,
                provider.getInterpreterArgsQuoted());
        } catch (ResourceModelSourceException e) {
            throw new ResourceModelSourceException(
                "failed to execute: " + provider.getScriptFile() + ": " + e.getMessage(), e);
        }
    }

    public void configure(final Properties configuration) throws ConfigurationException {
        this.configuration = configuration;
        if (!configuration.containsKey("project")) {
            throw new ConfigurationException("project is required");
        }
        if (null == format) {
            throw new ConfigurationException(
                ScriptPluginResourceModelSourceFactory.RESOURCE_FORMAT_PROP + " is required");
        }
        this.project = configuration.getProperty("project");

        configData = new HashMap<String, String>();
        for (final Object o : configuration.keySet()) {
            final String k = (String) o;
            configData.put(k, configuration.getProperty(k));
        }
        executionDataContext = ScriptDataContextUtil.createScriptDataContextObjectForProject(framework, project);
        executionDataContext.get("plugin").putAll(factory.createPluginData());
        executionDataContext.put("config", configData);
    }

    @Override
    public List<String> listSecretsPathResourceModel(Map<String, Object> configuration) {
        Description description = this.factory.getDescription();
        DataContext dataContext = ScriptDataContextUtil.createScriptDataContextObjectForProject(framework, project);
        return ScriptResourceUtil.generateListStoragePath(description, dataContext, configuration);
    }

    @Override
    public SecretBundle prepareSecretBundleResourceModel(Services services, Map<String, Object> configuration) {
        Description description = this.factory.getDescription();
        DataContext dataContext = ScriptDataContextUtil.createScriptDataContextObjectForProject(framework, project);
        StorageTree storageTree = services.getService(KeyStorageTree.class);

        try{
            return ScriptResourceUtil.generateBundle(description, dataContext, storageTree, configuration, logger );
        }catch (Exception e){
            throw new RuntimeException("Storage Unauthorized access. The following keys have : " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "script-plugin{" +
               "name=" + provider.getName() +
               ", plugin file=" + provider.getArchiveFile() +
               ", output format=" + format
               + '}';
    }
}
