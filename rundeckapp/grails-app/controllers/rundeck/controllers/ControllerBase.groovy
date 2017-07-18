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

import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import org.rundeck.util.Toposort
import org.rundeck.web.infosec.HMacSynchronizerTokensHolder
import org.codehaus.groovy.grails.web.metaclass.InvalidResponseHandler
import org.codehaus.groovy.grails.web.metaclass.ValidResponseHandler
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.servlet.mvc.TokenResponseHandler
import org.springframework.web.context.request.RequestContextHolder
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
    public static final ArrayList<String> UIPLUGIN_PAGES = [
            'menu/jobs',
            'menu/home',
            'menu/projectHome',
            'menu/executionMode',
            'menu/projectExport',
            'menu/projectImport',
            'menu/projectDelete',
            'menu/projectAcls',
            "menu/logStorage",
            "menu/securityConfig",
            "menu/acls",
            "menu/systemInfo",
            "menu/systemConfig",
            "menu/metrics",
            "menu/plugins",
            "menu/welcome",
            "menu/storage",
            "scheduledExecution/show",
            "scheduledExecution/edit",
            "scheduledExecution/delete",
            "scheduledExecution/create",
            "execution/show",
            "framework/nodes",
            "framework/adhoc",
            "framework/createProject",
            "framework/editProject",
            "framework/editProjectConfig",
            "framework/editProjectFile",
            "scm/index",
            "reports/index",
    ]
    def grailsApplication
    UiPluginService uiPluginService

    protected def loadUiPlugins(path) {
        def uiplugins = [:]
        if ((path in UIPLUGIN_PAGES)) {
            def page = uiPluginService.pluginsForPage(path)
            page.each { name, inst ->
                def requires = inst.requires(path)

                uiplugins[name] = [
                        scripts : inst.scriptResourcesForPath(path),
                        styles  : inst.styleResourcesForPath(path),
                        requires: requires,
                ]
            }
        }
        uiplugins
    }

    protected def sortUiPlugins(Map uiplugins) {
        Map inbound = [:]
        Map outbound = [:]

        uiplugins.each { name, inst ->
            inbound[name] = inst.requires ?: []
            inbound[name].each { k ->
                if (!outbound[k]) {
                    outbound[k] = [name]
                } else {
                    outbound[k] << name
                }
            }
        }
        List sort = uiplugins.keySet().sort()
        if (outbound.size() > 0 || inbound.size() > 0) {
            def result = Toposort.toposort(sort, outbound, inbound)
            if (!result.cycle) {
                return result.result
            }
        }
        sort
    }

    def afterInterceptor = { model ->
        model.uiplugins = loadUiPlugins(controllerName + "/" + actionName)
        model.uipluginsorder = sortUiPlugins(model.uiplugins)
        model.uipluginsPath = controllerName + "/" + actionName
    }
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
        if(grailsApplication.config.rundeck?.ajax?.compression=='gzip'
                && request.getHeader("Accept-Encoding").contains("gzip")){
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
}
