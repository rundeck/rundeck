package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.PluginControlServiceImpl
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionLogger
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.NodeRecorder
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListenerImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.task.AsyncListenableTaskExecutor
import rundeck.CommandExec
import rundeck.services.framework.RundeckProjectConfigurable
import rundeck.services.nodes.CacheNodeStatus
import java.util.concurrent.TimeUnit

class NodeStatusService implements RundeckProjectConfigurable, InitializingBean {

    public static final String DEFAULT_CACHE_SPEC = "refreshInterval=30s"
    public static final String PROJECT_STATUSNODECACHE_DELAY = 'project.nodeStatusCache.delay'

    FrameworkService frameworkService
    ExecutionUtilService executionUtilService
    ExecutionService executionService
    StorageService storageService
    NodeService nodeService
    def configurationService
    def AsyncListenableTaskExecutor nodeTaskExecutor

    //basic creation, created via spec string in afterPropertiesSet()
    private LoadingCache<StatusNodeCacheKey, CacheNodeStatus> nodeStatusCache =
            CacheBuilder.newBuilder()
                        .refreshAfterWrite(30, TimeUnit.SECONDS)
                        .build(
                    new CacheLoader<String, CacheNodeStatus>() {
                        public CacheNodeStatus load(String key) {
                            return loadNodeStatus(key);
                        }
                    }
            );

    String category='resourceModelSource'

    @Override
    Map<String, String> getCategories() {
        [delay: 'resourceModelSource']
    }

    @Override
    List<Property> getProjectConfigProperties() {
        [
                PropertyBuilder.builder().with {
                    integer 'delay'
                    title 'Node Status Cache Delay'
                    description 'Delay in seconds, at least 300.\n\nRefresh node status cache results after this many seconds have passed.'
                    required(false)
                    defaultValue '300'
                }.build()
        ]
    }

    @Override
    Map<String, String> getPropertiesMapping() {
        ['delay': PROJECT_STATUSNODECACHE_DELAY]
    }

    void afterPropertiesSet() {
        initCaches()
    }

    void initCaches() {
        def spec = configurationService?.getCacheSpecFor('nodeStatusService', 'nodeStatusCache', DEFAULT_CACHE_SPEC)?:DEFAULT_CACHE_SPEC

        nodeStatusCache = CacheBuilder.from(spec)
                                      .recordStats()
                                      .build(
                new CacheLoader<StatusNodeCacheKey, CacheNodeStatus>() {
                    CacheNodeStatus load(StatusNodeCacheKey key) {
                        return loadNodeStatus(key);
                    }


                    @Override
                    ListenableFuture<CacheNodeStatus> reload(
                            final StatusNodeCacheKey key,
                            final CacheNodeStatus oldValue
                    ) throws Exception {
                        if (needsReload(key, oldValue)) {
                            ListenableFutureTask<CacheNodeStatus> task = ListenableFutureTask.create{ loadNodeStatus(key) }
                            nodeTaskExecutor.execute(task);
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

    CacheNodeStatus getNodeStatus(String project, INodeEntry node, String user, String userRolelist){
        StatusNodeCacheKey key = new StatusNodeCacheKey( project: project, node: node, user: user, userRolelist: userRolelist )
        nodeStatusCache.get(key)
    }

    CacheNodeStatus loadNodeStatus(StatusNodeCacheKey nodeCacheKey){
        Framework framework = frameworkService.getRundeckFramework()
        String project = nodeCacheKey.project
        INodeEntry node = nodeCacheKey.node
        String user = nodeCacheKey.user
        String userRolelist = nodeCacheKey.userRolelist

        List roleList = userRolelist.split(",").collect{it as String}

        def authContext = frameworkService.getAuthContextForUserAndRolesAndProject(
                user,
                roleList,
                project
        )

        //create listener without output
        WorkflowExecutionListenerImpl executionListener = new WorkflowExecutionListenerImpl(
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
        }
        builder.framework(framework)
        builder.authContext(authContext)
        builder.threadCount(1)
        builder.executionListener(executionListener)

        def checkCommand

        //TODO: we can use a node attribute to get the healtcheck command
        if ("windows".equalsIgnoreCase(node.osFamily)) {
            checkCommand="dir"
        }else{
            checkCommand="uname"
        }

        def executorReachable
        def executorTimeout
        def statusDescription

        try {
            CommandExec step = new CommandExec(adhocRemoteString: checkCommand,
                                               adhocExecution: true)
            StepExecutionItem item = executionUtilService.itemForWFCmdItem(step, null, null)

            def context = builder.build()

            NodeStepExecutor interpreter = framework.getNodeStepExecutorForItem(item);
            def result = interpreter.executeNodeStep(context, item, node);

            if(result.isSuccess()){
                executorReachable = "successful"
                executorTimeout="false"
                statusDescription = "Health Check successful"
            }else{
                executorReachable="failure"
                executorTimeout="true"
                statusDescription = result.failureReason
            }

        }catch(Exception e){
            executorReachable="fail"
            executorTimeout="false"
            statusDescription = e.message
        }

        def cachedNodes = new CacheNodeStatus(
                nodeName: node.nodename,
                cacheTime: new Date(),
                executorReachable: executorReachable,
                executorTimeout: executorTimeout,
                statusDescription: statusDescription
        )

        cachedNodes

    }

    boolean needsReload(StatusNodeCacheKey nodeCacheKey, CacheNodeStatus oldNodes) {
        def project = nodeCacheKey.project
        def framework = frameworkService.getRundeckFramework()
        def now = new Date()
        def delay = nodeStatusCacheConfig(framework.projectManager.loadProjectConfig(project))
        if(now.time - oldNodes.cacheTime.time < delay){
            return false
        }
        return true
    }

    long nodeStatusCacheConfig(final IRundeckProjectConfig projectConfig) {
        if(projectConfig.hasProperty(PROJECT_STATUSNODECACHE_DELAY)){
            def delay = Long.parseLong(projectConfig.getProperty(PROJECT_STATUSNODECACHE_DELAY))
            return delay*1000
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

}
