package org.rundeck.app.web;

import javax.servlet.http.HttpServletRequest;

public interface RequestIdProvider {
    String HTTP_ATTRIBUTE_NAME = "requestId";
    
    String getRequestId(HttpServletRequest request);
}
