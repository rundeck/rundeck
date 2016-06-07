package com.dtolabs.rundeck.server.filters;

import com.dtolabs.rundeck.core.utils.GrailsServiceInjectorJobListener;
import org.apache.log4j.Logger;

import javax.management.relation.RoleList;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;

/**
 * Filter interrogates headers and modifies the request object to be passed down the filter chain
 *
 * @author John Stoltenborg
 */
public class AuthFilter implements Filter {

    private static final transient Logger LOG = Logger.getLogger(AuthFilter.class);


    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Enumeration<String> headerNames = httpRequest.getHeaderNames();

        final String forwardedUser = httpRequest.getHeader("X-Forwarded-User");

        ServletRequest requestModified =
                new HttpServletRequestWrapper((HttpServletRequest) request) {
                    @Override
                    public String getRemoteUser() {
                        return forwardedUser;
                    }

                    @Override
                    public Principal getUserPrincipal() {
                        Principal principle = new Principal() {
                            @Override
                            public String getName() {
                                return forwardedUser;
                            }
                        };
                        return principle;
                    }
                };

        //
        // Get the roles sent by the proxy and add them onto the request as an attribute for
        // PreauthenticatedAttributeRoleSource
        final String forwardedRoles = httpRequest.getHeader("X-Forwarded-Roles");
        requestModified.setAttribute("REMOTE_USER_GROUPS", forwardedRoles);

        filterChain.doFilter(requestModified, response);
    }

    public void destroy() {
    }
}