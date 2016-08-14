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

package com.dtolabs.rundeck.app.api

import com.dtolabs.rundeck.core.common.FrameworkResource
import grails.validation.Validateable

/**
 * Created by greg on 5/20/15.
 */

@Validateable
class ApiRunAdhocRequest {
    String project
    String exec
    String script
    String scriptInterpreter
    String argString
    Boolean interpreterArgsQuoted
    String url
    String description
    String filter
    String asUser
    String fileExtension
    Boolean nodeKeepgoing
    Integer nodeThreadcount
    static constraints={
        filter(nullable:true)
        asUser(nullable:true)
        nodeKeepgoing(nullable:true)
        nodeThreadcount(nullable:true)
        exec(nullable:true)
        script(nullable:true)
        scriptInterpreter(nullable:true)
        argString(nullable:true)
        interpreterArgsQuoted(nullable:true)
        url(nullable:true)
        fileExtension(nullable:true)
        description(nullable:true)
        project(nullable:false, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
    }

    @Override
    public String toString() {
        return "ApiRunAdhocRequest{" +
                "project='" + project + '\'' +
                ", exec='" + exec + '\'' +
                ", script='" + script + '\'' +
                ", scriptInterpreter='" + scriptInterpreter + '\'' +
                ", argString='" + argString + '\'' +
                ", interpreterArgsQuoted=" + interpreterArgsQuoted +
                ", url='" + url + '\'' +
                ", description='" + description + '\'' +
                ", filter='" + filter + '\'' +
                ", asUser='" + asUser + '\'' +
                ", fileExtension='" + fileExtension + '\'' +
                ", nodeKeepgoing=" + nodeKeepgoing +
                ", nodeThreadcount=" + nodeThreadcount +
                '}';
    }
}
