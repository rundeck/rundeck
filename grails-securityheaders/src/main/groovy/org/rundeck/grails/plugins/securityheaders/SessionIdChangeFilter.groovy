package org.rundeck.grails.plugins.securityheaders

import groovy.transform.CompileStatic
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Updates the SessionID after login, prevent session fixation attacks
 */
@CompileStatic
class SessionIdChangeFilter extends OncePerRequestFilter {

    public static final String UPDATED_SESSION_ID = SessionIdChangeFilter.class.name + 'UPDATED_SESSION_ID'

    @Override
    protected void doFilterInternal(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.context?.authentication?.authenticated) {
            def session = request.getSession(false)
            if (session) {
                if (!session.getAttribute(UPDATED_SESSION_ID)) {
                    logger.debug("reset ID for session ${session.getId()}, request ${request.getRequestURI()}, auth ${SecurityContextHolder.context.authentication.principal}")
                    request.changeSessionId()
                    session.setAttribute(UPDATED_SESSION_ID, "true")
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}
