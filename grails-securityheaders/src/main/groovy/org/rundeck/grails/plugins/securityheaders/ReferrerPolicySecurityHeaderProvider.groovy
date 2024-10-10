package org.rundeck.grails.plugins.securityheaders

import org.springframework.http.HttpStatus
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ReferrerPolicySecurityHeaderProvider implements SecurityHeaderProvider {

    static final String DEFAULT_REFERRER_POLICY = "Referrer-Policy"
    static final String DEFAULT_REFERRER_POLICY_VALUE = "strict-origin-when-cross-origin"

    String name = 'Referrer-Policy'
    Boolean defaultEnabled = true

    @Override
    List<SecurityHeader> getSecurityHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Map config
    ) {

        return [
                new SecurityHeaderImpl(name: DEFAULT_REFERRER_POLICY, value: DEFAULT_REFERRER_POLICY_VALUE),
        ]
    }
}