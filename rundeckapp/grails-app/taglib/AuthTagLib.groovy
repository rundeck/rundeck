import java.util.Collections;

import javax.security.auth.Subject

import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.AuthConstants;

class AuthTagLib {
    def static namespace="auth"
    def userService
    def FrameworkService frameworkService
    static returnObjectForTags = ['allowedTest']
    
    /**
     * Render an enclosed body if user authorization matches the assertion.  Attributes:
     *  'auth'= name of auth to check, 'has'= true/false [optional], 'altText'=failed assertion message [optional]
     *
     * if has is 'true' then the body is rendered if the user has the specified role.
     * if has is 'false' then the body is rendered if the user DOES NOT have the specified role.
     *
     * otherwise if altText is set, it is rendered.
     */
    def allowed={attrs,body->
        if(!attrs.action && !attrs.name){
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }
        
        def action = attrs.action
        if(!action) {
            action = attrs.name
        }
        
        
        if(!attrs.job) {
            throw new Exception("job required for action: " + action);
        }
        
        
        boolean has=(!attrs.has || attrs.has == "true")
        
        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()
        
        def resource = ["job": attrs.job?.jobName, "group": (attrs.job?.groupPath ?: "")]

        def env
        if (session.project) {
            env = Collections.singleton(new Attribute(new URI(AuthConstants.PROJECT_URI), session.project))
        } else {
            env = Collections.emptySet()
        }
        
        def decision = authr.evaluate(resource, request.subject, action, env)
        
        if(has && decision.authorized){
            out<<body()
        }else if(!has && !decision.authorized){
            out<<body()
        }else if(attrs.altText){
            out<<attrs.altText
        }
    }
    /**
     * return true if user authorization matches the assertion.  Attributes:
     *  'name'= name of auth to check, 'has'= true/false [optional], 'altText'=failed assertion message [optional]
     *  'name' can also be a list of auth names, and all of them must match
     *
     * if has is 'true' then the body is rendered if the user has the specified authorization.
     * if has is 'false' then the body is rendered if the user DOES NOT have the specified authorization.
     *
     */
    def allowedTest={attrs,body->
        boolean has=(null==attrs.has || attrs.has == "true")
        boolean auth=false
        if(!attrs.action && !attrs.name){
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }
        
        def action = attrs.action
        if(!action) {
            action = attrs.name
        }
        
        def Set tests=[]
        if(action instanceof String) {
            tests.add(action)
        } else if(action instanceof Collection){
            tests.addAll(action)
        }
        
        if(!attrs.job) {
            throw new Exception("job required for action: " + tests);
        }
        
                
//            def authorized = userService.userHasAuthorization(request.remoteUser,name)
        
        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()
        
        def Set resource = [ ["job": attrs.job?.jobName, "group": (attrs.job?.groupPath ?: "")] ]

        def env
        if(session.project){
            env = Collections.singleton(new Attribute(new URI(AuthConstants.PROJECT_URI),session.project))
        }else{
            env = Collections.emptySet()
        }
        
        authr.evaluate(resource, request.subject, tests, env).each{ def decision ->
            // has == true, authorized == true => auth = true
            // has == true, authorized == false => auth = false
            // has == false, authorized == true => auth = false
            // has == false, authorized == false => auth = true
            auth = !(has ^ decision.isAuthorized()) // inverse xor
            if(auth)
                return;
        }
        return auth;
    }
}
