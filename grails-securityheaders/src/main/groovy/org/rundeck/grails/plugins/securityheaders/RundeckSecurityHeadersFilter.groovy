/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.grails.plugins.securityheaders

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RundeckSecurityHeadersFilter extends OncePerRequestFilter implements ApplicationContextAware {
    private static final transient Logger LOG = LoggerFactory.getLogger(RundeckSecurityHeadersFilter.class);

    def Map config

    ApplicationContext applicationContext
    boolean enabled

    @Override
    void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        if (enabled) {
            def securityHeaders = applicationContext.getBeansOfType(SecurityHeaderProvider)
            for (SecurityHeaderProvider provider : securityHeaders.values()) {
                def provSettings = config.get(provider.name)
                def confEnabled = provSettings?.get('enabled')
                if (confEnabled == null) {
                    confEnabled = provider.defaultEnabled
                } else {
                    confEnabled = confEnabled in ['true', true]
                }
                if (confEnabled) {
                    Map provConf = provSettings?.get('config') ?: [:]
                    def list = provider.getSecurityHeaders(request, response, provConf)
                    if (list) {
                        list.each { SecurityHeader header ->
                            response.addHeader(header.name, header.value)
                        }
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }
}
