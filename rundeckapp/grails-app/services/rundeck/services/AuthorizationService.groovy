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
import com.dtolabs.rundeck.core.authorization.providers.*
import com.dtolabs.rundeck.core.config.Features
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import grails.compiler.GrailsCompileStatic
import grails.events.annotation.Subscriber
import grails.events.bus.EventBusAware
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.app.acl.ACLCacheControl
import org.rundeck.app.acl.ACLFileManagerListener
import org.rundeck.app.acl.AclPolicyFile
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.acl.ContextACLManager
import org.rundeck.app.auth.AuthManager
import org.rundeck.app.cluster.ClusterInfo
import org.springframework.beans.factory.InitializingBean
import rundeck.services.feature.FeatureService

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@GrailsCompileStatic
class AuthorizationService implements AuthManager, InitializingBean, EventBusAware, ACLCacheControl {
    public static final String SYSTEM_CONFIG_PATH = "system:config"

    @Delegate
    ContextACLManager<AppACLContext> aclStorageFileManager
    @Delegate
    Validator rundeckYamlAclValidator
    AclRuleSetAuthorization rundeckFilesystemPolicyAuthorization
    ConfigurationService configurationService
    MetricService metricService
    ClusterInfo clusterInfoService
    FeatureService featureService
    /**
     * Scheduled executor for retries
     */
    private ExecutorService executor = Executors.newFixedThreadPool(2)

    /**
     * Get the top-level system authorization
     * @return
     */
    private Authorization getSystemAuthorization() {
        if(metricService) {
            metricService.withTimer(this.class.name, 'getSystemAuthorization') {
                timedAuthorization(AclsUtil.append(rundeckFilesystemPolicyAuthorization, getStoredAuthorization()))
            }
        }else{
            AclsUtil.append(rundeckFilesystemPolicyAuthorization, getStoredAuthorization())
        }
    }

    @Override
    Authorization getAuthorizationForSubject(final UserAndRoles subject) {
        return getSystemAuthorization()
    }
    @Override
    Authorization getProjectAuthorizationForSubject(UserAndRoles subject, String project) {
        AclsUtil.append(getAuthorizationForSubject(subject), loadStoredProjectAuthorization(project))
    }
    /**
     *
     * @param project
     * @return
     * @deprecated use {@link #getProjectAuthorizationForSubject(com.dtolabs.rundeck.core.authorization.UserAndRoles, java.lang.String)}
     */
    @Deprecated
    Authorization getProjectAuthorization(String project){
        loadStoredProjectAuthorization(project)
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


    private AclRuleSetSource getStoredPolicies() {
        loadCachedStoredPolicies()
    }
    /**
     * return authorization from storage contents
     * @return authorization
     */
    public Authorization getStoredAuthorization() {
        loadStoredAuthorization()
    }

    private AclRuleSetSource loadCachedStoredPolicies(){
        storedPolicyPathsCache.get(SYSTEM_CONFIG_PATH)
    }
    private AclRuleSetSource loadCachedStoredPolicies(String project){
        storedPolicyPathsCache.get("project:"+project)
    }

    /**
     * load Policies from storage contents
     * @return policies
     */
    def AclRuleSetSource loadStoredPolicies() {
        //TODO: list of files is always reloaded?
        List<String> paths = aclStorageFileManager.listStoredPolicyFiles(AppACLContext.system())

        def sources = paths.collect { path ->
            try{
                return sourceCache.getUnchecked(new SourceKey(system:true,file:path))
            }catch (Throwable t) {
                log.warn("failed getting cached source for $path: "+t.message)
                log.debug("failed getting cached source for $path", t)
                return null
            }
        }.findAll{it!=null}
//        log.debug("loadStoredPolicies. paths: ${paths}, sources: ${sources}")
        new Policies(PoliciesCache.fromSources(sources))
    }

    /**
     * load Policies from storage contents for the project
     * @return policies
     */
    def Policies loadStoredPolicies(String project) {
        //TODO: list of files is always reloaded?
        List<String> paths = aclStorageFileManager.listStoredPolicyFiles(AppACLContext.project(project))

        def sources = paths.collect { path ->
            try {
                return sourceCache.getUnchecked(new SourceKey(project:project,file:path))
            }catch (Throwable t) {
                log.warn("failed getting cached source for $project : $path: "+t.message)
                log.debug("failed getting cached source for $project : $path", t)
                return null
            }
        }.findAll{it!=null}

        def context = AuthorizationUtil.projectContext(project)
        new Policies(PoliciesCache.fromSources(sources, context))
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
    /**
     * load authorization from storage contents
     * @return authorization
     */
    private Authorization loadStoredProjectAuthorization(String project) {
        return AclsUtil.createAuthorization(loadCachedStoredPolicies(project))
    }

    private CacheableYamlSource loadYamlSource(SourceKey key){
        def exists = aclStorageFileManager.existsPolicyFile(key.context, key.file)
        if(!exists){
            throw new Exception("path does not exist: ${key.context} - ${key.file}")
        }
        def aclPolicy = aclStorageFileManager.getAclPolicy(key.context, key.file)
        if(!aclPolicy){
            throw new Exception("path does not exist: ${key.context} - ${key.file}")
        }
        YamlProvider.sourceFromString("[$key.identity]${key.file}", aclPolicy.inputStream.text, aclPolicy.modified, new ValidationSet())
    }

    @ToString
    @EqualsAndHashCode
    static class SourceKey{
        boolean system
        String project
        String file
        public String getIdentity(){
            if(system){
                return SYSTEM_CONFIG_PATH
            }else {
                return "project:$project"
            }
        }
        public AppACLContext getContext(){
            if(system){
                return AppACLContext.system()
            }else{
                return AppACLContext.project(project)
            }
        }
        static SourceKey forContext(AppACLContext context, String file){
            return new SourceKey(system: context.system, project: context.project, file: file)
        }
    }

    //basic creation, created via spec string in afterPropertiesSet()
    private LoadingCache<SourceKey, CacheableYamlSource> sourceCache =
            CacheBuilder.newBuilder()
                        .refreshAfterWrite(2, TimeUnit.MINUTES)
                        .build(
                    new CacheLoader<SourceKey, CacheableYamlSource>() {
                        public CacheableYamlSource load(SourceKey key) {
                            return loadYamlSource(key);
                        }
                    }
            );

    private LoadingCache<String, AclRuleSetSource> storedPolicyPathsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<String, AclRuleSetSource>() {
                        @Override
                        public AclRuleSetSource load(String path) {
                            return path == SYSTEM_CONFIG_PATH ?
                                   loadStoredPolicies() :
                                   loadStoredPolicies(path.substring('project:'.length())
                            )
                        }
                    });

    boolean needsReload(SourceKey key,CacheableYamlSource source) {
        String file = key.file

        boolean needsReload=true
        def exists= aclStorageFileManager.existsPolicyFile(key.context,file)
        AclPolicyFile resource= exists ? aclStorageFileManager.getAclPolicy(key.context, file) : null
        needsReload = resource == null ||
                    source.lastModified == null ||
                    resource.modified.time > source.lastModified.time
        needsReload
    }

    /**
     * Clean system path caches
     * @param systemPath'
     * @deprecated use @{link #cleanAclCache}
     */
    void cleanSystemCaches(String systemPath) {
        cleanCaches(SourceKey.forContext(AppACLContext.system(), systemPath))
    }

    /**
     * Clean context caches
     * @param path
     */
    void cleanAclCache(AppACLContext context, String path) {
        cleanCaches(SourceKey.forContext(context, path))
    }

    void cleanCaches(SourceKey key){
        sourceCache.invalidate(key)
        storedPolicyPathsCache.invalidate(key.identity)
        eventBus.notify('acl.modified', [storage:'core-storage', context: key.context, path: key.file])
    }

    @Override
    void afterPropertiesSet() throws Exception {
        def spec = configurationService.getString('authorizationService.sourceCache.spec', "refreshAfterWrite=2m")

        log.debug("sourceCache: creating from spec: ${spec}")

        sourceCache = CacheBuilder.from(spec)
                                   .recordStats()
                                   .build(
                new CacheLoader<SourceKey, CacheableYamlSource>() {
                    public CacheableYamlSource load(SourceKey key) {
                        log.debug("sourceCache: loading source "+key)
                        return loadYamlSource(key);
                    }

                    @Override
                    ListenableFuture<CacheableYamlSource> reload(final SourceKey key, final CacheableYamlSource oldValue)
                            throws Exception
                    {
                        if (needsReload(key,oldValue)) {
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
        aclStorageFileManager?.addListenerMap { AppACLContext context ->
            [
                aclFileUpdated: this.&pathWasModified.curry(context),
                aclFileDeleted: this.&pathWasModified.curry(context),
            ] as ACLFileManagerListener
        }

        MetricRegistry registry = metricService?.getMetricRegistry()
        Util.addCacheMetrics(this.class.name + ".sourceCache",registry,sourceCache)
    }

    /**
     * Called by ACLFileManagerListener
     * @param context acl context
     * @param path path
     */
    void pathWasModified(AppACLContext context, String path){
        log.debug("Path modified/deleted: ${path}, invalidating")
        cleanAclCache(context, path)
        if (clusterInfoService.isClusterModeEnabled()) {
            eventBus.sendAndReceive(
                    'cluster.clearAclCache',
                    [
                        path      : path,
                        project   : context.isSystem() ? null : context.project,
                        system    : context.isSystem()
                    ]
            ) { Map resp ->
                log.debug("Cleaning the cache in the cluster is ${resp.clearCacheState}: ${resp.reason}")
            }
        }
    }
}
