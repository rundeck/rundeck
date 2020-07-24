package com.dtolabs.rundeck.server.projects

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.rundeck.core.auth.AuthConstants
import rundeck.services.FrameworkService

import javax.security.auth.Subject
import java.util.concurrent.TimeUnit

class AuthProjectsToCreate {
    private LoadingCache authProjectsToCreate
    FrameworkService frameworkService

    AuthProjectsToCreate() {
        this.authProjectsToCreate = initializeCache()
    }

    List<String> cachedList(Subject subject, String projectName){
        AuthProjectCacheKey key = new AuthProjectCacheKey(subject, projectName)
        List<String> cachedList = ((List<String>)this.authProjectsToCreate.get(key))

        if(projectName && cachedList.contains(projectName)){
            cachedList.remove(projectName)
        }

        return cachedList
    }

    private List<String> updateList(AuthProjectCacheKey authProjectCacheKey){
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(
                authProjectCacheKey.getSubject(),
                authProjectCacheKey.getProject()
        )
        def projectNames = frameworkService.projectNames(authContext)
        List authProjectsToCreate = []
        projectNames.each{
            if(frameworkService.authorizeProjectResource(
                    authContext,
                    AuthConstants.RESOURCE_TYPE_JOB,
                    AuthConstants.ACTION_CREATE,
                    it
            )){
                authProjectsToCreate.add(it)
            }
        }

        return authProjectsToCreate
    }

    private LoadingCache initializeCache() {
        LoadingCache<AuthProjectCacheKey, List<String>> loadingCache = CacheBuilder.newBuilder()
                .maximumSize(30)
                .expireAfterWrite(120, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<AuthProjectCacheKey, List<String>>() {
                            @Override
                            public List<String> load(AuthProjectCacheKey cacheKey) {
                                return updateList(cacheKey)
                            }
                        });

        return loadingCache
    }

    static final class AuthProjectCacheKey {
        Subject subject
        String project

        AuthProjectCacheKey(Subject subject, String project) {
            this.subject = subject
            this.project = project
        }

        @Override
        int hashCode() {
            return Objects.hash(this.subject, this.project);
        }

        @Override
        boolean equals(Object obj) {
            if (!(obj instanceof AuthProjectCacheKey)) {
                return false;
            }

            AuthProjectCacheKey c = (AuthProjectCacheKey) obj;

            return this.project == c.project &&
                    this.subject == c.subject
        }
    }
}
