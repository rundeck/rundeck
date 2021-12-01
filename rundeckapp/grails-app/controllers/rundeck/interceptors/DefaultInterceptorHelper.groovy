/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
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

import groovy.transform.CompileStatic
import org.rundeck.app.access.InterceptorHelper
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ConfigurationService

import javax.servlet.http.HttpServletRequest

@CompileStatic
class DefaultInterceptorHelper implements InterceptorHelper, InitializingBean {
    private List<String> allowedControllers = []
    private List<String> allowedPaths = []

    @Autowired
    ConfigurationService configurationService

    @Override
    boolean matchesAllowedAsset(String controllerName, HttpServletRequest request) {
        return allowedControllers.contains(controllerName) || matchesStaticServletPath(request.pathInfo)
    }

    boolean matchesStaticServletPath(String pathInfo) {
        return allowedPaths.contains(pathInfo)
    }

    @Override
    void afterPropertiesSet() throws Exception {
        allowedControllers = (List<String>)configurationService.getValue("security.interceptor.allowed.controllers",[])
        allowedPaths = (List<String>)configurationService.getValue("security.interceptor.allowed.paths",[])
    }
}
