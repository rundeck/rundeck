package com.dtolabs.rundeck.server


import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthEvaluator
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import rundeck.services.FrameworkService

import java.util.concurrent.TimeUnit

class AuthContextEvaluatorCacheManager implements AuthEvaluator{
    private final static long EXPIRATION_TIME_DEFAULT = 120

    long expirationTime
    boolean enabled
    private LoadingCache authContextEvaluatorCache

    AuthContextEvaluatorCacheManager() {
        this.authContextEvaluatorCache = initializeAuthContextEvaluatorCache()
    }

    Decision evaluate(AuthContext authContext,
                      Map<String, String> resource,
                      String action,
                      String project){
        if(this.enabled) {
            AuthContextEvaluatorCacheKey key = new AuthContextEvaluatorCacheKey(authContext, resource, action, project)
            return (Decision) this.authContextEvaluatorCache.get(key)
        }

        return authContext.evaluate(resource, action,
                project ? AuthorizationUtil.projectContext(project) : AuthorizationUtil.RUNDECK_APP_ENV)
    }

    Set<Decision> evaluate(AuthContext authContext,
                           Set<Map<String, String>> resources,
                           Set<String> actions,
                           String project){
        if(this.enabled) {
            AuthContextEvaluatorCacheKey key = new AuthContextEvaluatorCacheKey(authContext, resources, actions, project)
            return (Set<Decision>) this.authContextEvaluatorCache.get(key)
        }

        return authContext.evaluate(resources, actions,
                project ? AuthorizationUtil.projectContext(project) : AuthorizationUtil.RUNDECK_APP_ENV)
    }

    void invalidateAllCacheEntries(){
        if(enabled) {
            this.authContextEvaluatorCache?.invalidateAll()
        }
    }

    private LoadingCache initializeAuthContextEvaluatorCache() {
        LoadingCache<AuthContextEvaluatorCacheKey, Object> cache = CacheBuilder.newBuilder()
                .expireAfterWrite(this.expirationTime ?: EXPIRATION_TIME_DEFAULT, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<AuthContextEvaluatorCacheKey, Object>() {
                            @Override
                            public Object load(AuthContextEvaluatorCacheKey cacheKey) {
                                return cacheKey.doEvaluation()
                            }
                        });

        return cache
    }

    static final class AuthContextEvaluatorCacheKey {
        AuthContext authContext
        Set<Map<String, String>> resources
        Map<String, String> resourceMap
        Set<String> actions
        String action
        String project
        private Closure evaluate

        AuthContextEvaluatorCacheKey(AuthContext authContext, Set<Map<String, String>> resources, Set<String> actions, String project) {
            this.authContext = authContext
            this.resources = resources
            this.actions = actions
            this.project = project
            this.evaluate = {AuthContextEvaluatorCacheKey key ->
                return authContext.evaluate(resources, actions,
                        project ? AuthorizationUtil.projectContext(project) : AuthorizationUtil.RUNDECK_APP_ENV)
            }
        }

        AuthContextEvaluatorCacheKey(AuthContext authContext, Map<String, String> resource, String action, String project) {
            this.authContext = authContext
            this.resourceMap = resource
            this.action = action
            this.project = project
            this.evaluate = {
                return authContext.evaluate(resource, action,
                        project ? AuthorizationUtil.projectContext(project) : AuthorizationUtil.RUNDECK_APP_ENV)
            }
        }

        def doEvaluation(){
            evaluate.call()
        }

        boolean compareAuthContext(AuthContextEvaluatorCacheKey key){
            if(this.authContext instanceof SubjectAuthContext){
                return this.authContext.getUsername() == key.authContext?.getUsername() && this.authContext.getRoles()?.equals(key.authContext?.getRoles())
            }

            return this.authContext == key.authContext
        }

        int hashAuthContext(){
            if(this.authContext instanceof SubjectAuthContext){
                return Objects.hash(
                        this.authContext?.getUsername()?.hashCode(),
                        this.authContext?.getRoles()?.hashCode()
                )
            }

            return this.authContext?.hashCode()
        }

        @Override
        int hashCode() {
            return Objects.hash(
                    this.hashAuthContext(),
                    this.resources?.hashCode(),
                    this.actions?.hashCode(),
                    this.resourceMap?.hashCode(),
                    this.action?.hashCode(),
                    this.project?.hashCode()
            )
        }

        @Override
        boolean equals(Object obj) {
            if (!(obj instanceof AuthContextEvaluatorCacheKey)) {
                return false;
            }

            AuthContextEvaluatorCacheKey c = (AuthContextEvaluatorCacheKey) obj;

            return this.project == c.project &&
                    this.actions == c.actions &&
                    this.action == c.action &&
                    this.resourceMap == c.resourceMap &&
                    this.resources == c.resources &&
                    this.compareAuthContext(c)
        }
    }
}
