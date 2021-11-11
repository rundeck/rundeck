/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.controllers

import groovy.transform.CompileStatic
import org.grails.plugins.web.servlet.mvc.InvalidResponseHandler
import org.grails.plugins.web.servlet.mvc.ValidResponseHandler
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.servlet.mvc.TokenResponseHandler
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.authorization.UnauthorizedAccess
import org.rundeck.web.infosec.HMacSynchronizerTokensHolder
import org.springframework.web.context.request.RequestContextHolder
import rundeck.services.ApiService
import rundeck.services.UiPluginService

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.zip.GZIPOutputStream

/**
 * Mixin utility for controllers
 * @author greg
 * @since 2014-03-12
 */
class ControllerBase {
    UiPluginService uiPluginService
    ApiService apiService
    AppAuthContextProcessor rundeckWebAuthContextProcessor
    def grailsApplication

    protected def withHmacToken(Closure valid){
        GrailsWebRequest request= (GrailsWebRequest) RequestContextHolder.currentRequestAttributes()
        TokenResponseHandler handler
        if(isTokenValid(request)){
            resetToken(request)
            handler = new ValidResponseHandler(valid?.call())
        } else {
            handler = new InvalidResponseHandler()
        }

        request.request.setAttribute(TokenResponseHandler.KEY, handler)
        return handler
    }

    def resetToken(GrailsWebRequest request) {
        HMacSynchronizerTokensHolder holder = request.currentRequest.getSession(false)?.getAttribute(HMacSynchronizerTokensHolder.HOLDER)
        String tokenInRequest = request.params[HMacSynchronizerTokensHolder.TOKEN_KEY]
        if (!tokenInRequest) return

        holder.resetToken(tokenInRequest)
    }

    boolean isTokenValid(GrailsWebRequest request) {
        HMacSynchronizerTokensHolder holder = request.currentRequest.getSession(false)?.getAttribute(HMacSynchronizerTokensHolder.HOLDER)
        if (!holder) return false

        String tokenInRequest = request.params[HMacSynchronizerTokensHolder.TOKEN_KEY]
        if (!tokenInRequest) return false

        String timestampInRequest = request.params[HMacSynchronizerTokensHolder.TOKEN_TIMESTAMP]
        if (!timestampInRequest) return false

        long timestamp=0
        try{
            timestamp=Long.parseLong(timestampInRequest)
        }catch (NumberFormatException e){
            return false
        }

        try {
            return holder.isValid(timestamp, tokenInRequest)
        }
        catch (IllegalArgumentException) {
            return false
        }
    }
    def renderCompressed(HttpServletRequest request,HttpServletResponse response,String contentType, data){
        String compression = grailsApplication.config.getProperty("rundeck.ajax.compression", String.class)
        if(compression=='gzip' && request.getHeader("Accept-Encoding")?.contains("gzip")){
            response.setHeader("Content-Encoding","gzip")
            response.setHeader("Content-Type",contentType)
            def stream = new GZIPOutputStream(response.outputStream)
            stream.withWriter("UTF-8"){ it << data }
            stream.close()
        }else{
            return render(contentType:contentType,text:data)
        }
    }
/**
     * Send a Not Found response unless the test passes, return true if the response was sent.
     * @param test exists test
     * @param type object type
     * @param name object name
     * @param fragment if true, render only the error message content, otherwise render a view
     * @return true if response was committed
     */
    protected def notFoundResponse(Object test, String type, String name, boolean fragment = false) {
        if (!test) {
            renderNotfound(type, name, fragment)
        }
        return !test
    }

    /**
     * Send a Not Found response
     * @param type object type
     * @param name object name
     * @param fragment if true, render only the error message content, otherwise render a view
     * @return true if response was committed
     */
    protected void renderNotfound(String type, String name, boolean fragment = false) {
        request.errorCode = 'request.error.notfound.message'
        request.errorArgs = [type, name]
        response.status = HttpServletResponse.SC_NOT_FOUND
        request.titleCode = 'request.error.notfound.title'
        if (fragment) {
            renderErrorFragment([:])
        } else {
            renderErrorView([:])
        }
    }
    /**
     * Send an Unauthoried error response unless the test passes, return true if the response was sent.
     * @param test authorization test
     * @param action authorization action
     * @param type object type
     * @param name object name
     * @param fragment if true, render only the error message content, otherwise render a view
     * @return true if response was committed
     */
    protected def unauthorizedResponse(Object test, String action, String type, Object name = '',
                                       boolean fragment = false) {
        if (!test) {
            renderUnauthorized(action, type, name, fragment)
        }
        return !test
    }

    /**
     * Handle unauthorized exception
     * @param access exception
     */
    def handleUnauthorized(UnauthorizedAccess access){
        if(request.api_version){
            apiService.renderErrorFormat(response, [
                status: HttpServletResponse.SC_FORBIDDEN,
                code: 'api.error.item.unauthorized',
                args: [access.action, access.type, access.name]
            ])
        }else{
            renderUnauthorized(access.action, access.type, access.name)
        }
    }

    /**
     * Send an Unauthoried error response
     * @param action authorization action
     * @param type object type
     * @param name object name
     * @param fragment if true, render only the error message content, otherwise render a view
     */
    protected void renderUnauthorized(String action, String type, Object name, boolean fragment = false) {
        request.errorCode = 'request.error.unauthorized.message'
        request.errorArgs = [action, type, name]
        response.status = HttpServletResponse.SC_FORBIDDEN
        request.titleCode = 'request.error.unauthorized.title'
        if (fragment) {
            response.addHeader("X-Rundeck-Error-Message", g.message(code:request.errorCode,args:request.errorArgs))
            renderErrorFragment([:])
        } else {
            renderErrorView([:])
        }
    }
    /**
     * Send an Unauthoried error response
     * @param message message text
     * @param fragment if true, render only the error message content, otherwise render a view
     */
    protected void renderUnauthorized(String message, boolean fragment = false) {
        request.errorMessage = message
        response.status = HttpServletResponse.SC_FORBIDDEN
        request.titleCode = 'request.error.unauthorized.title'
        if (fragment) {
            response.addHeader("X-Rundeck-Error-Message", message)
            renderErrorFragment([:])
        } else {
            renderErrorView([:])
        }
    }

    /**
     * Send an error response view
     * @param message message text
     */
    protected def renderErrorView(String message) {
        request.errorMessage = message
        render(view: "/common/error")
    }
    /**
     * Send an error response
     * @param model error data
     */
    protected def renderErrorView(Map model) {
        render(view: "/common/error", model: model)
    }
    /**
     * Send an error response fragment
     * @param message message text
     */
    protected def renderErrorFragment(String message) {
        request.errorMessage = message
        render(template: "/common/errorFragment")
    }
    /**
     * Send an error response fragment
     * @param model data model
     */
    protected def renderErrorFragment(Map model) {
        render(template: "/common/errorFragment",model:model)
    }

    /**
     * Test for valid request token
     * @return true if token is valid, false if error response has been sent
     */
    protected boolean requestHasValidToken() {
        boolean valid = false
        withForm {
            valid = true
        }.invalidToken {
        }
        if (!valid) {
            request.errorCode = 'request.error.invalidtoken.message'
            renderErrorView([:])
        }
        valid
    }

    /**
     * Require the request to contain x-rundeck-ajax:true header, otherwise
     * redirect with the given params
     * @param params redirect params
     * @return true if redirected
     */
    protected boolean requireAjax(Map params) {
        boolean invalid = 'true' != request.getHeader('x-rundeck-ajax')
        if (invalid) {
            redirect(params)
        }
        invalid
    }
    /**
     * Require the params to contain an entry
     * @param name name of parameter
     * @return true if response was sent
     */
    protected boolean requireParam(String name) {
        if (!params[name]) {
            renderErrorView("parameter $name is required")
            return true
        }
        return false
    }
    /**
     * Require the params to contain an entry
     * @param name name of parameter
     * @return true if response was sent
     */
    protected boolean requireParams(List<String> names) {
        def missing=names.findAll{!params[it]}
        if (missing) {
            renderErrorView("parameters required: $missing")
            return true
        }
        return false
    }

    /** Flush response in a static compiled method to avoid Tomcat 7 introspection errors */
    @CompileStatic
    static void flush(HttpServletResponse response) {
        response.outputStream.flush()
    }

    /** Append to response output in a static compiled method to avoid Tomcat 7 introspection errors */
    @CompileStatic
    static void appendOutput(HttpServletResponse response, String output) {
        response.outputStream << output
    }
}
