/*
 * Copyright 2017 Rundeck Inc, <http://rundeck.com>
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

package rundeck.interceptors

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.FrameworkResource
import com.dtolabs.rundeck.server.authorization.AuthConstants

import javax.servlet.http.HttpServletResponse

/**
 * This filter checks whether a user has selected a valid project to use, stored in session.project.  If none is stored in
 * the session, then a project is selected from the first available option:
 * It checks in order these  places:
 *
 * params.project
 * session.project
 * user preference for "project" stored in the User profile
 * the first project named alphabetically from all available projects
 *
 * if no project is selected after these checks, it redirects the user to the project create form
 *
 * --
 * The filter is applied to all actions except: createProject, selectProject and projectSelect (framework controller), to
 * allow the create project form to be used.
 */
class ProjectSelectInterceptor {
    def frameworkService

    int order = HIGHEST_PRECEDENCE + 100

    ProjectSelectInterceptor() {
        matchAll().excludes(controller: 'framework', action:'(createProject(Post)?|selectProject|projectSelect|noProjectAccess|(create|save|check|edit|view)ResourceModelConfig)')
    }


    boolean before() {
        if(InterceptorHelper.matchesStaticAssets(controllerName)) return true
        if (request.is_allowed_api_request || request.api_version || request.is_api_req) {
            //only default the project if not an api request
            return true
        }
        if(controllerName=='user' && ( actionName in ['logout','login'] )){
            return true
        }
        if(controllerName=='menu' && ( actionName in ['home'] )){
            return true
        }
        if (session && session.user && session.subject) {
            //get user authorizations
            def AuthContext authContext = frameworkService.userAuthContext(session)


            def selected = params.project
            if (selected && !(selected =~ FrameworkResource.VALID_RESOURCE_NAME_REGEX)) {
                response.setStatus(400)
                request.errorCode = 'project.name.invalid'
                params.project = null
                render(view: '/common/error')
                AA_TimerInterceptor.afterRequest(request, response, session)
                return false
            }
            if (selected && !frameworkService.existsFrameworkProject(selected)) {
                response.setStatus(404)
                request.title= 'Not Found'
                request.errorCode= 'scheduledExecution.project.invalid.message'
                request.errorArgs= [params.project]
                params.project=null
                render(view: '/common/error')
                AA_TimerInterceptor.afterRequest(request, response, session)
                return false
            }
            if (selected
                    && !frameworkService.authorizeApplicationResourceAny(authContext,
                    frameworkService.authResourceForProject(selected), [AuthConstants.ACTION_READ,
                                                                        AuthConstants.ACTION_ADMIN])) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN)
                request.errorCode = 'request.error.unauthorized.message'
                request.errorArgs = ['view', 'Project', selected]
                request.titleCode = 'request.error.unauthorized.title'
                params.project = null
                render(view: '/common/error')
                AA_TimerInterceptor.afterRequest(request, response, session)
                return false
            }
        }
        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
