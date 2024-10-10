package org.rundeck.grails.plugins.securityheaders

import org.springframework.http.HttpStatus
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class PermissionsPolicySecurityHeaderProvider implements SecurityHeaderProvider {

    static final String DEFAULT_PERMISSIONS_POLICY = "Permissions-Policy"
    static final String DEFAULT_PERMISSIONS_POLICY_VALUE = "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()"

    String name = 'Permissions-Policy'
    Boolean defaultEnabled = true

    @Override
    List<SecurityHeader> getSecurityHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Map config
    ) {

        return [
                new SecurityHeaderImpl(name: DEFAULT_PERMISSIONS_POLICY, value: DEFAULT_PERMISSIONS_POLICY_VALUE),
        ]
    }
}
