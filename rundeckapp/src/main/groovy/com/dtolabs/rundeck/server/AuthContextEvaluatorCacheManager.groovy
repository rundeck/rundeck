package com.dtolabs.rundeck.server


import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import grails.events.annotation.Subscriber
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.app.authorization.AuthCache
import org.springframework.beans.factory.InitializingBean
import rundeck.services.Util

import java.util.concurrent.TimeUnit

@Slf4j
@CompileStatic
class AuthContextEvaluatorCacheManager implements AuthCache, InitializingBean{
    private final static long EXPIRATION_TIME_DEFAULT = 120

    long expirationTime
    boolean enabled
    private LoadingCache<AuthContextEvaluatorCacheKey,Set<Decision>> authContextEvaluatorCache
    MetricService metricService

    @Override
    void afterPropertiesSet() throws Exception {
        this.authContextEvaluatorCache = initializeAuthContextEvaluatorCache()
        Util.
            addCacheMetrics(
                this.class.name + ".authContextEvaluatorCache",
                metricService?.getMetricRegistry(),
                authContextEvaluatorCache
            )
    }

    Decision evaluate(
        AuthContext authContext,
        Map<String, String> resource,
        String action,
        String project
    ) {
        if (this.enabled) {
            AuthContextEvaluatorCacheKey key = new AuthContextEvaluatorCacheKey(authContext, resource, action, project)
            return this.authContextEvaluatorCache.get(key).first()
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
            return this.authContextEvaluatorCache.get(key)
        }

        return authContext.evaluate(resources, actions,
                project ? AuthorizationUtil.projectContext(project) : AuthorizationUtil.RUNDECK_APP_ENV)
    }

    @Subscriber('acl.modified')
    void invalidateAllCacheEntries(Map data){
        log.debug("acl.modified received: $data")
        if(enabled) {
            this.authContextEvaluatorCache?.invalidateAll()
        }
    }

    private LoadingCache<AuthContextEvaluatorCacheKey,Set<Decision>> initializeAuthContextEvaluatorCache() {
        LoadingCache<AuthContextEvaluatorCacheKey, Set<Decision>> cache =
            CacheBuilder.newBuilder()
                .expireAfterWrite(this.expirationTime ?: EXPIRATION_TIME_DEFAULT, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<AuthContextEvaluatorCacheKey, Set<Decision>>() {
                            @Override
                            public Set<Decision> load(AuthContextEvaluatorCacheKey cacheKey) {
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

        AuthContextEvaluatorCacheKey(AuthContext authContext, Set<Map<String, String>> resources, Set<String> actions, String project) {
            this.authContext = authContext
            this.resources = resources
            this.actions = actions
            this.project = project
        }

        AuthContextEvaluatorCacheKey(AuthContext authContext, Map<String, String> resource, String action, String project) {
            this.authContext = authContext
            this.resourceMap = resource
            this.action = action
            this.project = project
        }

        Set<Decision> doEvaluation() {
            if(resources){
                return authContext.evaluate(resources, actions,
                                            project ? AuthorizationUtil.projectContext(project) : AuthorizationUtil.RUNDECK_APP_ENV)
            }else{
                Set<Decision> decisions = new HashSet<Decision>()
                decisions.add authContext.
                    evaluate(
                        resourceMap,
                        action,
                        project ? AuthorizationUtil.projectContext(project) : AuthorizationUtil.RUNDECK_APP_ENV
                    )
                return decisions
            }
        }

        boolean compareAuthContext(AuthContextEvaluatorCacheKey key) {
            if (this.authContext instanceof UserAndRolesAuthContext && key.
                authContext instanceof UserAndRolesAuthContext) {
                UserAndRolesAuthContext uar1 = (UserAndRolesAuthContext) authContext
                UserAndRolesAuthContext uar2 = (UserAndRolesAuthContext) key.authContext
                return uar1.getUsername() == uar2?.getUsername() && uar1.getRoles()?.equals(uar2?.getRoles()) && uar1.getUrn() == uar2.getUrn()
            }

            return this.authContext == key.authContext
        }

        int hashAuthContext() {
            if (this.authContext instanceof UserAndRolesAuthContext) {
                UserAndRolesAuthContext uar1 = (UserAndRolesAuthContext) authContext
                return Objects.hash(
                    uar1.getUsername()?.hashCode(),
                    uar1.getRoles()?.hashCode(),
                    uar1.getUrn()?.hashCode()
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

        @Override
        String toString() {
            "Key: [${authContext}]: ${resources?resources.size():1} res ${resourceMap}, ${actions?:action}, ${project}"
        }
    }
}
