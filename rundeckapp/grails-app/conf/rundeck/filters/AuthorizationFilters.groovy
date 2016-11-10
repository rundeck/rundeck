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

package rundeck.filters

import org.rundeck.web.infosec.AuthorizationRoleSource
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import javax.security.auth.Subject;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.client.utils.Constants
import rundeck.User
import rundeck.AuthToken

import rundeck.services.FrameworkService

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest

/*
* AuthorizationFilters.groovy
*
* User: greg
* Created: Feb 2, 2010 11:08:13 AM
* $Id$
*/

public class AuthorizationFilters implements ApplicationContextAware{
    def FrameworkService frameworkService
    def ApplicationContext applicationContext
    def dependsOn = [ApiRequestFilters]

    def filters = {
        /**
         * Set the session.user to logged in user only when not performing user login/logout 
         */
        loginCheck(controller: 'user', action: '(logout|login|error|loggedout)', invert: true) {
            before = {
                if (request.api_version && request.remoteUser && !(grailsApplication.config.rundeck?.security?.apiCookieAccess?.enabled in ['true',true])){
                    //disallow api access via normal login
                    request.invalidApiAuthentication=true
                    return
                }
                if (request.remoteUser && session.user!=request.remoteUser) {
                    session.user = request.remoteUser

                    Subject subject=createAuthSubject(request)
                    
                    request.subject = subject
                    session.subject = subject
                } else if(request.remoteUser && session.subject && grailsApplication.config.rundeck.security.authorization.preauthenticated.enabled in ['true',true]){
                    // Preauthenticated mode is enabled, handle upstream role changes
                    Subject subject = createAuthSubject(request)
                    request.subject = subject
                    session.subject = subject
                } else if(request.remoteUser && session.subject && grailsApplication.config.rundeck.security.authorization.preauthenticated.enabled in ['false',false]) {
                    request.subject = session.subject
                } else if (request.api_version && !session.user ) {
                    //allow authentication token to be used 
                    def authtoken = params.authtoken? params.authtoken : request.getHeader('X-RunDeck-Auth-Token')
                    String user = lookupToken(authtoken, servletContext)

                    if (user){
                        session.user = user
                        request.authenticatedToken=authtoken
                        request.authenticatedUser=user
                        def subject = new Subject();
                        subject.principals << new Username(user)

                        ['api_token_group'].each{role->
                            subject.principals << new Group(role.trim());
                        }

                        request.subject = subject
                        session.subject = subject
                    }else{
                        request.subject=null
                        session.subject=null
                        session.user=null
                        if(authtoken){
                            request.invalidAuthToken = "Token:" + (authtoken.size()>5?authtoken.substring(0, 5):'') + "****"
                        }
                        request.authenticatedToken = null
                        request.authenticatedUser = null
                        request.invalidApiAuthentication = true
                        if(authtoken){
                            log.error("Invalid API token used: ${authtoken}");
                        }else{
                            log.error("Unauthenticated API request");
                        }
                    }
                } else if (!request.remoteUser && controllerName && !(controllerName in ['assets','feed'])) {
                    //unauthenticated request to an action
                    response.status = 403
                    request.errorCode = 'request.authentication.required'
                    render(view: '/common/error.gsp')
                    return false
                }
            }
        }
        /**
         * Check the user has authorization for the actions.
         */
        postLoginAuthorizationCheck(controller: '*', action: '*') {
            before = {
                if (request.invalidApiAuthentication) {
                    response.setStatus(403)
                    def authid = session.user ?: "(${request.invalidAuthToken ?: 'unauthenticated'})"
                    log.error("${authid} UNAUTHORIZED for ${controllerName}/${actionName}");
                    if (request.api_version) {
                        //api request
                        if (response.format in ['json']) {
                            render(contentType: "application/json", encoding: "UTF-8") {
                                error = true
                                apiversion = ApiRequestFilters.API_CURRENT_VERSION
                                errorCode = "unauthorized"
                                message = ("${authid} is not authorized for: ${request.forwardURI}")
                            }
                        } else {
                            render(contentType: "text/xml", encoding: "UTF-8") {
                                result(error: "true", apiversion: ApiRequestFilters.API_CURRENT_VERSION) {
                                    delegate.'error'(code: "unauthorized") {
                                        message("${authid} is not authorized for: ${request.forwardURI}")
                                    }
                                }
                            }
                        }
                        return false
                    }
                    flash.title = "Unauthorized"
                    flash.error = "${authid} is not authorized"
                    response.setHeader(Constants.X_RUNDECK_ACTION_UNAUTHORIZED_HEADER, flash.error)
                    redirect(controller: 'user', action: actionName ==~ /^.*(Fragment|Inline)$/ ? 'deniedFragment' : 'denied', params: params.xmlreq ? params.subMap(['xmlreq']) : null)
                    return false;
                }
            }
        }
        postApiTokenCheck(controller:'user',action:'logout',invert:true){
            after={
                if(request?.authenticatedToken && session && session?.user){
                    session.user=null
                    request.subject=null
                    session.subject=null
                }
            }
        }
    }

    private Subject createAuthSubject(HttpServletRequest request) {
        def principal = request.userPrincipal
        def subject = new Subject();
        subject.principals << new Username(principal.name)

        //find AuthorizationRoleSource instances
        Map<String,AuthorizationRoleSource> type = applicationContext.getBeansOfType(AuthorizationRoleSource)
        def roleset = new HashSet<String>()
        type.each {name,AuthorizationRoleSource source->
            if(source.enabled) {
                def roles = source.getUserRoles(principal.name, request)
                if(roles){
                    roleset.addAll(roles)
                    log.debug("Accepting user role list from bean ${name} for ${principal.name}: ${roles}")
                }else{
                    log.debug("Empty role list from bean ${name} for ${principal.name}")
                }
            }else {
                log.debug("Role source not enabled, bean ${name}")
            }
        }
        subject.principals.addAll(roleset.collect{new Group(it)})

        subject
    }

    /**
     * Look up the given authToken and return the associated username, or null
     * @param authtoken
     * @param context
     * @return
     */
    private String lookupToken(String authtoken, ServletContext context) {
        if(!authtoken){
            return null
        }
        if (context.getAttribute("TOKENS_FILE_PROPS")) {
            Properties tokens = (Properties) context.getAttribute("TOKENS_FILE_PROPS")
            if (tokens[authtoken]) {
                def user = tokens[authtoken]
                log.debug("loginCheck found user ${user} via tokens file, token: ${authtoken}");
                return user
            }
        }
        def tokenobj = authtoken ? AuthToken.findByToken(authtoken) : null
        if (tokenobj) {
            User user = tokenobj?.user
            log.debug("loginCheck found user ${user} via DB, token: ${authtoken}");
            return user.login
        }
        null
    }
}
