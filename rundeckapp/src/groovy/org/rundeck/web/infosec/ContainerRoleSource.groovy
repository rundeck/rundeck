package org.rundeck.web.infosec

import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.FrameworkService

import javax.servlet.http.HttpServletRequest

/**
 * Returns list of known roles that user is in via {@link HttpServletRequest#isUserInRole(java.lang.String)}
 */
class ContainerRoleSource implements AuthorizationRoleSource {
    boolean enabled
    @Autowired
    def FrameworkService frameworkService
    @Override
    Collection<String> getUserRoles(final String username, final HttpServletRequest request) {
        def roles=new ArrayList<String>()
        //try to determine roles based on aclpolicy group definitions
        frameworkService.getFrameworkRoles().each { rolename ->
            if (request.isUserInRole(rolename)) {
                roles<<rolename
            }
        }
        roles
    }
}
