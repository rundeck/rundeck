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

    private static final String DEFAULT_EXPIRES_VALUE = "0"
    private static final String DEFAULT_PRAGMA_VALUE = "no-cache"
    private static final String DEFAULT_CACHE_CONTROL_VALUE = "no-cache, no-store, max-age=0, must-revalidate"

    String name = 'cache-control'
    Boolean defaultEnabled = true

    @Override
    List<SecurityHeader> getSecurityHeaders(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Map config
    ) {

        def stat = response.getStatus()

        if (response.getHeader(HttpHeaders.CACHE_CONTROL)
            || response.getHeader(HttpHeaders.EXPIRES)
            || response.getHeader(HttpHeaders.PRAGMA)
            || response.getStatus() == HttpStatus.NOT_MODIFIED.value()) {
            return null
        }

        String value = config.get('value') ?: DEFAULT_CACHE_CONTROL_VALUE
        String pragma = config.get('pragma') ?: DEFAULT_PRAGMA_VALUE
        String expires = config.get('expires') ?: DEFAULT_EXPIRES_VALUE

        return [
            header(HttpHeaders.CACHE_CONTROL, value),
            header(HttpHeaders.PRAGMA, pragma),
            header(HttpHeaders.EXPIRES, expires)
        ]
    }
}
