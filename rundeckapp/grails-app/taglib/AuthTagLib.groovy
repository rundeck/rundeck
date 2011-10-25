import java.util.Collections;

import javax.security.auth.Subject

import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;

class AuthTagLib {
    def static namespace="auth"
    def FrameworkService frameworkService
    static returnObjectForTags = ['jobAllowedTest','adhocAllowedTest', 'resourceAllowedTest']
    
    /**
     * Render an enclosed body if user authorization matches the assertion.  Attributes:
     *  'auth'= name of auth to check, 'has'= true/false [optional], 'altText'=failed assertion message [optional]
     *
     * if has is 'true' then the body is rendered if the user has the specified role.
     * if has is 'false' then the body is rendered if the user DOES NOT have the specified role.
     *
     * otherwise if altText is set, it is rendered.
     */
    def jobAllowed ={attrs,body->
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
        
        def resource = ["job": attrs.job?.jobName, "group": (attrs.job?.groupPath ?: ""), type: 'job']

        def env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE+"project"), session.project))

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
     * Render an enclosed body if user authorization matches the assertion.  Attributes:
     *  'auth'= name of auth to check, 'has'= true/false [optional], 'altText'=failed assertion message [optional]
     *
     * if has is 'true' then the body is rendered if the user has the specified role.
     * if has is 'false' then the body is rendered if the user DOES NOT have the specified role.
     *
     * otherwise if altText is set, it is rendered.
     */
    def adhocAllowed ={attrs,body->
        if(!attrs.action ){
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }

        def action = attrs.action

        boolean has=(!attrs.has || attrs.has == "true")

        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()

        def resource = [ type: 'adhoc']

        def env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"project"), session.project))

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
     * Render an enclosed body if user authorization matches the assertion.  Attributes:
     *  'auth'= name of auth to check, 'has'= true/false [optional], 'altText'=failed assertion message [optional]
     *
     * if has is 'true' then the body is rendered if the user has the specified role.
     * if has is 'false' then the body is rendered if the user DOES NOT have the specified role.
     *
     * otherwise if altText is set, it is rendered.
     */
    def resourceAllowed = {attrs, body ->
        if (!attrs.action) {
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }

        def action = attrs.action
        def isset=false
        if(action instanceof Collection){
            action = action as Set
            isset=true
        }

        boolean has = (!attrs.has || attrs.has == "true")

        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()

        def env
        if ('application'==attrs.context){
            env=Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"application"), 'rundeck'))
        }else{
            env=Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"project"), session.project))
        }
        def resource = [type: attrs.type?:'resource']
        def tagattrs=[:]
        tagattrs.putAll(attrs)
        tagattrs.remove('type')
        tagattrs.remove('action')
        tagattrs.remove('has')
        tagattrs.remove('context')
        def attributes = attrs.attributes?:tagattrs
        if(attributes){
            resource.putAll(attributes)
        }

        def authorized
        if(isset){
            def decisions = authr.evaluate([resource] as Set, request.subject, action, env)
            authorized = !(decisions.find{!it.authorized})
        } else{
            def Decision decision = authr.evaluate(resource, request.subject, action, env)
            authorized=decision.authorized
        }

        if (!(has ^ authorized)) {
            out << body()
        } else if (attrs.altText) {
            out << attrs.altText
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
    def resourceAllowedTest = {attrs, body ->
        boolean has = (null == attrs.has || attrs.has == "true")
        boolean auth = false
        if (!attrs.action) {
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }

        def action = attrs.action

        def Set tests = []
        if (action instanceof String) {
            tests.add(action)
        } else if (action instanceof Collection) {
            tests.addAll(action)
        }

        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()
        def env
        if ('application' == attrs.context) {
            env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"application"), 'rundeck'))
        } else {
            env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"project"), session.project))
        }
        def resource = [type: attrs.type ?: 'resource']
        def tagattrs = [:]
        tagattrs.putAll(attrs)
        tagattrs.remove('type')
        tagattrs.remove('action')
        tagattrs.remove('has')
        tagattrs.remove('context')
        def attributes = attrs.attributes ?: tagattrs
        if (attributes) {
            resource.putAll(attributes)
        }
        def Set resources = [resource]

        def decisions= authr.evaluate(resources, request.subject, tests, env)
        //return true if all decsisions are (has==true) or are not (has!=true) authorized
        return !(decisions.find{has^it.authorized})

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
    def adhocAllowedTest = {attrs, body ->
        boolean has = (null == attrs.has || attrs.has == "true")
        boolean auth = false
        if (!attrs.action ) {
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }

        def action = attrs.action

        def Set tests = []
        if (action instanceof String) {
            tests.add(action)
        } else if (action instanceof Collection) {
            tests.addAll(action)
        }

        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()

        def Set resources = [[type: 'adhoc']]

        def env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"project"), session.project))


        def decisions = authr.evaluate(resources, request.subject, tests, env)
        //return true if all decsisions are (has==true) or are not (has!=true) authorized
        return !(decisions.find {has ^ it.authorized})
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
    def jobAllowedTest ={attrs,body->
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
        
                

        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()
        
        def Set resources = [ ["job": attrs.job?.jobName, "group": (attrs.job?.groupPath ?: ""), type:'job'] ]

        def env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"project"), session.project))


        def decisions = authr.evaluate(resources, request.subject, tests, env)
        //return true if all decsisions are (has==true) or are not (has!=true) authorized
        return !(decisions.find {has ^ it.authorized})
    }
}
