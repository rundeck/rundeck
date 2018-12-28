package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.LogFlusher
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.PluginControlServiceImpl
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionLogger
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.ContextManager
import com.dtolabs.rundeck.core.execution.workflow.NodeRecorder
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListenerImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import org.springframework.beans.factory.InitializingBean
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import rundeck.CommandExec
import rundeck.services.framework.RundeckProjectConfigurable
import rundeck.services.nodes.CacheNodeStatus

import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class NodeStatusService implements RundeckProjectConfigurable, InitializingBean {

    public static final String DEFAULT_CACHE_SPEC = "refreshAfterWrite=30s,expireAfterWrite=30m"
    public static final String PROJECT_NODECACHE_REFRESH= 'project.nodeStatusCache.refresh'

    FrameworkService frameworkService
    ExecutionUtilService executionUtilService
    ExecutionService executionService
    StorageService storageService
    NodeService nodeService
    def configurationService
    ThreadPoolTaskExecutor nodeStatusTaskExecutor
    def ThreadBoundOutputStream sysThreadBoundOut = ThreadBoundOutputStream.bindSystemOut()
    def ThreadBoundOutputStream sysThreadBoundErr = ThreadBoundOutputStream.bindSystemErr()
    def LoggingService loggingService

    //basic creation, created via spec string in afterPropertiesSet()
    private LoadingCache<StatusNodeCacheKey, CacheNodeStatus> nodeStatusCache =
            CacheBuilder.newBuilder()
                        .refreshAfterWrite(30, TimeUnit.SECONDS)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .build(
                    new CacheLoader<String, CacheNodeStatus>() {
                        CacheNodeStatus load(String key) {
                            return loadNodeStatus(key)
                        }
                    }
            )

    String category='resourceModelSource'

    @Override
    Map<String, String> getCategories() {
        [refresh: 'resourceModelSource']
    }

    @Override
    List<Property> getProjectConfigProperties() {
        [
                PropertyBuilder.builder().with {
                    integer 'refresh'
                    title 'Node Status Cache Duration'
                    description 'It will maintain the node status in the cache for X seconds, default 300s.\n\nIt will refresh node status cache results after this many seconds have passed.'
                    required(false)
                    defaultValue '300'
                }.build()
        ]
    }

    @Override
    Map<String, String> getPropertiesMapping() {
        ['refresh': PROJECT_NODECACHE_REFRESH]
    }

    void afterPropertiesSet() {
        initCaches()
    }

    void initCaches() {
        def spec = configurationService?.getCacheSpecFor('nodeStatusService', 'nodeStatusCache', DEFAULT_CACHE_SPEC)?:DEFAULT_CACHE_SPEC
        log.debug("nodeCache: creating from spec: ${spec}")

        nodeStatusCache = CacheBuilder.from(spec)
                                      .recordStats()
                                      .build(
                new CacheLoader<StatusNodeCacheKey, CacheNodeStatus>() {
                    CacheNodeStatus load(StatusNodeCacheKey key) {

                        Future<CacheNodeStatus> future = nodeStatusTaskExecutor.submit({
                           return loadNodeStatus(key)
                        } as Callable<CacheNodeStatus>)

                        return future.get()
                    }


                    @Override
                    ListenableFuture<CacheNodeStatus> reload(
                            final StatusNodeCacheKey key,
                            final CacheNodeStatus oldValue
                    ) throws Exception {
                        if (needsReload(key, oldValue)) {
                            ListenableFutureTask<CacheNodeStatus> task = ListenableFutureTask.create{ loadNodeStatus(key) }
                            nodeStatusTaskExecutor.execute(task);
                            return task;
                        }else {
                            return Futures.immediateFuture(oldValue)
                        }
                    }

                });
    }

    //get all cache for a project
    Map<String, CacheNodeStatus> getCurrentStatus(String project){
        Map result = [:]
        def listCurrentCache = nodeStatusCache.asMap()
        if(listCurrentCache.size()>0){
            result = listCurrentCache.findAll {it.key.project == project}.collectEntries {k, v -> [(k.node.nodename): v]}
        }
        result
    }

    void mergeNodeAttributes(String project, INodeSet nodeSet){
        //merged attributes with health check cache
        Map<String, CacheNodeStatus> cacheStatus = getCurrentStatus(project)
        for (final INodeEntry iNodeEntry : nodeSet.getNodes()) {
            if(cacheStatus.get(iNodeEntry.nodename)) {
                def status =  cacheStatus.get(iNodeEntry.nodename)
                iNodeEntry.attributes?.put("healthCheckStatus", status.executorReachable)
                iNodeEntry.attributes?.put("healthCheckExecutorTimeout", status.executorTimeout)
                iNodeEntry.attributes?.put("healthCheckStatusDescription", status.statusDescription)
                iNodeEntry.attributes?.put("healthCheckLastChecktime", status.lastChecktime.format("dd/MM HH:mm:ss z"))
                iNodeEntry.attributes?.put("healthCheckDurationTime", (status.checkDurationTime/1000).toString())
                if(status.executorReachable=="successful"){
                    iNodeEntry.attributes?.put("healthCheckStatus:icon", "glyphicon-ok text-success")
                }else{
                    iNodeEntry.attributes?.put("healthCheckStatus:icon", "glyphicon-remove text-danger")
                }
            }
        }
    }

    void registerStatus(String project, INodeEntry node, String user, String userRolelist){
        StatusNodeCacheKey key = new StatusNodeCacheKey( project: project, node: node, user: user, userRolelist: userRolelist )
        nodeStatusCache.get(key)
    }


    CacheNodeStatus loadNodeStatus(StatusNodeCacheKey nodeCacheKey){

        log.debug("getting status of node: ${nodeCacheKey}")
        Framework framework = frameworkService.getRundeckFramework()
        String project = nodeCacheKey.project
        INodeEntry node = nodeCacheKey.node
        String user = nodeCacheKey.user
        String userRolelist = nodeCacheKey.userRolelist

        List roleList = userRolelist.split(",").collect { it as String }

        def authContext = frameworkService.getAuthContextForUserAndRolesAndProject(
                user,
                roleList,
                project
        )

        ContextManager contextmanager = new ContextManager()
        def logOutFlusher = new LogFlusher()
        def logErrFlusher = new LogFlusher()

        //create listener without output
        WorkflowExecutionListenerImpl executionListenerWf = new WorkflowExecutionListenerImpl(
                new NodeRecorder(),
                new HiddenLogger()
        )

        //create execution context
        def builder = ExecutionContextImpl.builder()
        builder.with {
            pluginControlService(PluginControlServiceImpl.forProject(framework, project))
            frameworkProject(project)
            storageTree(storageService.storageTreeWithContext(authContext))
            nodeService(nodeService)
            executionListener(executionListenerWf)
        }
        builder.framework(framework)
        builder.authContext(authContext)
        builder.threadCount(1)

        def checkCommand

        //TODO: we can use a node attribute to get the healtcheck command
        if ("windows".equalsIgnoreCase(node.osFamily)) {
            checkCommand = "dir"
        } else {
            checkCommand = "uname"
        }


        def executorReachable
        def executorTimeout
        def statusDescription

        try {

            sysThreadBoundOut.installThreadStream(loggingService.createLogOutputStream(
                    new NoLogWriter(),
                    LogLevel.DEBUG,
                    contextmanager,
                    logOutFlusher,
                    null
            ))
            sysThreadBoundErr.installThreadStream(loggingService.createLogOutputStream(
                    new NoLogWriter(),
                    LogLevel.ERROR,
                    contextmanager,
                    logErrFlusher,
                    null
            ))

            CommandExec step = new CommandExec(
                    adhocRemoteString: checkCommand,
                    adhocExecution: true
            )
            StepExecutionItem item = executionUtilService.itemForWFCmdItem(step, null, null)

            def context = builder.build()

            NodeStepExecutor interpreter = framework.getNodeStepExecutorForItem(item);
            def result = interpreter.executeNodeStep(context, item, node);

            if (result.isSuccess()) {
                executorReachable = "successful"
                executorTimeout = "false"
                statusDescription = "Health Check successful"
            } else {
                executorReachable = "failure"
                executorTimeout = "true"
                statusDescription = result.failureReason
            }

        } catch (Exception e) {
            executorReachable = "fail"
            executorTimeout = "false"
            statusDescription = e.message
        } finally{
            sysThreadBoundOut.close()
            sysThreadBoundOut.removeThreadStream()
            sysThreadBoundErr.close()
            sysThreadBoundErr.removeThreadStream()

        }

        def refresh = nodeStatusCacheConfig(framework.projectManager.loadProjectConfig(project))

        def cachedNodes = new CacheNodeStatus(
                nodeName: node.nodename,
                lastChecktime: new Date(),
                checkDurationTime: refresh,
                executorReachable: executorReachable,
                executorTimeout: executorTimeout,
                statusDescription: statusDescription
        )

        return cachedNodes
    }

    boolean needsReload(StatusNodeCacheKey nodeCacheKey, CacheNodeStatus oldNodes) {
        def project = nodeCacheKey.project
        def framework = frameworkService.getRundeckFramework()
        def now = new Date()
        def refresh = nodeStatusCacheConfig(framework.projectManager.loadProjectConfig(project))
        if(now.time - oldNodes.lastChecktime.time < refresh){
            log.debug("node cache need to be reload: ${nodeCacheKey}")
            return false
        }
        return true
    }

    long nodeStatusCacheConfig(final IRundeckProjectConfig projectConfig) {
        if(projectConfig.hasProperty(PROJECT_NODECACHE_REFRESH)){
            def refresh = Long.parseLong(projectConfig.getProperty(PROJECT_NODECACHE_REFRESH))
            return refresh*1000
        }else{
            (30*1000)
        }
    }


    static class HiddenLogger implements ExecutionLogger{

        @Override
        void log(final int level, final String message) {

        }

        @Override
        void log(final int level, final String message, final Map eventMeta) {

        }

        @Override
        void event(final String eventType, final String message, final Map eventMeta) {

        }
    }

    static class StatusNodeCacheKey {
        String project
        INodeEntry node
        String user
        String userRolelist

        boolean equals(final o) {
            if (this.is(o)) {
                return true
            }
            if (getClass() != o.class) {
                return false
            }

            final StatusNodeCacheKey that = (StatusNodeCacheKey) o

            if (project != that.project) {
                return false
            }
            if (user != that.user) {
                return false
            }
            if (userRolelist != that.userRolelist) {
                return false
            }
            if (node.nodename != node.nodename) {
                return false
            }

            return true
        }

        int hashCode() {
            int result
            result = (project != null ? project.hashCode() : 0)
            result = 31 * result + (node.nodename != null ? node.nodename.hashCode() : 0)
            result = 31 * result + (user != null ? user.hashCode() : 0)
            result = 31 * result + (userRolelist != null ? userRolelist.hashCode() : 0)
            return result
        }

        @Override
        public String toString() {
            return "StatusNodeCacheKey{" +
                   "project='" + project + '\'' +
                   ", node.nodename='" + node.nodename + '\'' +
                   ", user=" + user +
                   ", userRolelist=" + userRolelist +
                   "} " + super.toString();
        }
    }



    static class NoLogWriter implements StreamingLogWriter{
        @Override
        void openStream() throws IOException {

        }

        @Override
        void addEvent(final LogEvent event) {

        }

        @Override
        void close() {

        }
    }

}
