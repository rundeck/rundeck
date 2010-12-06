import com.dtolabs.rundeck.core.common.Framework
import javax.servlet.http.HttpSession

class UserController {
    UserService userService
    RoleService roleService
    def grailsApplication

    def index = {
        redirect(action:"login")
    }
    def error = {
        session.invalidate()
        flash.error="Invalid username and password."
        redirect(controller:'menu', action:'index')
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
        if(!roleService.isUserInAnyRoles(request,['admin','user_admin'])){
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
        if(params.login!=session.user && !roleService.isUserInAnyRoles(request,['admin','user_admin'])){
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
        u = new User(login:params.login,authorization:UserAuth.createDefault())
        u.dashboardPref="1,2,3,4"

        def model=[user: u,newRegistration:true]
        return model
    }
    def store={
        if(params.login!=session.user && !roleService.isUserInAnyRoles(request,['admin','user_admin'])){
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
        if(params.login!=session.user && !roleService.isUserInAnyRoles(request,['admin','user_admin'])){
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

    def setDashboardPref={
        def User u = userService.findOrCreateUser(session.user)
        if(!u){
            log.error("Couldn't find user: ${session.user}")
            flash.error="Couldn't find user: ${session.user}"
            /*

             render(contentType:"text/json"){
                error("Execution with id "+params.id+" not found")
                id(params.id.toString())
                dataoffset("0")
                iscompleted(false)
                entries(){
                }
            }
             */
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
        def User u = userService.findOrCreateUser(session.user)
        if(!u){
            log.error("Couldn't find user: ${session.user}")
            flash.error="Couldn't find user: ${session.user}"
            return render(template:"/common/error")
        }
        def inpref = parseKeyValuePref(params.filterpref)
        def storedpref=parseKeyValuePref(u.filterPref)
        storedpref.putAll(inpref)
        storedpref=storedpref.findAll{it.value!='!'}

        u.filterPref=genKeyValuePref(storedpref)
        u.save()

        render(contentType:"text/json"){
            result("success")
            filterpref(storedpref)
        }
    }

    /**
     * Parse a "key=value,key=value" string and return a Map of string->String
     */
    public static Map parseKeyValuePref(String pref){


        def inpref =[:]
        if(pref){
            def list=pref.split(",")
            list.each{String item->
                def p=item.split("=",2)
                if(p[1]){
                    inpref[p[0]]=p[1]
                }
            }
        }
        return inpref
    }



    /**
    * Take a map of String->String and generate a string like "key=value,key2=value2"
     */
    public static String genKeyValuePref(Map map){
        if(map){
            return map.collect{item-> item.key+"="+item.value }.join(",")
        }
        return ""
    }
}
