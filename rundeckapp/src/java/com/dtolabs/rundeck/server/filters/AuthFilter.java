/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.server.filters;

import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

/**
 * Filter interrogates headers and modifies the request object to be passed down the filter chain
 *
 * @author John Stoltenborg
 */

public class AuthFilter implements Filter {

    private static final transient Logger LOG = Logger.getLogger(AuthFilter.class);

    boolean enabled;
    String rolesAttribute;
    String userNameHeader;
    String rolesHeader;

    public void init(FilterConfig filterConfig) throws ServletException {

        WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(filterConfig
                                                                                                          .getServletContext());
        GrailsApplication grailsApplication = springContext.getBean(GrailsApplication.class);

        if (grailsApplication.equals(null)) {
            throw new IllegalStateException("grailsApplication not found in context");
        }

        Map map = grailsApplication.getFlatConfig();
        Object o = map.get("rundeck.security.authorization.preauthenticated.enabled");
        enabled = Boolean.parseBoolean(o.toString());
        rolesAttribute = (String) map.get("rundeck.security.authorization.preauthenticated.attributeName");
        rolesHeader = (String) map.get("rundeck.security.authorization.preauthenticated.userRolesHeader");
        userNameHeader = (String) map.get("rundeck.security.authorization.preauthenticated.userNameHeader");


    }

    public void doFilter(
            ServletRequest request, ServletResponse response,
            FilterChain filterChain
    )
            throws IOException, ServletException
    {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletRequest requestModified;
        if (userNameHeader != null) {
            final String forwardedUser = httpRequest.getHeader(userNameHeader);

            LOG.info("User header " + userNameHeader);
            LOG.info("User / UUID recieved " + forwardedUser);
            requestModified =
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
        } else {
            requestModified = httpRequest;
        }
        if (rolesAttribute != null && rolesHeader != null) {
            // Get the roles sent by the proxy and add them onto the request as an attribute for
            // PreauthenticatedAttributeRoleSource
            final String forwardedRoles = httpRequest.getHeader(rolesHeader);
            requestModified.setAttribute(rolesAttribute, forwardedRoles);
            LOG.info("Roles header " + rolesHeader);
            LOG.info("Roles received " + forwardedRoles);
        }
        filterChain.doFilter(requestModified, response);
    }

    public void destroy() {
    }
}