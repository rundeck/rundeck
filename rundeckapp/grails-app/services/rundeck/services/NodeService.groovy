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
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
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
import org.springframework.core.task.AsyncTaskExecutor
import rundeck.services.nodes.CachedProjectNodes

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

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
    def AsyncTaskExecutor nodeTaskExecutor
    def Services rundeckSpiBaseServicesProvider

    def nodeSourceLoaderService

    @Override
    Map<String, String> getCategories() {
        [enabled: 'resourceModelSource', delay: 'resourceModelSource', firstLoadSynch: 'resourceModelSource', loadThreads: 'resourceModelSource']
    }
    @Override
    List<Property> getProjectConfigProperties() {
        [
                PropertyBuilder.builder().with {
                    booleanType 'enabled'
                    title 'Use Asynchronous Cache'
                    description 'Use asynchronous cache for all Node Source results in this project'
                    required(false)
                    defaultValue 'true'
                    renderingOption(StringRenderingConstants.GROUP_NAME, 'Node Sources')
                }.build(),
                PropertyBuilder.builder().with {
                    integer 'delay'
                    title 'Cache Delay'
                    description 'Delay in seconds, at least 30.\n\nRefresh results after this many seconds have passed. Results may be this many seconds old. Cache refreshes no more frequently that 30s.'
                    required(false)
                    defaultValue '30'
                    renderingOption(StringRenderingConstants.GROUP_NAME, 'Node Sources')
                }.build(),
                PropertyBuilder.builder().with {
                    booleanType  'firstLoadSynch'
                    title 'Synchronous First Load'
                    description 'When the cache is empty, forces the first load to happen synchronously to prevent empty node results.'
                    required(false)
                    defaultValue 'true'
                    renderingOption(StringRenderingConstants.GROUP_NAME, 'Node Sources')
                }.build(),
                PropertyBuilder.builder().with {
                    integer 'loadThreads'
                    title 'Load Threads'
                    description 'Number of Node Sources queried concurrently when loading nodes.\n\nThe default of 1 queries Node Sources one at a time. Higher values reduce load time for projects with many Node Sources, but increase concurrent requests to the systems behind them, which may throttle.'
                    required(false)
                    defaultValue '1'
                    renderingOption(StringRenderingConstants.GROUP_NAME, 'Node Sources')
                }.build()
        ]
    }
    String serviceName= ServiceNameConstants.ResourceModelSource
    String propertyPrefix = FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX

    @Override
    Map<String, String> getPropertiesMapping() {
        ['delay': PROJECT_NODECACHE_DELAY, 'enabled': PROJECT_NODECACHE_ENABLED, 'firstLoadSynch': PROJECT_NODECACHE_FIRSTLOAD_SYNCH, 'loadThreads': ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS]
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


    /**
     * Source of increasing numbers for background reload requests. Never reset, so a request made after a project's
     * tracking was removed is still newer than any node set loaded before.
     */
    private final AtomicLong reloadRequestSequence = new AtomicLong()

    /**
     * Number of the latest background reload request per project. Tracked outside the cached nodes, because a reload
     * replaces them and a request made while a reload is running must not be lost. Removed when the project's
     * nodes are forcibly refreshed, such as when the project is deleted.
     */
    private final ConcurrentMap<String, Long> lastReloadRequests = new ConcurrentHashMap<>()

    private File _frameworkVarDir

    private File getFrameworkVarDir(){
        if (null == _frameworkVarDir) {
            // loaded after startup to avoid repeated queries for framework props value which would not change
            _frameworkVarDir = frameworkService.getFrameworkVarDir()
        }
        return _frameworkVarDir
    }

    @Override
    void afterPropertiesSet() throws Exception {
        def spec = configurationService?.getString('nodeService.nodeCache.spec', DEFAULT_CACHE_SPEC)?:DEFAULT_CACHE_SPEC

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
                            //a failed reload must not retry forever because of the requests it handles
                            oldValue.reloadRequestSequence = lastReloadRequest(key)
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
        def globalEnabled = configurationService.getBoolean('nodeService.nodeCache.enabled', true)
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
        if (lastReloadRequest(project) > oldNodes.reloadRequestSequence) {
            log.debug("reload requested, forcing node reload for ${project}")
            return true
        }
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
        //read before loading the config, so a request made after this point triggers another reload
        long requestSequence = lastReloadRequest(project)
        def framework = frameworkService.getRundeckFramework()
        def rdprojectconfig = framework.getFrameworkProjectMgr().loadProjectConfig(project)
        def enabled = isCacheEnabled(rdprojectconfig)
        log.debug("loadNodes for ${project}... (cacheEnabled: ${enabled})")

        def resourceModelSourceService = framework.getResourceModelSourceService()

        def nodeSupport = new ProjectNodeSupport(
            getFrameworkVarDir(),
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
                reloadRequestSequence: requestSequence,
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
        //the forced reload covers any pending request, and this stops tracking projects that no longer exist
        lastReloadRequests.remove(name)
        nodeCache.invalidate(name)
    }

    /**
     * @param project project name
     * @return the number of the latest background reload requested for the project, 0 if none is tracked
     */
    long lastReloadRequest(final String project) {
        lastReloadRequests.get(project) ?: 0L
    }

    /**
     * Reload the nodes for a project in the background. Unlike {@link #refreshProjectNodes(String)}, the cached
     * nodes keep being served until the new node set has finished loading, so use this when a slow reload
     * should not make the nodes unavailable (e.g. after a project config change).
     * @param name project name
     */
    @Override
    void refreshProjectNodesInBackground(final String name) {
        lastReloadRequests.compute(name, { String k, Long v -> reloadRequestSequence.incrementAndGet() })
        if (nodeCache.getIfPresent(name) != null) {
            //ignored by the cache while a reload is already running, the request number makes the next check reload again
            nodeCache.refresh(name)
        } else {
            //nothing to keep serving, and refresh of an absent key would load synchronously on this thread
            nodeCache.invalidate(name)
        }
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
