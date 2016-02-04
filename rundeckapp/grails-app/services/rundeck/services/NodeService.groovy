package rundeck.services

import com.codahale.metrics.MetricRegistry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IProjectNodes
import com.dtolabs.rundeck.core.common.IProjectNodesFactory
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.ProjectNodeSupport
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import org.springframework.beans.factory.InitializingBean
import rundeck.Project
import rundeck.services.framework.RundeckProjectConfigurable
import rundeck.services.nodes.CachedProjectNodes

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Provides asynchronous loading and caching of nodesets for projects
 */
class NodeService implements InitializingBean, RundeckProjectConfigurable,IProjectNodesFactory {
    public static final String PROJECT_NODECACHE_DELAY = 'project.nodeCache.delay'
    public static final String PROJECT_NODECACHE_ENABLED = 'project.nodeCache.enabled'
    static transactional = false
    public static final String DEFAULT_CACHE_SPEC = "refreshAfterWrite=30s"
    def metricService
    def frameworkService
    def configurationService

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
                    description 'Delay in seconds.\n\nRefresh results after this many seconds have passed. (Results may be this many seconds old.)'
                    required(false)
                    defaultValue '30'
                }.build()
        ]
    }

    @Override
    Map<String, String> getPropertiesMapping() {
        ['delay': PROJECT_NODECACHE_DELAY, 'enabled': PROJECT_NODECACHE_ENABLED]
    }
    /**
     * Scheduled executor for retries
     */
    private ExecutorService executor = Executors.newFixedThreadPool(2)

    //basic creation, created via spec string in afterPropertiesSet()
    private LoadingCache<String, CachedProjectNodes> nodeCache =
            CacheBuilder.newBuilder()
                        .refreshAfterWrite(30, TimeUnit.SECONDS)
                        .build(
                    new CacheLoader<String, CachedProjectNodes>() {
                        public CachedProjectNodes load(String key) {
                            return loadNodes(key);
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
                        return loadNodes(key);
                    }

                    @Override
                    ListenableFuture<CachedProjectNodes> reload(final String key, final CachedProjectNodes oldValue)
                            throws Exception
                    {
                        if (needsReload(key, oldValue)) {
                            ListenableFutureTask<CachedProjectNodes> task = ListenableFutureTask.create{ loadNodes(key) }
                            executor.execute(task);
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
        if(now.time - oldNodes.cacheTime.time < projectNodeCacheDelayConfig(rdprojectconfig)){
            return false
        }
        Project.withNewSession {
            Project rdproject = Project.findByName(project)
            boolean needsReload = rdproject == null ||
                    oldNodes.projectConfig.configLastModifiedTime == null ||
                    rdprojectconfig.getConfigLastModifiedTime() > oldNodes.projectConfig.configLastModifiedTime
            needsReload
        }
    }

    /**
     * Return project config for node cache delay
     * @param project
     * @return
     */
    long projectNodeCacheDelayConfig(final IRundeckProjectConfig projectConfig) {
        projectConfig.hasProperty(PROJECT_NODECACHE_DELAY)?
                Long.parseLong(projectConfig.getProperty(PROJECT_NODECACHE_DELAY)) :
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

    CachedProjectNodes loadNodes(final String project) {
        def framework = frameworkService.getRundeckFramework()
        def rdprojectconfig = framework.getProjectManager().loadProjectConfig(project)
        log.debug("loadNodes for ${project}... (cacheEnabled: ${isCacheEnabled(rdprojectconfig)})")


        def nodeSupport = new ProjectNodeSupport(
                rdprojectconfig,
                framework.getResourceFormatGeneratorService(),
                framework.getResourceModelSourceService()
        )
        def cachedNodes = new CachedProjectNodes(
                cacheTime: new Date(),
                nodeSupport: nodeSupport,
                doCache: isCacheEnabled(rdprojectconfig)
        )
        Closure clos = cachedNodes.&reloadNodeSet
        metricService?.withTimer(this.class.name, "project.${project}.loadNodes", clos) ?: clos()

        cachedNodes
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
