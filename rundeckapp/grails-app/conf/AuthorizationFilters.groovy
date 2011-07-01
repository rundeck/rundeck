import javax.security.auth.Subject;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.client.utils.Constants;

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
    def roleService

    /*
        Define static authorization lists to use with authSet definitions
     */
    /* Workflow authorization requirement definitions */
    private static _wf_create = UserAuth.WF_CREATE
    private static _wf_read = UserAuth.WF_READ
    private static _wf_update = UserAuth.WF_UPDATE
    private static _wf_delete = UserAuth.WF_DELETE
    private static _wf_run = UserAuth.WF_RUN
    private static _wf_kill = UserAuth.WF_KILL
    private static _wf_read_update = [UserAuth.WF_READ, UserAuth.WF_UPDATE]
    private static _wf_create_update = [UserAuth.WF_CREATE, UserAuth.WF_UPDATE]
    private static _wf_create_run = [UserAuth.WF_CREATE, UserAuth.WF_RUN]

    /* Events authorization requirement definitions */
    private static _ev_create = UserAuth.EV_CREATE
    private static _ev_read = UserAuth.EV_READ
    private static _ev_update = UserAuth.EV_UPDATE
    private static _ev_delete = UserAuth.EV_DELETE

    /* Resource authorization requirement definitions */
    private static _rs_create = UserAuth.RS_CREATE
    private static _rs_delete = UserAuth.RS_DELETE
    private static _rs_read = UserAuth.RS_READ
    private static _rs_update = UserAuth.RS_UPDATE

    /**
     * Authorization definitions.  This is a map of controller->[action->[required authorization list]]
     * if the user does not have all of authorization flags defined in the list, then the action is denied
     */
    def authSet = [
        /*
            Workflow authorization actions for ScheduledExecutionController and ExecutionController
         */
        'scheduledExecution': [
            //create
            'create': _wf_create,
            'createFromExecution': [_wf_create,_wf_read],
            'copy': _wf_create,
            'save': _wf_create,
            '_dosave': _wf_create,
            //read
            'show': _wf_read,
            'apiJobExport': _wf_read,
            //update
            'update': _wf_update,
            '_doupdate': _wf_update,
            'edit': _wf_read_update,
            'renderEditFragment': _wf_read_update,
            //delete
            'delete': _wf_delete,
            'deleteBulk': _wf_delete,
            'apiJobDelete': _wf_delete,

            //run
            'execute': _wf_run,
            'executeInline': _wf_run,
            'runJobNow': _wf_run,
            'executeNow': _wf_run,
            'apiJobRun': _wf_run,
            'apiRunCommand': _wf_run,
            'apiRunScript': _wf_run,

            //combinations//
            //create+update
            'upload': _wf_create_update,
            'apiJobsImport': _wf_create_update,

            //create+run//
            'uploadAndExec': _wf_create_run,
            'saveAndExec': _wf_create_run,
            'execAndForget': _wf_create_run,
        ],
        'execution': [
            //read
            'follow': _wf_read,
            'show': _wf_read,
            'apiExecution': _wf_read,
            'downloadOutput': _wf_read,
            'tailExecutionOutput': _wf_read,

            //kill
            'cancelExecution': _wf_kill,
            'apiExecutionAbort': _wf_kill,
        ],

        /*
            MenuController authorizations
         */
        'menu': [
            //read
            'workflows': _wf_read,
            'workflowsFragment': _wf_read,
            'jobs': _wf_read,
            'jobsFragment': _wf_read,
            'nowrunning': _wf_read,
            'nowrunningFragment': _wf_read,
            'nowrunningData': _wf_read,
            'apiExecutionsRunning': _wf_read,
            'queueFragment': _wf_read,
            'apiJobsList': _wf_read,
            'apiJobsExport': _wf_read,
        ],
        /*
            ReportsController authorizations
         */
        'reports': [
            //read
            'index': _ev_read,
            'eventsFragment': _ev_read,
            'timelineFragment': _ev_read,
            'commands': _ev_read,
            'jobs': _ev_read,
            'query': _ev_read,
            'apiHistory': _ev_read,
        ],
        /*
            FrameworkController authorizations
         */
        'framework': [
            //read
//            '*': _rs_read,
            'nodes': _rs_read,
            'nodesData': _rs_read,
            'nodesFragment': _rs_read,
            'listFrameworkResourceInstances': _rs_read,
            'apiProjects': _rs_read,
            'apiProject': _rs_read,
            'apiResource': _rs_read,
            'apiResources': _rs_read,
            'apiProjectResources': [_rs_read, _rs_create, _rs_update],
            'apiProjectResourcesRefresh': [_rs_read, _rs_create, _rs_update],
        ],
        /*
            ReportsController authorizations
         */
        'feed': [
            //read
//            '*': _ev_read,
        ],
    ]

    def filters = {
        /**
         * Set the session.user to logged in user only when not performing user login/logout 
         */
        loginCheck(controller: 'user', action: '(logout|login|error|denied|deniedFragment)', invert: true) {
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

                    //check user role membership for verification
                    def foundroles=roleService.listMappedRoleMembership(request)
                    if(!foundroles){
                        log.warn("User ${session.user} has no role membership in mapped roles");
                    }
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
                def authc = authSet[controllerName]
                def authReq
                if (authc) {
                    authReq = authc[actionName]
                }
                if (!authReq && authc) {
                    //look for controller/* auth requirement
                    authReq = authc['*']
                }
                def roletest
                if (authReq) {
                    if (authReq instanceof String) {
                        authReq = [authReq]
                    }
                    //get user authorizations
                    def User user
                    if(session && session.user){
                        def admintest = roleService.isUserInRole(request, 'admin') || request.subject?.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class)?.find{it.name=='admin'}
                        roletest = admintest || roleService.isUserInAllRoles(request, authReq)
                    }
                }

                if (request.invalidApiAuthentication || (authReq && !roletest)) {
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