/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.api

import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.core.api.ApiInfoService
import grails.web.mapping.LinkGenerator
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class ApiInfo implements ApiInfoService {
    @Autowired
    LinkGenerator grailsLinkGenerator

    @Override
    int getCurrentVersion() {
        ApiVersions.API_CURRENT_VERSION
    }

    @Override
    int getMinimumSupportedVersion() {
        ApiVersions.API_MIN_VERSION
    }

    @Override
    String getApiBaseURL() {
        return grailsLinkGenerator.link(uri: "/api/$currentVersion")
    }
}
