package org.rundeck.grails.plugins.securityheaders

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * Insert cache-control headers into the response to prevent caching if no
 * headers have been specified.
 *
 * <pre>
 * Cache-Control: no-cache, no-store, max-age=0, must-revalidate
 * Pragma: no-cache
 * Expires: 0
 * </pre>
 *
 */
class CacheControlSecurityHeaderProvider implements SecurityHeaderProvider {

    static final String DEFAULT_EXPIRES_VALUE = "0"
    static final String DEFAULT_PRAGMA_VALUE = "no-cache"
    static final String DEFAULT_CACHE_CONTROL_VALUE = "no-cache, no-store, max-age=0, must-revalidate"

    String name = 'cache-control'
    Boolean defaultEnabled = true

    @Override
    List<SecurityHeader> getSecurityHeaders(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Map config
    ) {

        // Avoid replacing the headers if they are already set
        if (response.getHeader(HttpHeaders.CACHE_CONTROL)
            || response.getHeader(HttpHeaders.EXPIRES)
            || response.getHeader(HttpHeaders.PRAGMA)
            || response.getStatus() == HttpStatus.NOT_MODIFIED.value()) {
            return []
        }

        return [
            new SecurityHeaderImpl(name: HttpHeaders.CACHE_CONTROL, value: DEFAULT_CACHE_CONTROL_VALUE),
            new SecurityHeaderImpl(name: HttpHeaders.PRAGMA, value: DEFAULT_PRAGMA_VALUE),
            new SecurityHeaderImpl(name: HttpHeaders.EXPIRES, value: DEFAULT_EXPIRES_VALUE)
        ]
    }
}
