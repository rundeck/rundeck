package org.rundeck.web

import groovy.transform.CompileStatic
import org.rundeck.app.web.RequestIdProvider

import javax.servlet.http.HttpServletRequest

/**
 * Provides a default RequestId when no other provider has been wired up to do so.
 */
@CompileStatic
class DefaultRequestIdProvider implements RequestIdProvider {
    @Override
    String getRequestId(HttpServletRequest request) {
        return UUID.randomUUID().toString()
    }
}
