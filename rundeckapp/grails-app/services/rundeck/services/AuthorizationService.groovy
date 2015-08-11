package rundeck.services

import com.codahale.metrics.Gauge
import com.codahale.metrics.Meter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import com.dtolabs.rundeck.core.authorization.AclRuleSetSource
import com.dtolabs.rundeck.core.authorization.AclsUtil
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.RuleEvaluator
import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.core.authorization.providers.*
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import org.springframework.beans.factory.InitializingBean
import rundeck.Storage

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AuthorizationService implements InitializingBean{
    public static final String ACL_STORAGE_PATH_BASE = 'acls/'

    def configStorageService
    def rundeckFilesystemPolicyAuthorization
    def grailsApplication
    def metricService
    /**
     * Scheduled executor for retries
     */
    private ExecutorService executor = Executors.newFixedThreadPool(2)

    /**
     * Get the top-level system authorization
     * @return
     */
    def Authorization getSystemAuthorization() {
        if(metricService) {
            metricService.withTimer(this.class.name, 'getSystemAuthorization') {
                timedAuthorization(AclsUtil.append(rundeckFilesystemPolicyAuthorization, getStoredAuthorization()))
            }
        }else{
            AclsUtil.append(rundeckFilesystemPolicyAuthorization, getStoredAuthorization())
        }
    }

    private Authorization timedAuthorization(Authorization auth){
        Timer timer = metricService.timer(this.class.name + ".systemAuthorization", "evaluateTimer")
        Timer timerset = metricService.timer(this.class.name + ".systemAuthorization", "evaluateSetTimer")
        Meter meter= metricService.meter(this.class.name + ".systemAuthorization", "evaluateMeter")
        Meter meterset= metricService.meter(this.class.name + ".systemAuthorization", "evaluateSetMeter")
        new TimedAuthorization(auth,timer,timerset,meter,meterset)
    }
    /**
     * list group/role names used by the policies
     * @return
     */
    def Set<String> getRoleList() {
        AclsUtil.getGroups(AclsUtil.merge(getFilesystemRules(), getStoredPolicies()))
    }
    def AclRuleSetSource getFilesystemRules(){
        if(rundeckFilesystemPolicyAuthorization instanceof SAREAuthorization){
            return rundeckFilesystemPolicyAuthorization.policies
        }else if(rundeckFilesystemPolicyAuthorization instanceof RuleEvaluator){
            return rundeckFilesystemPolicyAuthorization
        }
    }

    public Validation validateYamlPolicy(String ident, String text) {
        validateYamlPolicy(null, ident, text)
    }
    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident identity string for the sources
     * @param text yaml aclpolicy text
     * @return validation
     */
    public Validation validateYamlPolicy(String project, String ident, String text) {
        YamlProvider.validate(
                YamlProvider.sourceFromString(ident, text, new Date()),
                project ? AuthorizationUtil.projectContext(project) : null
        )
    }
    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident identity string for the sources
     * @param text yaml aclpolicy text
     * @return validation
     */
    public Validation validateYamlPolicy(String project, String ident, File source) {
        Validation validation=null
        source.withInputStream {stream->
            validation=YamlProvider.validate(
                    YamlProvider.sourceFromStream(ident, stream, new Date()),
                    project ? AuthorizationUtil.projectContext(project) : null
            )
        }
        validation
    }

    public Validation validateYamlPolicy(File file){
        YamlProvider.validate(YamlProvider.sourceFromFile(file))
    }

    private Policies getStoredPolicies() {
        //TODO: cache
        loadStoredPolicies()
    }
    /**
     * return authorization from storage contents
     * @return authorization
     */
    public Authorization getStoredAuthorization() {
        //TODO: cache
        loadStoredAuthorization()
    }

    /**
     * load authorization from storage contents
     * @return authorization
     */
    private Policies loadStoredPolicies() {
        //TODO: list of files is always reloaded?
        def paths = configStorageService.listDirPaths(ACL_STORAGE_PATH_BASE, ".*\\.aclpolicy")

        def sources = paths.collect { path ->
            sourceCache.get(path)
        }.findAll{it!=null}
//        log.debug("loadStoredPolicies. paths: ${paths}, sources: ${sources}")
        new Policies(PoliciesCache.fromSources(sources))
    }

    /**
     * load authorization from storage contents
     * @return authorization
     */
    private Authorization loadStoredAuthorization() {
        return AclsUtil.createAuthorization(loadStoredPolicies())
    }

    private CacheableYamlSource loadYamlSource(String path){
        def exists = configStorageService.existsFileResource(path)
        log.debug("loadYamlSource. path: ${path}, exists: ${exists}")
        if(!exists){
            return null
        }
        def resource = configStorageService.getFileResource(path)
        def file = resource.contents
        def text =file.inputStream.getText()
        YamlProvider.sourceFromString("[system:config]${path}", text, file.modificationTime)
    }



    //basic creation, created via spec string in afterPropertiesSet()
    private LoadingCache<String, CacheableYamlSource> sourceCache =
            CacheBuilder.newBuilder()
                        .refreshAfterWrite(2, TimeUnit.MINUTES)
                        .build(
                    new CacheLoader<String, CacheableYamlSource>() {
                        public CacheableYamlSource load(String key) {
                            return loadYamlSource(key);
                        }
                    }
            );

    boolean needsReload(CacheableYamlSource source) {
        String path = source.identity.substring('[system:config]'.length())

        boolean needsReload=true
        Storage.withNewSession {
            def exists=configStorageService.existsFileResource(path)
            def resource=exists?configStorageService.getFileResource(path):null
            needsReload = resource == null ||
                    source.lastModified == null ||
                    resource.contents.modificationTime > source.lastModified
        }
        needsReload
    }
    @Override
    void afterPropertiesSet() throws Exception {
        def spec = grailsApplication.config.rundeck?.authorizationService?.sourceCache?.spec ?:
                "refreshAfterWrite=2m"

        log.debug("sourceCache: creating from spec: ${spec}")

        sourceCache = CacheBuilder.from(spec)
                                   .recordStats()
                                   .build(
                new CacheLoader<String, CacheableYamlSource>() {
                    public CacheableYamlSource load(String key) {
                        log.debug("sourceCache: loading source "+key)
                        return loadYamlSource(key);
                    }

                    @Override
                    ListenableFuture<CacheableYamlSource> reload(final String key, final CacheableYamlSource oldValue)
                            throws Exception
                    {
                        if (needsReload(oldValue)) {
                            ListenableFutureTask<CacheableYamlSource> task = ListenableFutureTask.create(
                                    new Callable<CacheableYamlSource>() {
                                        public CacheableYamlSource call() {

                                            log.debug("sourceCache: reloading source "+key)
                                            return loadYamlSource(key);
                                        }
                                    }
                            );
                            executor.execute(task);
                            return task;
                        } else {
                            return Futures.immediateFuture(oldValue)
                        }
                    }
                }
        )
        configStorageService?.addListener([
                resourceCreated:{String path->
                    log.debug("resourceCreated ${path}")
                },
                resourceModified:{String path->
                    log.debug("resourceModified ${path}, invalidating")
                    sourceCache.invalidate(path)
                },
                resourceDeleted:{String path->
                    log.debug("resourceDeleted ${path}, invalidating")
                    sourceCache.invalidate(path)
                },
        ] as StorageManagerListener)


        MetricRegistry registry = metricService?.getMetricRegistry()
        Util.addCacheMetrics(this.class.name + ".sourceCache",registry,sourceCache)

    }
}
