package org.rundeck.web.infosec

import com.dtolabs.rundeck.core.authentication.Group

import javax.security.auth.Subject
import javax.servlet.http.HttpServletRequest

/**
 * Return a list of roles by introspecting the userPrincipal object
 */
class ContainerPrincipalRoleSource implements AuthorizationRoleSource {
    boolean enabled
    @Override
    Collection<String> getUserRoles(final String username, final HttpServletRequest request) {
        def roles=new ArrayList<String>()
        def principal = request.userPrincipal
        if (principal.hasProperty('subject')) {
            Subject osubject = (Subject) principal.subject
            osubject.getPrincipals().each { p ->
                if (p.class.name.equals('org.eclipse.jetty.plus.jaas.JAASRole') || p.class.name.contains('Role')) {
                    roles<<p.name
                }
            }
        } else if (principal.hasProperty('roles')) {
            if (principal.roles instanceof Iterator) {
                def Iterator iter = principal.roles
                while (iter.hasNext()) {
                    def role = iter.next()
                    if (role.rolename) {
                        roles<<role.rolename
                    } else if (role instanceof String) {
                        roles<<role
                    }

                }
            } else if (principal.roles instanceof Collection || principal.roles instanceof Object[]) {
                principal.roles?.each { name ->
                    roles<<name
                }
            } else {
                principal.roles?.members.each { group ->
                    roles<<group.name
                }
            }
        }
        roles
    }
}
