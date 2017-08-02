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

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.converters.JSON
import org.rundeck.util.Sizes
import rundeck.AuthToken
import rundeck.User
import rundeck.services.FrameworkService
import rundeck.services.UserService

import javax.servlet.http.HttpServletResponse

class UserController extends ControllerBase{

    UserService userService
    FrameworkService frameworkService
    def grailsApplication
    def apiService
    def configurationService

    static allowedMethods = [
            addFilterPref      : 'POST',
            store              : 'POST',
            update             : 'POST',
            clearApiToken      : 'POST',
            generateUserToken  : 'POST',
            renderUsertoken    : 'POST',
            removeExpiredTokens: 'POST',
    ]

    def index = {
        redirect(action:"login")
    }

    def error = {
        flash.loginerror=message(code:"invalid.username.and.password")
        return render(view:'login')
    }

    def logout = {
        session.invalidate()
        return redirect(action: 'loggedout')
    }

    def loggedout(){
        if(grailsApplication.config.rundeck.security.authorization.preauthenticated.redirectLogout in ['true',true]) {
            return redirect(url: grailsApplication.config.grails.serverURL + grailsApplication.config.rundeck.security.authorization.preauthenticated.redirectUrl)
        }
    }

    def login = {
        if (session.user) {
            redirect(controller:'menu', action:'index')
        }
    }

    def handleLogin = {
        // Simple pass threw for now!
        session.user = params.login
        redirect(controller:'menu', action:'index')
    }
    def denied={
        response.setStatus(403)
        renderErrorView('Access denied')
    }
    def deniedFragment={
        response.setStatus(403)
        renderErrorFragment('Access denied')
    }
    def list={
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if(unauthorizedResponse(frameworkService.authorizeApplicationResourceType(authContext, AuthConstants.TYPE_USER,
                AuthConstants.ACTION_ADMIN),
                AuthConstants.ACTION_ADMIN, 'User', 'accounts')) {
            return
        }
        [users:User.listOrderByLogin()]
    }

    def profile() {
        //check auth to view profile
        //default to current user profile
        if(!params.login){
            params.login=session.user
        }
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if(unauthorizedResponse(params.login == session.user || frameworkService.authorizeApplicationResourceType
                (authContext, AuthConstants.TYPE_USER, AuthConstants.ACTION_ADMIN), AuthConstants.ACTION_ADMIN,'Users',
                params.login)){
            return
        }
        def User u = User.findByLogin(params.login)
        if(!u && params.login==session.user){
            //redirect to profile edit page, so user can setup their profile
            flash.message="Please fill out your profile"
            return redirect(action:'register')
        }
        if(notFoundResponse(u, 'User', params['login'])){
            return
        }
        [
                user              : u,
                authRoles         : authContext.getRoles(),
                tokenMaxExpiration: apiService.maxTokenDurationConfig()
        ]
    }
    def create={
        render(view:'register',model:[user:new User(),newuser:true])
    }
    def register={
        if(!params.login){
            params.login=session.user
        }
        def User u = User.findByLogin(params.login)
        if(u){
           return redirect(action:'edit')
        }
        u = new User(login:params.login)
        u.dashboardPref="1,2,3,4"

        def model=[user: u,newRegistration:true]
        return model
    }
    public def store(User user){
        withForm{
        if(user.hasErrors()){
            flash.errors=user.errors
            return render(view: 'edit', model: [user: user,newuser:params.newuser])
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (unauthorizedResponse(user.login == session.user || frameworkService.authorizeApplicationResourceType
                (authContext, AuthConstants.TYPE_USER, AuthConstants.ACTION_ADMIN), AuthConstants.ACTION_ADMIN, 'Users',
                user.login)) {
            return
        }

        def User u = User.findByLogin(user.login)
        if(u){
            request.errorCode = 'api.error.item.alreadyexists'
            request.errorArgs = ['User profile', user.login]
            return renderErrorView([:])
        }
        u = new User(user.properties.subMap(['login','firstName','lastName','email']))

        if(!u.save(flush:true)){
            def errmsg= u.errors.allErrors.collect { g.message(error:it) }.join("\n")
            flash.error="Error updating user: ${errmsg}"
            flash.message = "Error updating user: ${errmsg}"
            flash.errors = user.errors
            return render(view:'edit',model:[user:u])
        }
        flash.message="User profile updated: ${user.login}"
        return redirect(action:'profile',params:[login: user.login])
        }.invalidToken {
            flash.error = g.message(code: 'request.error.invalidtoken.message')
            return render(view: 'edit', model: [user: user])
        }
    }
    public def update (User user) {
        withForm{
        if (user.hasErrors()) {
            flash.errors = user.errors
            return render(view: 'edit', model: [user: user])
        }
        //check auth to view profile
        //default to current user profile
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if(unauthorizedResponse(params.login == session.user || frameworkService.authorizeApplicationResourceType
                (authContext, AuthConstants.TYPE_USER,
                AuthConstants.ACTION_ADMIN), AuthConstants.ACTION_ADMIN,'User',params.login)){
            return
        }
        def User u = User.findByLogin(params.login)
        if(notFoundResponse(u,'User',params.login)){
            return
        }

        bindData(u,params.subMap(['firstName','lastName','email']))

        if(!u.save(flush:true)){
            def errmsg= u.errors.allErrors.collect { g.message(error:it) }.join("\n")
            request.error="Error updating user: ${errmsg}"
            return render(view:'edit',model:[user:u])
        }
        flash.message="User profile updated: ${params.login}"
        return redirect(action:'profile',params:[login:params.login])
        }.invalidToken {
            flash.error = g.message(code: 'request.error.invalidtoken.message')
            return render(view: 'edit', model: [user: user])
        }
    }

    def cancel(){
        return redirect(action:'profile')
    }
    def edit(){
        def model=profile()
        return model
    }



    def generateUserToken(String tokenTime, String tokenTimeUnit, String tokenUser, String tokenRoles) {
        boolean valid=false

        withForm{
            valid=true
            g.refreshFormTokensHeader()
        }.invalidToken{
            response.status=HttpServletResponse.SC_BAD_REQUEST
            request.error = g.message(code: 'request.error.invalidtoken.message')
            withFormat {
                html {
                    return render(view: 'profile', model: [user: user])
                }
                json {
                    render([error: request.error] as JSON)
                }
            }
        }
        if (!valid) {
            return
        }

        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        def tokenTimeString = null
        if (tokenTime && tokenTimeUnit) {
            tokenTimeString = tokenTime + tokenTimeUnit
        }

        if (tokenTimeString && !Sizes.validTimeDuration(tokenTimeString)) {
            def result = [result: false, error: "Duration format was not valid"]
            return renderTokenGenerateResult(result, params.login)
        }
        Integer tokenDurationSeconds = tokenTimeString ? Sizes.parseTimeDuration(tokenTimeString) : 0
        //check auth to edit profile
        //default to current user profile
        def result = [:]
        try {
            AuthToken token = apiService.generateUserToken(
                    authContext,
                    tokenDurationSeconds,
                    tokenUser ?: params.login,
                    AuthToken.parseAuthRoles(tokenRoles)
            )
            result = [result: true, /*apitoken: token.token, */ tokenid: token.uuid]
        } catch (Exception e) {
            result = [result: false, error: e.getCause().message]
        }
        return renderTokenGenerateResult(result, params.login)
    }

    def renderUsertoken(String tokenid) {
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        boolean valid = false

        withForm {
            valid = true
            g.refreshFormTokensHeader()
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.error = g.message(code: 'request.error.invalidtoken.message')
            withFormat {
                html {
                    return render(view: 'profile', model: [user: user])
                }
                json {
                    render([error: request.error] as JSON)
                }
            }
        }
        if (!valid) {
            return
        }
        def adminAuth = apiService.hasTokenAdminAuth(authContext)

        try {
            AuthToken token = adminAuth ?
                    apiService.findTokenId(tokenid) :
                    apiService.findUserTokenId(authContext.username, tokenid)
            if (!token) {
                renderTokenGenerateResult([result: false, error: 'Not Found'], authContext.username)
            }
            renderTokenGenerateResult([result: true, apitoken: token.token, tokenid: token.uuid], authContext.username)
        } catch (Exception e) {
            renderTokenGenerateResult([result: false, error: e.message], authContext.username)
        }
    }

    private Map renderTokenGenerateResult(Map result, String login) {
        if (result.error) {
            log.error result.error
        }
        withFormat {
            html {
                flash.error = result.error
                redirect(controller: 'user', action: 'profile', params: [login: login])
            }
            json {
                render(result as JSON)
            }
        }
    }

    def renderApiToken() {
        def login = params.login
        def result
        def user
        def token
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def adminAuth = apiService.hasTokenAdminAuth(authContext)
        if (!adminAuth && login != authContext.username) {
            def error = "Unauthorized: retrieve token for ${login}"
            log.error error
            return renderErrorFragment(error)
        }
        user = userService.findOrCreateUser(login)
        if (!user) {
            def error = "Couldn't find user: ${login}"
            log.error error
            result = [result: false, error: error]
        } else {
            def t = adminAuth ?
                    (apiService.findTokenId(params.tokenid)) :
                    apiService.findUserTokenId(login, params.tokenid);
            if (t) {
                token = t
            } else {
                def error = "Couldn't find user auth token: ${params.token}"
                log.error error
                result = [result: false, error: error]
            }
        }
        if(user && token){
            return render(template: 'token',model: [user:user,token:token]);
        }else{
            return renderErrorFragment(result.error)
        }

    }

    def removeExpiredTokens() {
        boolean valid = false
        withForm {
            valid = true
            g.refreshFormTokensHeader()
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.error = g.message(code: 'request.error.invalidtoken.message')
            withFormat {
                html {
                    return render(view: 'profile', model: [user: user])
                }
                json {
                    render([error: request.error] as JSON)
                }
            }
        }
        if (!valid) {
            return
        }
        def login = params.login
        def deleteall = params.deleteall?.toBoolean()
        def result
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def adminAuth = apiService.hasTokenAdminAuth(authContext)
        if (unauthorizedResponse(
                login == authContext.username || adminAuth,
                AuthConstants.ACTION_ADMIN,
                'API Tokens for user: ',
                params.login
        )) {
            return
        }
        if (unauthorizedResponse(
                !deleteall || adminAuth,
                AuthConstants.ACTION_ADMIN,
                'API Tokens for ',
                "All users"
        )) {
            return
        }

        def total = deleteall ? apiService.removeAllExpiredTokens() : apiService.removeAllExpiredTokens(login)

        if (total > 0) {
            flash.message = "Removed $total expired tokens"
        } else {
            flash.error = "No expired tokens found"
        }
        def done = false
        withFormat {
            html {

            }
            json {
                done = true
                render(result as JSON)
            }
        }
        if (done) {
            return
        }

        return redirect(controller: 'user', action: 'profile', params: [login: login])
    }
    def clearApiToken(User user) {
        boolean valid = false
        withForm {
            valid = true
            g.refreshFormTokensHeader()
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.error = g.message(code: 'request.error.invalidtoken.message')
            withFormat {
                html {
                    return render(view: 'profile', model: [user: user])
                }
                json {
                    render([error: request.error] as JSON)
                }
            }
        }
        if (!valid) {
            return
        }
        if (user.hasErrors()) {
            request.errors = user.errors
            withFormat {
                html {
                    return render(view: 'profile', model: [user: user])
                }
                json {
                    render([error: 'Invalid input'] as JSON)
                }
            }

        }
        def login = user.login
        def result
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def adminAuth = apiService.hasTokenAdminAuth(authContext)
        if (!params.token && !params.tokenid) {
            def error = "Parameter tokenid required"
            log.error error
            result = [result: false, error: error]
        } else if (params.No == 'No') {
            return redirect(controller: 'user', action: 'profile', params: [login: login])
        } else if (!adminAuth && login != authContext.username) {
            def error = "Unauthorized: admin role required"
            log.error error
            result = [result: false, error: error]
        } else {

            def User u = userService.findOrCreateUser(login)
            if (!u) {
                def error = "Couldn't find user: ${login}"
                log.error error
                result=[result: false, error: error]
            }else{
                def findtoken = params.tokenid ?: params.token
                AuthToken found = null
                if (adminAuth) {
                    //admin can delete any token
                    found = params.token ?
                            AuthToken.findByToken(params.token) :
                            apiService.findTokenId(params.tokenid)
                } else {
                    //users can delete owned token
                    found = params.token ?
                            AuthToken.findByTokenAndCreator(params.token, login) :
                            apiService.findUserTokenId(login, params.tokenid)
                }

                if(!found){
                    def error = "Couldn't find token ${findtoken} for user ${login}"
                    log.error error
                    result = [result: false, error: error]
                } else {
                    apiService.removeToken(found)
                    result = [result: true]
                }
            }
        }
        def done=false
        withFormat {
            html{

            }
            json{
                done=true
                render(result as JSON)
            }
        }
        if(done){
            return
        }

        if (result.error) {
            flash.error = result.error
        }
        return redirect(controller: 'user', action: 'profile', params: [login: login])

    }
    def setDashboardPref={
        def User u = userService.findOrCreateUser(session.user)
        def list=params.dpref.split(",").collect{Integer.parseInt(it)}
        (1..4).each{
            if(!list.contains(it)){
                return renderErrorView("Invalid pref order: ${params.dpref}")
            }
        }
        u.dashboardPref=params.dpref
        u.save()

        render(contentType:"text/json"){
            delegate.result="success"
            delegate.dashboard=u.dashboardPref
        }
    }

    def addFilterPref={
        withForm{
            def result = userService.storeFilterPref(session.user,params.filterpref)
            if(result.error){
                return renderErrorFragment(result.error)
            }
            def storedpref=result.storedpref

            //include new request tokens as headers in response
            g.refreshFormTokensHeader()

            render(contentType:"text/json"){
                delegate.result="success"
                delegate.filterpref=storedpref
            }

        }.invalidToken{
            response.status= HttpServletResponse.SC_BAD_REQUEST
            render(contentType: "text/json") {
                delegate.result = "error"
                delegate.message=g.message(code:'request.error.invalidtoken.message')
            }
        }
    }

}
