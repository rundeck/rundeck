package rundeck.services

import com.codahale.metrics.MetricRegistry
import com.dtolabs.rundeck.core.common.IProjectNodes
import com.dtolabs.rundeck.core.common.IProjectNodesFactory
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.ProjectNodeSupport
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.resources.SourceFactory
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.task.AsyncListenableTaskExecutor
import rundeck.Project
import rundeck.services.framework.RundeckProjectConfigurable
import rundeck.services.nodes.CachedProjectNodes

import java.util.concurrent.TimeUnit

/**
 * Provides asynchronous loading and caching of nodesets for projects
 */
class NodeService implements InitializingBean, RundeckProjectConfigurable,IProjectNodesFactory {
    public static final String PROJECT_NODECACHE_DELAY = 'project.nodeCache.delay'
    public static final String PROJECT_NODECACHE_ENABLED = 'project.nodeCache.enabled'
    static transactional = false
    public static final String DEFAULT_CACHE_SPEC = "refreshInterval=30s"
    def metricService
    def frameworkService
    def configurationService
    def AsyncListenableTaskExecutor nodeTaskExecutor

    String category='resourceModelSource'

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
                }.build()
        ]
    }

    @Override
    Map<String, String> getPropertiesMapping() {
        ['delay': PROJECT_NODECACHE_DELAY, 'enabled': PROJECT_NODECACHE_ENABLED]
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
        def rdprojectconfig = framework.getProjectManager().loadProjectConfig(project)
        def enabled = isCacheEnabled(rdprojectconfig)
        log.debug("loadNodes for ${project}... (cacheEnabled: ${enabled})")

        /**
         * base node support object for loading all node data synchronously
         */
        def nodeSupport = new ProjectNodeSupport(
                rdprojectconfig,
                framework.getResourceFormatGeneratorService(),
                framework.getResourceModelSourceService()
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
        def asynchronousFirstLoad = configurationService.getBoolean('nodeService.nodeCache.firstLoadAsynch', true)
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
        expireProjectNodes(name)
    }

    def expireProjectNodes(String name){
        nodeCache.invalidate(name)
    }

    IProjectNodes getNodes(final String name) {
        def framework = frameworkService.getRundeckFramework()
        if (!framework.projectManager.existsFrameworkProject(name)) {
            throw new IllegalArgumentException("Project does not exist: " + name)
        }
        def result = nodeCache.get(name)
        if (!result) {
            throw new IllegalArgumentException("Project does not exist: " + name)
        }
        result
    }
}
