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

package rundeck

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import com.dtolabs.rundeck.server.authorization.AuthConstants
import rundeck.services.FrameworkService;

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

        def authContext = frameworkService.getAuthContextForSubjectAndProject(request.subject,attrs.project)
        def resource = frameworkService.authResourceForJob(attrs.job?.jobName, attrs.job?.groupPath)

        def env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE+"project"), attrs.project))

        def decision = authContext.evaluate(resource, action, env)

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

        def resource = [ type: 'adhoc']

        def env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"project"), attrs.project))


        def authContext = frameworkService.getAuthContextForSubjectAndProject(request.subject,attrs.project)
        def decision = authContext.evaluate(resource, action, env)

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
        def check=resourceAllowedTest(attrs,body)

        if (check) {
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
        boolean anyCheck = ((null != attrs.any) && (attrs.any in [true,"true"]))
        boolean other = ((null != attrs.others) && (attrs.others in [true,"true"]))

        boolean auth = false
        if (!attrs.action) {
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }

        def action = attrs.action

        def List tests = []
        if (action instanceof String) {
            tests.add(action)
        } else if (action instanceof Collection) {
            tests.addAll(action)
        }

        def env

        def appContext = 'application' == attrs.context
        if (appContext) {
            env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"application"), 'rundeck'))
        } else {
            env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"project"), attrs.project))
        }
        def resource = [type: attrs.type ?: 'resource']
        def tagattrs = [:]
        tagattrs.putAll(attrs)
        tagattrs.remove('type')
        tagattrs.remove('action')
        tagattrs.remove('has')
        tagattrs.remove('context')
        tagattrs.remove('any')
        tagattrs.remove('others')
        def attributes = attrs.attributes ?: tagattrs
        if (attributes) {
            resource.putAll(attributes)
        }
        Set resources = [resource]

        def authContext = appContext?frameworkService.getAuthContextForSubject(request.subject):
                frameworkService.getAuthContextForSubjectAndProject(request.subject,attrs.project)

        if(other){
            boolean isAuth = false
            def projectNames = frameworkService.projectNames(authContext)
            projectNames.each{
                env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"project"), it))
                if(it != attrs.project){
                    def decision=  authContext.evaluate(resources, tests as Set, env)
                    if(!decision.find{has^it.authorized}){
                        isAuth = true
                    }
                }
            }
            return isAuth
        }

        if(anyCheck){
            return tests.any { authContext.evaluate(resources, [it] as Set, env).any{has==it.authorized} }
        }
        def decisions = authContext.evaluate(resources, tests as Set, env)
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

        def Set resources = [[type: 'adhoc']]

        def env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"project"), attrs.project))


        def authContext = frameworkService.getAuthContextForSubjectAndProject(request.subject,attrs.project)
        def decisions = authContext.evaluate(resources, tests, env)
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
        boolean anyCheck = ((null != attrs.any) && (attrs.any in [true, "true"]))
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
        
                

        def Set resources = [frameworkService.authResourceForJob(attrs.job?.jobName, attrs.job?.groupPath) ]

        def env = Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE +"project"), attrs.job?.project))

        def authContext = frameworkService.getAuthContextForSubjectAndProject(request.subject,attrs.job?.project)
        def decisions = authContext.evaluate(resources, tests, env)
        //return true if all decsisions are (has==true) or are not (has!=true) authorized
        if (anyCheck) {
            return decisions.find { has ? it.authorized : !it.authorized }
        }
        return !(decisions.find { has ^ it.authorized })

    }
}
