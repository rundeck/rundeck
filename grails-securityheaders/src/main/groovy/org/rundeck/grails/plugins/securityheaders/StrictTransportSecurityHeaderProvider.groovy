package org.rundeck.grails.plugins.securityheaders

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

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

        return [
                new SecurityHeaderImpl(name: "Strict-Transport-Security", value: DEFAULT_STRICT_TRANSPORT_VALUE),
        ]
    }
}
