package org.rundeck.web

import javax.servlet.http.HttpServletRequest

/**
 * Provides a default RequestId when no other provider has been wired up to do so.
 */
class DefaultRequestIdProvider implements RequestIdProvider {
    @Override
    String getRequestId(HttpServletRequest request) {
        return UUID.randomUUID().toString()
    }
}
