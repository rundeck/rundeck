import com.dtolabs.rundeck.core.common.Framework
import javax.servlet.http.HttpSession
import java.security.SecureRandom
import grails.converters.JSON
import org.apache.commons.lang.RandomStringUtils
import com.dtolabs.rundeck.server.authorization.AuthConstants

class UserController {
    UserService userService
    FrameworkService frameworkService
    def grailsApplication

    def index = {
        redirect(action:"login")
    }
    def error = {
        flash.error="Invalid username and password."
        return render(view:'login')
    }
    def logout = {
        session.invalidate()
        if(params.refLink && grailsApplication.config.grails.serverURL && params.refLink.startsWith(grailsApplication.config.grails.serverURL)){
            return redirect(url:params.refLink)
        }else{
            return redirect(controller:'menu', action:'index')
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
        render(template:'/common/error',model:[:])
    }
    def deniedFragment={
        response.setStatus(403)
        render(template:'/common/messages',model:[:])
    }
    def list={
        def Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        if(!frameworkService.authorizeApplicationResourceType(framework,'user', AuthConstants.ACTION_ADMIN)){
            flash.error="User Admin role required"
            flash.title="Unauthorized"
            return denied()
        }
        [users:User.listOrderByLogin()]
    }

    def profile={
        //check auth to view profile
        //default to current user profile
        if(!params.login){
            params.login=session.user
        }
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if(params.login!=session.user && !frameworkService.authorizeApplicationResourceType(framework, 'user', AuthConstants.ACTION_ADMIN)){
            flash.error="Unauthorized: admin role required"
            return render(template:"/common/error")
        }
        def User u = User.findByLogin(params.login)
        if(!u){
            if(params.login==session.user){
                //redirect to profile edit page, so user can setup their profile
                flash.message="Please fill out your profile"
                return redirect(action:register)
            }else{
                flash.error="User not found: ${params.login}"
                return render(template:"/common/error")
            }
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
    def store={

        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if(params.login!=session.user && !frameworkService.authorizeApplicationResourceType(framework, 'user', AuthConstants.ACTION_ADMIN)) {
            //require user_admin/admin if modifying a different user profile
            flash.error="Unauthorized: admin role required"
            return render(template:"/common/error")
        }
        def User u = User.findByLogin(params.login)
        if(u){
            flash.error="User alread exists: ${params.login}"
            return render(template:"/common/error")
        }
        u = new User(params.subMap(['login','firstName','lastName','email']))

        if(!u.save(flush:true)){
            def errmsg= u.errors.allErrors.collect { g.message(error:it) }.join("\n")
            flash.error="Error updating user: ${errmsg}"
            flash.message = "Error updating user: ${errmsg}"
            return render(view:'edit',model:[user:u])
        }
        flash.message="User profile updated: ${params.login}"
        return redirect(action:'profile',params:[login:params.login])
    }
    def update={
        //check auth to view profile
        //default to current user profile
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if(params.login!=session.user && !frameworkService.authorizeApplicationResourceType(framework, 'user', AuthConstants.ACTION_ADMIN)) {
            flash.error="Unauthorized: admin role required"
            return render(template:"/common/error")
        }
        def User u = User.findByLogin(params.login)
        if(!u){
            flash.error="User not found: ${params.login}"
            return render(template:"/common/error")
        }

        bindData(u,params.subMap(['firstName','lastName','email']))

        if(!u.save(flush:true)){
            def errmsg= u.errors.allErrors.collect { g.message(error:it) }.join("\n")
            flash.error="Error updating user: ${errmsg}"
            return render(view:'edit',model:[user:u])
        }
        flash.message="User profile updated: ${params.login}"
        return redirect(action:'profile',params:[login:params.login])
    }

    def cancel={
        return redirect(action:'profile')
    }
    def edit={
        def model=profile(params)
        return model
    }
//    private static SecureRandom srandom=new SecureRandom()

    private String genRandomString() {
//        return new BigInteger(130, srandom).toString(32)
        return RandomStringUtils.random(32,"rundeckRUNDECK0123456789dvopsDVOPS")
    }
    def generateApiToken={
        //check auth to edit profile
        //default to current user profile
        def login = params.login
        def result
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!frameworkService.authorizeApplicationResourceType(framework, 'user', AuthConstants.ACTION_ADMIN)) {
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
                String newtoken= genRandomString()
                while(AuthToken.findByToken(newtoken) != null){
                    newtoken = genRandomString()
                }
                final userroles = request.subject?.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class).collect { it.name }
                AuthToken token = new AuthToken(token:newtoken,authRoles: userroles.join(","),user:u)

                if(token.save()){
                    log.debug("GENERATE TOKEN ${newtoken} for User ${login} with roles: ${token.authRoles}")
                    result= [result: true, apitoken: newtoken]
                }else{
                    def msg= "Failed to save token for User ${login}"
                    log.error(msg)
                    result= [result: false,error:msg]
                }
            }
        }
        def done=false
        withFormat {
            html{

            }
            json {
                done=true
                render(result as JSON)
            }
        }
        if (done) {
            return
        }
        if (result.error) {
            flash.error = result.error
        }else{
            flash.newtoken=result.apitoken
        }
        return redirect(controller: 'user', action: 'profile', params: [login: login])
    }

    def renderApiToken = {
        //check auth to edit profile
        //default to current user profile

        def login = params.login
        def result
        def user
        def token
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!frameworkService.authorizeApplicationResourceType(framework, 'user', AuthConstants.ACTION_ADMIN)) {
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
            flash.error=result.error
            return render(template:'/common/error')
        }

    }

    def clearApiToken={
        def login = params.login
        def result
        def Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!frameworkService.authorizeApplicationResourceType(framework, 'user', AuthConstants.ACTION_ADMIN)) {
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
        if(!u){
            log.error("Couldn't find user: ${session.user}")
            flash.error="Couldn't find user: ${session.user}"

            return render(template:"/common/error")
        }
        def list=params.dpref.split(",").collect{Integer.parseInt(it)}
        (1..4).each{
            if(!list.contains(it)){
                flash.error="Invalid pref order: ${params.dpref}"
                return render(template:"/common/error")
            }
        }
        u.dashboardPref=params.dpref
        u.save()

        render(contentType:"text/json"){
            result("success")
            dashboard(u.dashboardPref)
        }
    }

    def addFilterPref={
        def result = userService.storeFilterPref(session.user,params.filterpref)
        if(result.error){
            flash.error=result.error
            return render(template:"/common/error")
        }
        def storedpref=result.storedpref

        render(contentType:"text/json"){
            delegate.'result'("success")
            filterpref(storedpref)
        }
    }

}
