import javax.security.auth.Subject;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.core.common.Framework;

/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* AuthorizationFilters.groovy
*
* User: greg
* Created: Feb 2, 2010 11:08:13 AM
* $Id$
*/

public class AuthorizationFilters {
    def userService
    def FrameworkService frameworkService

    def filters = {
        /**
         * Set the session.user to logged in user only when not performing user login/logout 
         */
        loginCheck(controller: 'user', action: '(logout|login|error)', invert: true) {
            before = {
                if (request.remoteUser) {
                    session.user = request.remoteUser
                    
                    def principal = request.userPrincipal
                    def subject = new Subject();
                    subject.principals << new Username(principal.name)
                    if(principal.roles instanceof Iterator){
                        def Iterator iter= principal.roles
                        while(iter.hasNext()){
                            def role=iter.next()
                            if(role.rolename){
                                subject.principals << new Group(role.rolename)
                            }else if(role instanceof String){
                                subject.principals << new Group(role)
                            }

                        }
                    } else if (principal.roles instanceof Collection || principal.roles instanceof Object[]){
                        principal.roles?.each { name ->
                            subject.principals << new Group(name);
                        }
                    }else{
                        principal.roles?.members.each { group ->
                            subject.principals << new Group(group.name);
                        }
                    }
                    
                    request.subject = subject
                }else if (request.api_version) {
                    //allow authentication token to be used 
                    def authtoken = params.authtoken? params.authtoken : request.getHeader('X-RunDeck-Auth-Token')
                    def tokenobj=authtoken?AuthToken.findByToken(authtoken):null
                    User user = tokenobj?.user
                    if (tokenobj && user){
                        session.user = user.login
                        request.authenticatedToken=authtoken
                        log.debug("loginCheck found user ${user} via token: ${authtoken}");
                        def subject = new Subject();
                        subject.principals << new Username(user.login)

                        ['api_token_group'].each{role->
                            subject.principals << new Group(role.trim());
                        }

                        request.subject = subject
                    }else{
                        request.subject=null
                        session.user=null
                        if(authtoken){
                            request.invalidAuthToken = "Token:" + authtoken.substring(0, 5) + "****"
                        }
                        request.authenticatedToken = null
                        request.invalidApiAuthentication = true
                        if(authtoken){
                            log.error("Invalid API token used: ${authtoken}");
                        }else{
                            log.error("Unauthenticated API request");
                        }
                    }
                }
            }
        }
        /**
         * Check the user has authorization for the actions.
         */
        authorizationCheck(controller: '*', action: '*') {
            before = {
               
                if (request.invalidApiAuthentication ) {
                    response.setStatus(403)
                    def authid = session.user ?: "(${request.invalidAuthToken ?: 'unauthenticated'})"
                    log.error("${authid} UNAUTHORIZED for ${controllerName}/${actionName}");
                    if (request.api_version) {
                        //api request
                        render(contentType: "text/xml", encoding: "UTF-8") {
                            result(error: "true", apiversion: ApiRequestFilters.API_CURRENT_VERSION) {
                                delegate.'error'(code: "unauthorized") {
                                    message("${authid} is not authorized for: ${request.forwardURI}")
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
                }
            }
        }
    }
}