package org.rundeck.app.web;

import jakarta.servlet.http.HttpServletRequest;

public interface RequestIdProvider {
    String HTTP_ATTRIBUTE_NAME = "requestId";
    
    String getRequestId(HttpServletRequest request);
}
