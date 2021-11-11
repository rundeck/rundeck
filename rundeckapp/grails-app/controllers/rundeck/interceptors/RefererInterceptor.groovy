/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
package rundeck.interceptors

import org.rundeck.app.access.InterceptorHelper
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ConfigurationService

import javax.servlet.http.HttpServletResponse
import java.util.regex.Pattern

/**
 * Requires Referer header matches the grails.serverURL value for POST requests, prevents CSRF attacks.
 *
 * Configuration:
 * <code><pre>
 * #Set HTTP Method to filter based on Referer header.  Can be POST, or "*" for all methods. Default:
 * # NONE (disabled)
 * rundeck.security.csrf.referer.filterMethod=NONE|POST|*
 *
 * # Allow /api/* requests without requireing matching Referer header. Default: true.
 * rundeck.security.csrf.referer.allowApi=true|false
 *
 * # If server URL is HTTPS, Require referer header to be from HTTPS version of server URL, if false allow HTTP as
 * # well. Default: true.
 * rundeck.security.csrf.referer.requireHttps=true|false
 * </pre></code>
 */
class RefererInterceptor {

    @Autowired
    ConfigurationService configurationService
    InterceptorHelper interceptorHelper

    int order = HIGHEST_PRECEDENCE + 29

    RefererInterceptor() {
        matchAll()
    }

    boolean before() {
        if(interceptorHelper.matchesAllowedAsset(controllerName, request)) return true
        // Set HTTP Method to filter based on Referer header.  Can be POST, or "*" for all methods. Default:
        // NONE (disabled)
        def csrf = configurationService.getString('security.csrf.referer.filterMethod', 'NONE')
        if (!csrf || csrf == 'NONE') {
            return true
        }

        // Allow /api/* access without matching Referer header. Default: true.
        def allowApi = configurationService.getBoolean('security.csrf.referer.allowApi', true)
        if (request.api_version && allowApi) {
            return true
        }

        def urlString = grailsApplication.config.getProperty("grails.serverURL",String.class,'')
        // Require referer header to be from HTTPS version of server URL, otherwise allow HTTP. Default: true.
        def requireHttps = configurationService.getBoolean('security.csrf.referer.requireHttps', true)

        def quoteUrl = Pattern.quote(urlString)
        if (!requireHttps) {
            quoteUrl = urlString.replaceFirst("(?i)^https://(.*)\$") {
                "https?://" + Pattern.quote(it[1])
            }
        }

        def validRefererPrefix = "(?i)^" + quoteUrl
        def referer = request.getHeader('Referer')

        def isvalidReferer = referer && referer =~ validRefererPrefix
        if (csrf == 'POST') {
            if (request.method.toUpperCase() == "POST") {
                // referer must match serverURL, optionally https

                if (!isvalidReferer) {
                    def refid = "REFID:${UUID.randomUUID()}"
                    log.error("${request.method}: reject referer: $referer for $request.forwardURI [$refid]")
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    request.titleCode = 'request.error.unauthorized.title'
                    request.errorCode = 'request.error.invalidrequest.message'
                    request.errorArgs = [refid]
                    render(view: '/common/error')
                }
                return isvalidReferer
            }
        } else if (csrf == '*') {
            if (!isvalidReferer) {
                def refid = "REFID:${UUID.randomUUID()}"
                log.error("${request.method}: reject referer: $referer for $request.forwardURI [$refid]")
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                request.titleCode = 'request.error.unauthorized.title'
                request.errorCode = 'request.error.invalidrequest.message'
                request.errorArgs = [refid]
                render(view: '/common/error')
            }
            return isvalidReferer
        }

        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
