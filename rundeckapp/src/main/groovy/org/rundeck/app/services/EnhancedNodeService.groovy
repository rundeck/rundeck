/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.services

import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IProjectNodes
import com.dtolabs.rundeck.core.common.IProjectNodesFactory
import com.dtolabs.rundeck.core.common.ProjectNodeSupport
import com.dtolabs.rundeck.core.nodes.ProjectNodeService
import com.dtolabs.rundeck.core.plugins.PluginConfiguration
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.nodes.NodeEnhancerPlugin
import groovy.transform.CompileStatic
import org.rundeck.core.projects.ProjectPluginListConfigurable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.FrameworkService
import rundeck.services.NodeService
import rundeck.services.PluginService
import rundeck.services.feature.FeatureService

/**
 *
 */
@CompileStatic
class EnhancedNodeService
        implements IProjectNodesFactory, ProjectNodeService, InitializingBean, ProjectPluginListConfigurable {
    private static final Logger LOG = LoggerFactory.getLogger(EnhancedNodeService)
    @Autowired NodeService nodeService
    @Autowired FrameworkService frameworkService
    @Autowired PluginService pluginService
    @Autowired FeatureService featureService
    private Map<String, ProjectNodesEnhancer> loadedPlugins = [:]

    private boolean enabled

    @Override
    void afterPropertiesSet() throws Exception {
        enabled = featureService.featurePresent('enhanced-nodes', false)
    }

    String serviceName = ServiceNameConstants.NodeEnhancer
    String propertyPrefix = ProjectNodeSupport.NODE_ENHANCER_PROP_PREFIX

    @Override
    IProjectNodes getNodes(final String name) {
        if (!enabled) {
            return nodeService.getNodes(name)
        }
        if (null == loadedPlugins[name]) {
            loadPlugins(name)
        }
        return loadedPlugins[name].withProjectNodes(nodeService.getNodes(name))
    }

    private void loadPlugins(final String project) {
        def framework = frameworkService.getRundeckFramework()
        def rdprojectconfig = framework.getFrameworkProjectMgr().loadProjectConfig(project)
        def plugins = ProjectNodeSupport.listPluginConfigurations(
                rdprojectconfig.projectProperties,
                propertyPrefix,
                serviceName
        )

        def cacheItem = new ProjectNodesEnhancer(project: project)
        plugins.eachWithIndex { PluginConfiguration pluginConfig, int index ->
            LOG.debug("Configure node enhancer $pluginConfig with $pluginConfig.configuration")
            def validated = pluginService.validatePluginConfig(
                    pluginConfig.service,
                    pluginConfig.provider,
                    pluginConfig.configuration
            )
            if (!validated) {
                LOG.error(
                        "Invalid $pluginConfig.service plugin [$index] for $project: $pluginConfig.provider: Not Found"

                )
                return
            }
            if (!validated.valid) {
                LOG.error(
                        "Invalid $pluginConfig.service plugin [$index] for $project: $pluginConfig.provider: " +
                        validated.toString()
                )
                return
            }
            def configured = pluginService.configurePlugin(
                    pluginConfig.provider,
                    pluginConfig.service,
                    pluginConfig.configuration
            )
            if (configured.instance) {
                cacheItem.plugins << new TypedNodeEnhancerPlugin(
                        (NodeEnhancerPlugin) configured.instance,
                        pluginConfig.provider
                )
            }

        }
        cacheItem.loadedTime = System.currentTimeMillis()
        loadedPlugins[project] = cacheItem
    }

    @Override
    void refreshProjectNodes(final String name) {
        if (enabled) {
            loadedPlugins.remove(name)
        }
        nodeService.refreshProjectNodes(name)
    }

    @Override
    INodeSet getNodeSet(final String name) {
        if (!enabled) {
            return nodeService.getNodeSet(name)
        }
        getNodes(name).getNodeSet()
    }

}
