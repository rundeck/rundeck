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

import com.codahale.metrics.Meter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import com.dtolabs.rundeck.core.authorization.*
import com.dtolabs.rundeck.core.authorization.providers.CacheableYamlSource
import com.dtolabs.rundeck.core.authorization.providers.Policies
import com.dtolabs.rundeck.core.authorization.providers.PoliciesCache
import com.dtolabs.rundeck.core.authorization.providers.YamlProvider
import com.dtolabs.rundeck.core.config.Features
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import groovy.transform.CompileStatic
import org.rundeck.app.acl.ACLManager
import org.springframework.beans.factory.InitializingBean
import rundeck.Storage
import rundeck.services.feature.FeatureService

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AuthorizationService implements InitializingBean, EventPublisher, ACLManager{
    public static final String ACL_STORAGE_PATH_BASE = 'acls/'

    def configStorageService
    @Delegate
    ACLManager aclManagerService
    def rundeckFilesystemPolicyAuthorization
    def grailsApplication
    def metricService
    def frameworkService
    FeatureService featureService
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

    private Authorization timedAuthorization(AclRuleSetAuthorization auth){
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
        if(rundeckFilesystemPolicyAuthorization instanceof AclRuleSetSource){
            return rundeckFilesystemPolicyAuthorization
        }
    }


    private Policies getStoredPolicies() {
        loadCachedStoredPolicies()
    }
    /**
     * return authorization from storage contents
     * @return authorization
     */
    public Authorization getStoredAuthorization() {
        loadStoredAuthorization()
    }

    private Policies loadCachedStoredPolicies(){
        storedPolicyPathsCache.get("authorization:policies")
    }

    /**
     * load authorization from storage contents
     * @return authorization
     */
    private Policies loadStoredPolicies() {
        //TODO: list of files is always reloaded?
        List<String> paths = aclManagerService.listStoredPolicyFiles()

        def sources = paths.collect { path ->
            sourceCache.get(path)
        }.findAll{it!=null}
//        log.debug("loadStoredPolicies. paths: ${paths}, sources: ${sources}")
        new Policies(PoliciesCache.fromSources(sources))
    }

    @Subscriber("rundeck.bootstrap")
    @CompileStatic
    public void init() {
        if(!featureService.featurePresent(Features.AUTH_SVC_BOOTSTRAP_WARMUP_CACHE)){
            return
        }
        log.debug("init...")
        long start = System.currentTimeMillis()
        loadCachedStoredPolicies()
        log.debug("init: loadCachedStoredPolicies: ${System.currentTimeMillis() - start}")
    }

    /**
     * load authorization from storage contents
     * @return authorization
     */
    private Authorization loadStoredAuthorization() {
        return AclsUtil.createAuthorization(loadCachedStoredPolicies())
    }

    private CacheableYamlSource loadYamlSource(String file){
        def exists = aclManagerService.existsPolicyFile(file)
        log.debug("loadYamlSource. path: ${file}, exists: ${exists}")
        if(!exists){
            return null
        }
        def aclPolicy = aclManagerService.getAclPolicy(file)
        YamlProvider.sourceFromString("[system:config]${file}", aclPolicy.text, aclPolicy.modified, new ValidationSet())
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

    private LoadingCache<String, Policies> storedPolicyPathsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<String, Policies>() {
                        @Override
                        public Policies load(String path) {
                            return loadStoredPolicies()
                        }
                    });

    boolean needsReload(CacheableYamlSource source) {
        String file = source.identity.substring('[system:config]'.length())

        boolean needsReload=true
        Storage.withNewSession {
            def exists=aclManagerService.existsPolicyFile(file)
            def resource=exists?aclManagerService.getAclPolicy(file):null
            needsReload = resource == null ||
                    source.lastModified == null ||
                    resource.modified > source.lastModified
        }
        needsReload
    }

    void cleanCaches(String path){
        sourceCache.invalidate(path)
        storedPolicyPathsCache.invalidateAll()
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
                    if(path.startsWith(ACL_STORAGE_PATH_BASE) && path.endsWith('.aclpolicy')) {
                        invalidateEntriesAclCache(path)
                    }
                },
                resourceModified:{String path->
                    log.debug("resourceModified ${path}, invalidating")
                    sourceCache.invalidate(path)
                    if(path.startsWith(ACL_STORAGE_PATH_BASE) && path.endsWith('.aclpolicy')) {
                        invalidateEntriesAclCache(path)
                    }
                },
                resourceDeleted:{String path->
                    log.debug("resourceDeleted ${path}, invalidating")
                    sourceCache.invalidate(path)
                    if(path.startsWith(ACL_STORAGE_PATH_BASE) && path.endsWith('.aclpolicy')) {
                        invalidateEntriesAclCache(path)
                    }
                },
        ] as StorageManagerListener)


        MetricRegistry registry = metricService?.getMetricRegistry()
        Util.addCacheMetrics(this.class.name + ".sourceCache",registry,sourceCache)

    }

    private void invalidateEntriesAclCache(String path) {
        cleanCaches(path)

        if (frameworkService.isClusterModeEnabled()) {
            sendAndReceive(
                    'cluster.clearAclCache',
                    [
                            uuidSource: frameworkService.getServerUUID(),
                            path: path
                    ]
            ) { resp ->
                log.debug("Cleaning the cache in the cluster is ${resp.clearCacheState}: ${resp.reason}")
            }
        }
    }
}
