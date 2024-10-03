package org.rundeck.grails.plugins.securityheaders

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class StrictTransportSecurityHeaderProvider implements SecurityHeaderProvider {

    static final String DEFAULT_STRICT_TRANSPORT_VALUE = "max-age=31536000; includeSubDomains"

    String name = 'strict-transport-security'
    Boolean defaultEnabled = true

    @Override
    List<SecurityHeader> getSecurityHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Map config
    ) {

        // Avoid replacing the headers if they are already set
        if (response.getHeader("Strict-Transport-Security") || response.getStatus() == HttpStatus.NOT_MODIFIED.value()) {
            return []
        }

        return [
                new SecurityHeaderImpl(name: "Strict-Transport-Security", value: DEFAULT_STRICT_TRANSPORT_VALUE),
        ]
    }
}
