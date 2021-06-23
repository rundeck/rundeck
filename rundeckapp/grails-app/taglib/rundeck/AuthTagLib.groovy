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

import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import com.dtolabs.rundeck.server.AuthContextEvaluatorCacheManager
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.app.authorization.AppAuthContextProcessor
import rundeck.services.FrameworkService;

class AuthTagLib {
    def static namespace="auth"
    def FrameworkService frameworkService
    AppAuthContextProcessor rundeckAuthContextProcessor
    AuthContextEvaluatorCacheManager authContextEvaluatorCacheManager
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

        def authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(request.subject,attrs.project)
        def resource = rundeckAuthContextProcessor.authResourceForJob(attrs.job?.jobName, attrs.job?.groupPath, attrs.job?.extid)

        def env = AuthorizationUtil.projectContext(attrs.project)

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

        def env = AuthorizationUtil.projectContext(attrs.project)


        def authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(request.subject,attrs.project)
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
            env = AuthorizationUtil.RUNDECK_APP_ENV
        } else {
            env = AuthorizationUtil.projectContext(attrs.project)
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

        def authContext = appContext?rundeckAuthContextProcessor.getAuthContextForSubject(request.subject):
                          rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(request.subject,attrs.project)

        if(other){
            boolean isAuth = false
            def projectNames = frameworkService.projectNames(authContext)
            projectNames.each{
                if(it != attrs.project){
                    def decision=  authContextEvaluatorCacheManager.evaluate(authContext, resources, tests as Set, it)
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

        def env = AuthorizationUtil.projectContext(attrs.project)


        def authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(request.subject,attrs.project)
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



        def Set resources = [rundeckAuthContextProcessor.authResourceForJob(attrs.job?.jobName, attrs.job?.groupPath, attrs.job?.extid) ]

        def env = AuthorizationUtil.projectContext(attrs.job?.project)

        def authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(request.subject,attrs.job?.project)
        def decisions = authContext.evaluate(resources, tests, env)
        //return true if all decsisions are (has==true) or are not (has!=true) authorized
        if (anyCheck) {
            return decisions.find { has ? it.authorized : !it.authorized }
        }
        return !(decisions.find { has ^ it.authorized })

    }
}
