package org.rundeck.app.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Possibly handle exeptions within a Servlet request context
 */
public interface WebExceptionHandler {
    /**
     * If possible, handle the throwable with appropriate http response
     *
     * @param request  request
     * @param response response
     * @param thrown   throwable
     * @return true if response was sent for the exception
     */
    boolean handleException(
            HttpServletRequest request,
            HttpServletResponse response,
            Throwable thrown
    );
}
