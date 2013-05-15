package rundeck.filters
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

    public static final int V1 = 1
    public static final int V2 = 2
    public static final int V3 = 3
    public static final int V4 = 4
    public static final int V5 = 5
    public static final int V6 = 6
    public static final int V7 = 7
    public final static int API_EARLIEST_VERSION=V1
    public final static int API_CURRENT_VERSION=V7
    public final static int API_MIN_VERSION=API_EARLIEST_VERSION
    public final static int API_MAX_VERSION=API_CURRENT_VERSION

    def allowed_actions=["renderError","invalid","error"]
    def filters = {
            /**
             * Require valid api version in request path /api/version/...
             */
            apiVersion(uri:'/api/**') {
                before = {
                    if(controllerName=='api' && allowed_actions.contains(actionName) || request.api_version){
                        return true
                    }

                    if(!params.api_version){
                        flash.errorCode='api.error.api-version.required'
                        redirect(controller:'api',action:'renderError')
                        return false
                    }
                    final def reqversion
                    def unsupported=!(params.api_version==~/^[1-9][0-9]*$/)
                    if(!unsupported){
                        try{
                            reqversion = Integer.parseInt(params.api_version)
                            if (reqversion < API_MIN_VERSION || reqversion > API_MAX_VERSION) {
                                unsupported = true
                            }
                        }catch (NumberFormatException e){
                            unsupported=true
                        }
                    }
                    if(unsupported){
                        render(contentType: "text/xml", encoding: "UTF-8") {
                            result(error: "true", apiversion: API_CURRENT_VERSION) {
                                delegate.'error' {
                                    message("Unsupported API Version \"${params.api_version}\". API Request: ${request.forwardURI}. Reason: Current version: ${API_CURRENT_VERSION}")
                                }
                            }
                        }
                        return false;
                    }
                    request.api_version=reqversion
                    return true
                }
            }
    }
}
