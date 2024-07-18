package org.rundeck.web

import javax.servlet.http.HttpServletRequest

interface RequestIdProvider {
    String HTTP_ATTRIBUTE_NAME = "requestId"

    String getRequestId(HttpServletRequest request)
}
