package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.converters.JSON
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

    static allowedMethods = [
            addFilterPref:'POST',
            store:'POST',
            update:'POST',
            clearApiToken:'POST',
            generateApiToken:'POST',
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

    def profile={
        //check auth to view profile
        //default to current user profile
        if(!params.login){
            params.login=session.user
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if(unauthorizedResponse(params.login == session.user || frameworkService.authorizeApplicationResourceType
                (authContext, AuthConstants.TYPE_USER, AuthConstants.ACTION_ADMIN), AuthConstants.ACTION_ADMIN,'Users',
                params.login)){
            return
        }
        def User u = User.findByLogin(params.login)
        if(!u && params.login==session.user){
            //redirect to profile edit page, so user can setup their profile
            flash.message="Please fill out your profile"
            return redirect(action:register)
        }
        if(notFoundResponse(u, 'User', params['login'])){
            return
        }
        [user:u]
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
           return redirect(action:edit)
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

    def cancel={
        return redirect(action:'profile')
    }
    def edit={
        def model=profile(params)
        return model
    }
    def generateApiToken(User user) {
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
        if(!valid){
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
        //check auth to edit profile
        //default to current user profile
        def login = params.login
        def result
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!frameworkService.authorizeApplicationResourceType(authContext, AuthConstants.TYPE_USER,
                AuthConstants.ACTION_ADMIN)) {
            def error = "Unauthorized: admin role required"
            log.error error
            result=[result: false, error: error] 
        }else{
            def User u = userService.findOrCreateUser(login)
            if (!u) {
                def error = "Couldn't find user: ${login}"
                log.error error
                result=[result: false, error: error]
            }else{
                try{
                    AuthToken token =apiService.generateAuthToken(u)
                    result = [result: true, apitoken: token.token]
                }catch (Exception e){
                    result = [result: false, error: e.message]
                }
            }
        }
        withFormat {
            html{
                if (result.error) {
                    flash.error = result.error
                }else{
                    flash.newtoken=result.apitoken
                }
                redirect(controller: 'user', action: 'profile', params: [login: login])
            }
            json {
                render(result as JSON)
            }
        }
    }

    def renderApiToken = {
        //check auth to edit profile
        //default to current user profile

        def login = params.login
        def result
        def user
        def token
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!frameworkService.authorizeApplicationResourceType(authContext, AuthConstants.TYPE_USER, AuthConstants.ACTION_ADMIN)) {
            def error = "Unauthorized: admin role required"
            log.error error
            result = [result: false, error: error]
        } else {
            user = userService.findOrCreateUser(login)
            if (!user) {
                def error = "Couldn't find user: ${login}"
                log.error error
                result = [result: false, error: error]
            } else {
                def t= AuthToken.findByUserAndToken(user,params.token);
                if(t){
                    token=t
                }else{
                    def error = "Couldn't find user auth token: ${params.token}"
                    log.error error
                    result = [result: false, error: error]
                }
            }
        }
        if(user && token){
            return render(template: 'token',model: [user:user,token:token]);
        }else{
            return renderErrorFragment(result.error)
        }

    }

    def clearApiToken(User user) {
        boolean valid=false
        withForm{
            valid=true
            g.refreshFormTokensHeader()
        }.invalidToken{
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
        if(!valid){
            return
        }
        if (user.hasErrors()) {
            request.errors = user.errors
            withFormat {
                html{
                    return render(view: 'profile', model: [user: user])
                }
                json{
                    render([error: 'Invalid input'] as JSON)
                }
            }

        }
        def login = user.login
        def result
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!frameworkService.authorizeApplicationResourceType(authContext, AuthConstants.TYPE_USER, AuthConstants.ACTION_ADMIN)) {
            def error = "Unauthorized: admin role required"
            log.error error
            result=[result: false, error: error]
        }else if(!params.token){
            def error = "Parameter token required"
            log.error error
            result = [result: false, error: error]
        }else if (params.No == 'No') {
            return redirect(controller: 'user', action: 'profile', params: [login: login])
        }else if (request.getMethod()=='GET') {
            return redirect(controller: 'user', action: 'profile', params: [login: login,showConfirm:true,token:params.token])
        }else{
            def User u = userService.findOrCreateUser(login)
            if (!u) {
                def error = "Couldn't find user: ${login}"
                log.error error
                result=[result: false, error: error]
            }else{
                def findtoken=params.token
                def found=AuthToken.findByUserAndToken(u,findtoken)
                if(!found){
                    def error = "Couldn't find token ${findtoken} for user ${login}"
                    log.error error
                    result = [result: false, error: error]
                }else{

                    AuthToken oldtoken=found
                    def oldAuthRoles=oldtoken.authRoles

                    oldtoken.delete(flush: true)
                    log.info("EXPIRE TOKEN ${findtoken} for User ${login} with roles: ${oldAuthRoles}")
                    result= [result: true]
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
