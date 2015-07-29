package rundeck.services

import com.dtolabs.rundeck.core.authorization.AclRuleSetSource
import com.dtolabs.rundeck.core.authorization.AclsUtil
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.RuleEvaluator
import com.dtolabs.rundeck.core.authorization.providers.CacheableYamlSource
import com.dtolabs.rundeck.core.authorization.providers.Policies
import com.dtolabs.rundeck.core.authorization.providers.PoliciesCache
import com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization
import com.dtolabs.rundeck.core.authorization.providers.YamlProvider
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import org.rundeck.storage.api.Resource
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
    /**
     * Scheduled executor for retries
     */
    private ExecutorService executor = Executors.newFixedThreadPool(2)

    /**
     * Get the top-level system authorization
     * @return
     */
    def Authorization getSystemAuthorization() {
        AclsUtil.append rundeckFilesystemPolicyAuthorization, getStoredAuthorization()
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
        configStorageService.addListener([
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


    }
}
