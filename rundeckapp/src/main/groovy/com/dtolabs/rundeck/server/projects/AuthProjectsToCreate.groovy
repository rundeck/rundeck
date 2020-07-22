package com.dtolabs.rundeck.server.projects

import com.dtolabs.rundeck.core.authorization.AuthContext
import org.rundeck.core.auth.AuthConstants
import rundeck.services.FrameworkService

class AuthProjectsToCreate {
    private List authProjectsToCreate = []
    private AuthContext authContext
    private String project
    FrameworkService frameworkService

    public void updateList(AuthContext authContext, String project){
        this.authContext = authContext
        this.project = project
        def projectNames = frameworkService.projectNames(authContext)
        this.authProjectsToCreate = []
        projectNames.each{
            if(it != project && frameworkService.authorizeProjectResource(
                    authContext,
                    AuthConstants.RESOURCE_TYPE_JOB,
                    AuthConstants.ACTION_CREATE,
                    it
            )){
                authProjectsToCreate.add(it)
            }
        }
    }

    public List cachedList(AuthContext authContext, String projectName){
        if(!authContext.equals(authContext) || projectName != this.project){
            this.updateList(authContext, projectName)
        }

        return this.authProjectsToCreate
    }

    public boolean isAuthorized(AuthContext authContext, String projectName){
        if(authContext != this.authContext || projectName != this.project){
            this.updateList(authContext, projectName)
        }

        return this.authProjectsToCreate.contains(projectName)
    }
}
