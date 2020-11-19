package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.AclsUtil
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.AuthorizationService
import rundeck.services.FrameworkService

import javax.security.auth.Subject

@CompileStatic
@Slf4j
class AuthContextProviderService implements AuthContextProvider {

    @Autowired AuthorizationService authorizationService
    @Autowired FrameworkService frameworkService

    public UserAndRolesAuthContext getAuthContextForSubject(Subject subject) {
        if (!subject) {
            throw new RuntimeException("getAuthContextForSubject: Cannot get AuthContext without subject")
        }
        return new SubjectAuthContext(subject, authorizationService.systemAuthorization)
    }
    /**
     * Extend a generic auth context, with project-specific authorization
     * @param orig original auth context
     * @param project project name
     * @return new AuthContext with project-specific authorization added
     */
    public UserAndRolesAuthContext getAuthContextWithProject(UserAndRolesAuthContext orig, String project) {
        if (!orig) {
            throw new RuntimeException("getAuthContextWithProject: Cannot get AuthContext without orig")
        }
        if (!project) {
            throw new RuntimeException("getAuthContextWithProject: Cannot get AuthContext without project")
        }
        if (!frameworkService.existsFrameworkProject(project)) {
            throw new RuntimeException("getAuthContextForSubjectAndProject: Project does not exist: $project")
        }

        def projectAuth = authorizationService.loadStoredProjectAuthorization(project)
        log.debug("getAuthContextWithProject ${project}, orig: ${orig}, project auth ${projectAuth}")
        return orig.combineWith(projectAuth)
    }

    public UserAndRolesAuthContext getAuthContextForSubjectAndProject(Subject subject, String project) {
        if (!subject) {
            throw new RuntimeException("getAuthContextForSubjectAndProject: Cannot get AuthContext without subject")
        }
        if (!project) {
            throw new RuntimeException("getAuthContextForSubjectAndProject: Cannot get AuthContext without project")
        }
        if (!frameworkService.existsFrameworkProject(project)) {
            throw new RuntimeException("getAuthContextForSubjectAndProject: Project does not exist: $project")
        }

        def projectAuth = authorizationService.loadStoredProjectAuthorization(project)
        def authorization = AclsUtil.append(authorizationService.systemAuthorization, projectAuth)
        log.
            debug(
                "getAuthContextForSubjectAndProject ${project}, authorization: ${authorization}, project auth " +
                "${projectAuth}"
            )
        return new SubjectAuthContext(subject, authorization)
    }

    public UserAndRolesAuthContext getAuthContextForUserAndRolesAndProject(
        String user,
        List<String> rolelist,
        String project
    ) {
        getAuthContextWithProject(getAuthContextForUserAndRoles(user, rolelist), project)
    }

    public UserAndRolesAuthContext getAuthContextForUserAndRoles(String user, List<String> rolelist) {
        if (!(null != user && null != rolelist)) {
            throw new RuntimeException(
                "getAuthContextForUserAndRoles: Cannot get AuthContext without user, roles: " +
                "${user}, ${rolelist}"
            )
        }
        //create fake subject
        Subject subject = new Subject()
        subject.getPrincipals().add(new Username(user))
        rolelist.each { String s ->
            subject.getPrincipals().add(new Group(s))
        }
        return new SubjectAuthContext(subject, authorizationService.systemAuthorization)
    }
}
