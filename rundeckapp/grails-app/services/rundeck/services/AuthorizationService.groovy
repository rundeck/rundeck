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
import com.dtolabs.rundeck.core.authorization.AclRuleSetSource
import com.dtolabs.rundeck.core.authorization.AclsUtil
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.RuleEvaluator
import com.dtolabs.rundeck.core.authorization.ValidationSet
import com.dtolabs.rundeck.core.authorization.providers.*
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import org.springframework.beans.factory.InitializingBean
import rundeck.Storage
import rundeck.services.authorization.PoliciesValidation

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
        if(rundeckFilesystemPolicyAuthorization instanceof RuleEvaluator){
            return rundeckFilesystemPolicyAuthorization
        }
    }

    public PoliciesValidation validateYamlPolicy(String ident, String text) {
        validateYamlPolicy(null, ident, text)
    }
    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident identity string for the sources
     * @param text yaml aclpolicy text
     * @return validation
     */
    public PoliciesValidation validateYamlPolicy(String project, String ident, String text) {
        ValidationSet validation = new ValidationSet()
        def source = YamlProvider.sourceFromString(ident, text, new Date(),validation)
        def policies = YamlProvider.policiesFromSource(
                source,
                project ? AuthorizationUtil.projectContext(project) : null,
                validation
        )
        validation.complete();
        new PoliciesValidation(validation: validation, policies: policies)
    }
    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident identity string for the sources
     * @param text yaml aclpolicy text
     * @return validation
     */
    public PoliciesValidation validateYamlPolicy(String project, String ident, File source) {
        ValidationSet validation = new ValidationSet()
        PolicyCollection policies=null
        source.withInputStream {stream->
            def streamSource = YamlProvider.sourceFromStream(ident, stream, new Date(),validation)
            policies = YamlProvider.policiesFromSource(
                    streamSource,
                    project ? AuthorizationUtil.projectContext(project) : null,
                    validation
            )
        }
        validation.complete();
        new PoliciesValidation(validation: validation, policies: policies)
    }

    public PoliciesValidation validateYamlPolicy(File file) {
        ValidationSet validation = new ValidationSet()
        def policies = YamlProvider.policiesFromSource(YamlProvider.sourceFromFile(file,validation), null, validation)
        validation.complete();
        new PoliciesValidation(validation: validation, policies: policies)
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
        List<String> paths = listStoredPolicyPaths()

        def sources = paths.collect { path ->
            sourceCache.get(path)
        }.findAll{it!=null}
//        log.debug("loadStoredPolicies. paths: ${paths}, sources: ${sources}")
        new Policies(PoliciesCache.fromSources(sources))
    }

    /**
     * List the system aclpolicy file names, not including the dir path
     * @return
     */
    public List<String> listStoredPolicyFiles() {
        listStoredPolicyPaths().collect {
            it.substring(ACL_STORAGE_PATH_BASE.size())
        }
    }

    /**
     * List the system aclpolicy file paths, including the base dir name of acls/
     * @return
     */
    public List<String> listStoredPolicyPaths() {
        configStorageService.listDirPaths(ACL_STORAGE_PATH_BASE, ".*\\.aclpolicy")
    }

    /**
     *
     * @param file name without path
     * @return true if the policy file with the given name exists
     */
    public boolean existsPolicyFile(String file) {
        configStorageService.existsFileResource(ACL_STORAGE_PATH_BASE + file)
    }

    /**
     * @param fileName name of policy file, without path
     * @return text contents of the policy file
     */
    public String getPolicyFileContents(String fileName) {
        def resource = configStorageService.getFileResource(ACL_STORAGE_PATH_BASE + fileName)
        def file = resource.contents
        file.inputStream.getText()
    }

    /**
     * Store a system policy file
     * @param fileName name without path
     * @param fileText contents
     * @return size of bytes stored
     */
    public long storePolicyFileContents(String fileName, String fileText) {
        def bytes = fileText.bytes
        def result = configStorageService.writeFileResource(
                ACL_STORAGE_PATH_BASE + fileName,
                new ByteArrayInputStream(bytes),
                [:]
        )
        bytes.length
    }

    /**
     * Delete a policy file
     * @return true if successful
     */
    public boolean deletePolicyFile(String fileName) {
        configStorageService.deleteFileResource(ACL_STORAGE_PATH_BASE + fileName)
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
        YamlProvider.sourceFromString("[system:config]${path}", text, file.modificationTime,new ValidationSet())
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
