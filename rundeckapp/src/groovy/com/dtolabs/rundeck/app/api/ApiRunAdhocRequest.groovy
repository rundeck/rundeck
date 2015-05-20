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
