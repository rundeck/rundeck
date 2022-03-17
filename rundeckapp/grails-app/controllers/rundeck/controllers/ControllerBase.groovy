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

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import groovy.transform.CompileStatic
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.authorization.domain.AppAuthorizer
import org.rundeck.app.authorization.domain.job.AuthorizingJob
import org.rundeck.core.auth.app.type.AuthorizingAppType
import org.rundeck.core.auth.app.type.AuthorizingProjectType
import org.rundeck.core.auth.web.WebParamsIdResolver
import org.rundeck.app.authorization.domain.execution.AuthorizingExecution
import org.rundeck.app.auth.types.AuthorizingProject
import org.rundeck.core.auth.app.type.AuthorizingProjectAdhoc
import org.rundeck.app.web.WebExceptionHandler
import org.rundeck.core.auth.access.MissingParameter
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.rundeck.core.auth.app.type.AuthorizingSystem
import org.rundeck.core.auth.web.WebDefaultParameterNamesMapper
import rundeck.services.ApiService
import rundeck.services.UiPluginService

import javax.security.auth.Subject
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
    AppAuthContextProcessor rundeckAuthContextProcessor
    AppAuthorizer rundeckAppAuthorizer
    WebExceptionHandler rundeckExceptionHandler
    WebDefaultParameterNamesMapper rundeckWebDefaultParameterNamesMapper
    def grailsApplication

    protected UserAndRolesAuthContext getSystemAuthContext(){
        rundeckAuthContextProcessor.getAuthContextForSubject(getSubject())
    }

    protected UserAndRolesAuthContext getProjectAuthContext(){
        requireParams('project')
        rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(getSubject(),params.project.toString())
    }

    private WebParamsIdResolver createParamsIdResolver() {
        new WebParamsIdResolver(rundeckWebDefaultParameterNamesMapper.webDefaultParameterNames, params)
    }
    /**
     *
     * @return authorized access to project, requires request parameter 'project'
     */
    protected AuthorizingProject getAuthorizingProject() {
        requireParams('project')
        rundeckAppAuthorizer.project(subject, createParamsIdResolver())
    }

    /**
     *
     * @return authorized access to project, requires request parameter 'project'
     */
    protected AuthorizingProject authorizingProject(String project) {
        rundeckAppAuthorizer.project(subject, project)
    }

    /**
     *
     * @return authorized access to project adhoc resource, requires request parameter 'project'
     */
    protected AuthorizingProjectAdhoc getAuthorizingProjectAdhoc() {
        requireParams('project')
        rundeckAppAuthorizer.adhoc(subject, createParamsIdResolver())
    }
    /**
     *
     * @return authorized access to execution, requires request parameter 'id'
     */
    protected AuthorizingExecution getAuthorizingExecution() {
        requireParams('id')
        rundeckAppAuthorizer.execution(subject, createParamsIdResolver())
    }
    /**
     *
     * @return authorized access to job, requires request parameter 'id'
     */
    protected AuthorizingJob getAuthorizingJob() {
        requireParams('id')
        rundeckAppAuthorizer.job(subject, createParamsIdResolver())
    }

    /**
     * @return authorized job, requires request parameter 'id'
     * @param project project name
     * @param id job UUID
     */
    protected AuthorizingJob getAuthorizingJob(String project, String id) {
        rundeckAppAuthorizer.job(subject, project, id)
    }

    /**
     *
     * @return authorized access to system
     */
    protected AuthorizingSystem getAuthorizingSystem() {
        rundeckAppAuthorizer.system(subject)
    }

    /**
     * @return authorizing application type, requires type name
     * @param type type name
     */
    protected AuthorizingAppType getAuthorizingApplicationType(String type) {
        rundeckAppAuthorizer.applicationType(subject, type)
    }
    /**
     * @return authorizing application type, requires type name
     * @param type type name
     */
    protected AuthorizingProjectType getAuthorizingProjectType(String project, String type) {
        rundeckAppAuthorizer.projectType(subject,project, type)
    }

    protected Subject getSubject(){
        if(session.subject instanceof Subject){
            return session.subject
        }
        throw new IllegalStateException("no subject found in session")
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
        rundeckExceptionHandler.handleException(request, response, access)
    }
    /**
     * Handle unauthorized exception
     * @param notFound exception
     */
    def handleNotFound(NotFound notFound){
        rundeckExceptionHandler.handleException(request, response, notFound)
    }
    /**
     * Handle unauthorized exception
     * @param notFound exception
     */
    def handleMissingParameter(MissingParameter notFound){
        rundeckExceptionHandler.handleException(request, response, notFound)
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
            throw new MissingParameter([name])
        }
        return false
    }
    /**
     * Require the params to contain an entry
     * @param name name of parameter
     * @return true if response was sent
     */
    protected boolean requireParams(String... names) {
        requireParams(names.toList())
    }
    /**
     * Require the params to contain an entry
     * @param name name of parameter
     * @return true if response was sent
     */
    protected boolean requireParams(List<String> names) {
        def missing=names.findAll{!params[it]}
        if (missing) {
            throw new MissingParameter(missing)
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
