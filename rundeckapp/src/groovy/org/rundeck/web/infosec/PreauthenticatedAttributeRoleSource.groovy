package org.rundeck.web.infosec

import javax.servlet.http.HttpServletRequest
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Returns list of roles defined in a request attribute
 */
class PreauthenticatedAttributeRoleSource implements AuthorizationRoleSource {
    String attributeName
    String delimiter=','
    boolean enabled
    @Override
    Collection<String> getUserRoles(final String username, final HttpServletRequest request) {
        if(enabled && attributeName){
            def value=request.getAttribute(attributeName)
            if(value && value instanceof String){
                return value.split(" *${Pattern.quote(delimiter)} *").collect{it.trim()} as List<String>
            }
        }
        []
    }
}
