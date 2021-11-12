package org.rundeck.web

import grails.artefact.controller.support.ResponseRenderer
import groovy.transform.CompileStatic
import org.rundeck.app.web.WebExceptionHandler
import org.rundeck.core.auth.access.MissingParameter
import org.rundeck.app.web.WebUtilService
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.springframework.beans.factory.annotation.Autowired

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@CompileStatic
class ExceptionHandler implements WebExceptionHandler {
    @Autowired
    WebUtilService rundeckWebUtil

    @Override
    boolean handleException(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Throwable thrown
    ) {
        if (thrown instanceof UnauthorizedAccess) {
            handleUnauthorized(request, response, thrown)
        } else if (thrown instanceof NotFound) {
            handleNotFound(request, response, thrown)
        } else if (thrown instanceof MissingParameter) {
            handleMissingParameter(request, response, thrown)
        } else {
            return false
        }
        return true
    }


    void handleUnauthorized(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final UnauthorizedAccess access
    ) {
        request.setAttribute('titleCode', 'request.error.unauthorized.title')
        rundeckWebUtil.respondError(
            request,
            response,
            'api.error.item.unauthorized',
            HttpServletResponse.SC_FORBIDDEN,
            [access.action, access.type, access.name]
        )
    }

    void handleNotFound(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final NotFound notFound
    ) {
        request.setAttribute('titleCode', 'request.error.notfound.title')
        rundeckWebUtil.respondError(
            request,
            response,
            'api.error.item.doesnotexist',
            HttpServletResponse.SC_NOT_FOUND,
            [notFound.type, notFound.name]
        )
    }

    void handleMissingParameter(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final MissingParameter missingParameter
    ) {
        request.setAttribute('titleCode', 'request.error.title')
        rundeckWebUtil.respondError(
            request,
            response,
            'api.error.parameter.required',
            HttpServletResponse.SC_BAD_REQUEST,
            [missingParameter.parameters.join(',')]
        )
    }
}
