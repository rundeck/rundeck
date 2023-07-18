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

package rundeck.services

import com.codahale.metrics.MetricRegistry
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IProjectNodes
import com.dtolabs.rundeck.core.common.IProjectNodesFactory
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.NodeSourceLoader
import com.dtolabs.rundeck.core.common.ProjectNodeSupport
import com.dtolabs.rundeck.core.common.SourceDefinition
import com.dtolabs.rundeck.core.nodes.ProjectNodeService
import com.dtolabs.rundeck.core.plugins.Closeables
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.core.resources.SourceFactory
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.cache.RemovalListener
import com.google.common.cache.RemovalNotification
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import org.rundeck.app.spi.Services
import org.rundeck.core.projects.ProjectConfigurable
import org.rundeck.core.projects.ProjectPluginListConfigurable
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.AsyncListenableTaskExecutor
import rundeck.services.nodes.CachedProjectNodes

import java.util.concurrent.TimeUnit

/**
 * Provides asynchronous loading and caching of nodesets for projects
 */
class NodeService implements InitializingBean, ProjectConfigurable, IProjectNodesFactory, ProjectNodeService, ProjectPluginListConfigurable {
    public static final String PROJECT_NODECACHE_DELAY = 'project.nodeCache.delay'
    public static final String PROJECT_NODECACHE_ENABLED = 'project.nodeCache.enabled'
    public static final String PROJECT_NODECACHE_FIRSTLOAD_SYNCH = 'project.nodeCache.firstLoadSynch'
    static transactional = false
    public static final String DEFAULT_CACHE_SPEC = "refreshInterval=30s"
    def metricService
    def frameworkService
    def configurationService
    def projectManagerService
    def pluginService
    def AsyncListenableTaskExecutor nodeTaskExecutor
    def Services rundeckSpiBaseServicesProvider

    def nodeSourceLoaderService

    @Override
    Map<String, String> getCategories() {
        [enabled: 'resourceModelSource', delay: 'resourceModelSource', firstLoadSynch: 'resourceModelSource']
    }
    @Override
    List<Property> getProjectConfigProperties() {
        [
                PropertyBuilder.builder().with {
                    booleanType 'enabled'
                    title 'Use Asynchronous Cache'
                    description 'Use asynchronous cache for all Resource Model Source results in this project'
                    required(false)
                    defaultValue 'true'
                }.build(),
                PropertyBuilder.builder().with {
                    integer 'delay'
                    title 'Cache Delay'
                    description 'Delay in seconds, at least 30.\n\nRefresh results after this many seconds have passed. Results may be this many seconds old. Cache refreshes no more frequently that 30s.'
                    required(false)
                    defaultValue '30'
                }.build(),
                PropertyBuilder.builder().with {
                    booleanType  'firstLoadSynch'
                    title 'Synchronous First Load'
                    description 'When the cache is empty, forces the first load to happen synchronously to prevent empty node results.'
                    required(false)
                    defaultValue 'true'
                }.build()
        ]
    }
    String serviceName= ServiceNameConstants.ResourceModelSource
    String propertyPrefix = FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX

    @Override
    Map<String, String> getPropertiesMapping() {
        ['delay': PROJECT_NODECACHE_DELAY, 'enabled': PROJECT_NODECACHE_ENABLED, 'firstLoadSynch': PROJECT_NODECACHE_FIRSTLOAD_SYNCH]
    }

    //basic creation, created via spec string in afterPropertiesSet()
    private LoadingCache<String, CachedProjectNodes> nodeCache =
            CacheBuilder.newBuilder()
                        .refreshAfterWrite(30, TimeUnit.SECONDS)
                        .build(
                    new CacheLoader<String, CachedProjectNodes>() {
                        public CachedProjectNodes load(String key) {
                            return loadNodes(key,null);
                        }
                    }
            );

    @Override
    void afterPropertiesSet() throws Exception {
        def spec = configurationService?.getCacheSpecFor('nodeService', 'nodeCache', DEFAULT_CACHE_SPEC)?:DEFAULT_CACHE_SPEC

        log.debug("nodeCache: creating from spec: ${spec}")

        nodeCache = CacheBuilder.from(spec)
                                .recordStats()
                                .removalListener(
                new RemovalListener<String, CachedProjectNodes>() {
                    @Override
                    void onRemoval(final RemovalNotification<String, CachedProjectNodes> notification) {
                        Closeables.closeQuietly(notification.getValue()?.nodeSupport)
                    }
                }
        )
                                .build(
                new CacheLoader<String, CachedProjectNodes>() {
                    public CachedProjectNodes load(String key) {
                        return loadNodes(key,null);
                    }

                    @Override
                    ListenableFuture<CachedProjectNodes> reload(final String key, final CachedProjectNodes oldValue)
                            throws Exception
                    {
                        if (needsReload(key, oldValue)) {
                            ListenableFutureTask<CachedProjectNodes> task = ListenableFutureTask.create{ loadNodes(key,oldValue) }
                            nodeTaskExecutor.execute(task);
                            return task;
                        } else {
                            return Futures.immediateFuture(oldValue)
                        }
                    }
                }
        )

        MetricRegistry registry = metricService?.getMetricRegistry()
        Util.addCacheMetrics(this.class.name + ".nodeCache", registry, nodeCache)
    }
    boolean isCacheEnabled(IRundeckProjectConfig projectConfig){
        def globalEnabled = configurationService.getCacheEnabledFor('nodeService','nodeCache', true)
        return globalEnabled && projectNodeCacheEnabledConfig(projectConfig)
    }

    boolean isCacheFirstloadSynchEnabled(IRundeckProjectConfig projectConfig, Boolean defval) {
        projectConfig.hasProperty(PROJECT_NODECACHE_FIRSTLOAD_SYNCH) ?
        Boolean.parseBoolean(projectConfig.getProperty(PROJECT_NODECACHE_FIRSTLOAD_SYNCH)) :
        defval
    }

    boolean needsReload(String project, CachedProjectNodes oldNodes) {
        def framework = frameworkService.getRundeckFramework()
        def rdprojectconfig = framework.projectManager.loadProjectConfig(project)
        def now = new Date()
        def delay = projectNodeCacheDelayConfig(rdprojectconfig)
        log.debug("check needs reload ${project} delay ${delay}, elapsed ${now.time - oldNodes.cacheTime.time}...")
        if(rdprojectconfig.configLastModifiedTime > oldNodes.cacheTime){
            log.debug("config changed, forcing node reload for ${project}")
            //refresh if config has changed
            return true
        }
        if(now.time - oldNodes.cacheTime.time < delay){
            log.debug("within cache duration, not reloading for ${project}")
            return false
        }
        log.debug("Elapsed cache duration, will reload for ${project}")
        return true
    }

    /**
     * Return project config for node cache delay
     * @param project
     * @return
     */
    long projectNodeCacheDelayConfig(final IRundeckProjectConfig projectConfig) {
        projectConfig.hasProperty(PROJECT_NODECACHE_DELAY)?
                Long.parseLong(projectConfig.getProperty(PROJECT_NODECACHE_DELAY))*1000 :
        (30*1000)
    }
    /**
     * Return project config for node cache delay
     * @param project
     * @param s @return
     */
    boolean projectNodeCacheEnabledConfig(final IRundeckProjectConfig projectConfig) {
        projectConfig.hasProperty(PROJECT_NODECACHE_ENABLED)?
                Boolean.parseBoolean(projectConfig.getProperty(PROJECT_NODECACHE_ENABLED)) :
        true
    }

    /**
     * Create the project nodes object for a project.
     * @param project project name
     * @param oldValue old value if present, null if this is the first load
     * @return project nodes object
     */
    CachedProjectNodes loadNodes(final String project, final CachedProjectNodes oldValue) {
        def framework = frameworkService.getRundeckFramework()
        def rdprojectconfig = framework.getFrameworkProjectMgr().loadProjectConfig(project)
        def enabled = isCacheEnabled(rdprojectconfig)
        log.debug("loadNodes for ${project}... (cacheEnabled: ${enabled})")

        def resourceModelSourceService = framework.getResourceModelSourceService()

        def nodeSupport = new ProjectNodeSupport(
            rdprojectconfig,
            framework.getResourceFormatGeneratorService(),
            resourceModelSourceService,
            nodeSourceLoaderService
        )

        def preloadedNodes = null

        if(enabled){
            /**
             * Use a loading cache to preload data if it is cached on disk
             */
            def loadingCache = nodeSupport.createCachingSource(
                    SourceFactory.staticSource(null),
                    "cache",
                    "(cache)",
                    SourceFactory.CacheType.LOAD_ONLY,
                    false
            )
            preloadedNodes = loadingCache.nodes

        }

        log.debug("Preload nodes cache for ${project} size: ${preloadedNodes?.nodes?.size() ?: 0}")

        /**
         * Create a caching source to write data loaded from nodeSupport to disk when successful
         */
        def source = ProjectNodeSupport.asModelSource(nodeSupport)
        if(enabled) {
            source = nodeSupport.createCachingSource(
                    source,
                    "cache",
                    "(cache)",
                    SourceFactory.CacheType.STORE_ONLY,
                    true
            )
        }

        /**
         * actual object used for project node loading, using preloaded node data,
         * and writing successful loads to disk.  Uses nodeSupport as delegate for other IProjectNodes method calls.
         */
        def cachedNodes = new CachedProjectNodes(
                cacheTime: new Date(),
                nodeSupport: nodeSupport,
                doCache: enabled,
                nodes: preloadedNodes,
                source: source
        )

        /**
         * asynchronous first load, unless disabled by configuration
         */
        def asynchronousFirstLoad = configurationService.getBoolean('nodeService.nodeCache.firstLoadAsynch', false)
        //project config will override app config
        asynchronousFirstLoad = !isCacheFirstloadSynchEnabled(rdprojectconfig, !asynchronousFirstLoad)
        def firstLoadInBg = null==oldValue && (preloadedNodes?.nodes?.size()>0 || asynchronousFirstLoad)
        if(null==oldValue && !firstLoadInBg){
            log.debug("Empty preload cache, loading nodes synchronously for $project ...")
        }

        Closure clos = {
            long start=System.currentTimeMillis()
            def result = cachedNodes.reloadNodeSet()
            log.debug("Finish reloadNodeSet for ${project} in ${System.currentTimeMillis()-start}")
            result
        }
        if (firstLoadInBg) {
            //want to return something asap, and have some cache data, so perform first reload in background thread
            nodeTaskExecutor.execute {
                metricService?.withTimer(this.class.name, "project.${project}.loadNodes", clos) ?: clos()
            }
        } else {
            //we are refreshing the data in asynch thread already, so can perform this synchronously
            //or we have no preloaded cache data, so force synchronous first load
            metricService?.withTimer(this.class.name, "project.${project}.loadNodes", clos) ?: clos()
        }

        cachedNodes
    }

    @Override
    void refreshProjectNodes(final String name) {
        nodeCache.invalidate(name)
    }

    INodeSet getNodeSet(final String name) {
        getNodes(name).nodeSet
    }

    @Override
    INodeSet getNodeSet(String name, List<String> excludePlugins) {
        getNodes(name).nodeSet
    }

    IProjectNodes getNodes(final String name) {
        def framework = frameworkService.getRundeckFramework()
        if (!framework.frameworkProjectMgr.existsFrameworkProject(name)) {
            throw new IllegalArgumentException("Project does not exist: " + name)
        }
        def result = nodeCache.get(name)
        if (!result) {
            throw new IllegalArgumentException("Project does not exist: " + name)
        }
        result
    }

}
