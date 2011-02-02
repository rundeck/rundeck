/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */
 
/*
 * ApiRequestFilters.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Feb 1, 2011 12:14:56 PM
 * 
 */

public class ApiRequestFilters {
    def supported_versions=["1.2"]
    def allowed_actions=["renderError","invalid"]
    def filters = {
            /**
             * Require api version request header or parameter
             */
            apiVersion(uri:'/api/**') {
                before = {
                    if(controllerName=='api' && allowed_actions.contains(actionName)){
                        return true
                    }
                    final def header = request.getHeader('X-RUNDECK-API-VERSION')
                    final def apiversion = params.api_version
                    def reqversion = header?:apiversion
                    if(!reqversion){
                        flash.errorCode='api.error.api-version.required'
                        redirect(controller:'api',action:'renderError')
                        return false
                    }else if (!supported_versions.contains(reqversion)) {
                        flash.errorCode='api.error.api-version.unsupported'
                        flash.errorArgs=[reqversion]
                        redirect(controller:'api',action:'renderError')
                        return false
                    }
                    request.api_version=reqversion
                    return true
                }
            }
    }
}