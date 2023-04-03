package org.rundeck.app.jobs.options;

import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigEntry
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude;

class JobOptionConfigRemoteUrl implements JobOptionConfigEntry {
    public static final String TYPE = "remote-url";

    @Override
    String configType() {
        return TYPE
    }

    RemoteUrlAuthenticationType authenticationType

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String username

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String passwordStoragePath

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String keyName

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String tokenStoragePath

    @JsonInclude(JsonInclude.Include.NON_NULL)
    ApiTokenReporter apiTokenReporter

    @JsonIgnore
    String password

    @JsonIgnore
    String token

    @JsonIgnore
    String errors

    static JobOptionConfigRemoteUrl fromMap(Map map){
        JobOptionConfigRemoteUrl configRemoteUrl = new JobOptionConfigRemoteUrl()
        configRemoteUrl.authenticationType = RemoteUrlAuthenticationType.valueOf(map.authenticationType)
        if(map.username){
            configRemoteUrl.username = map.username
        }

        if(map.passwordStoragePath){
            configRemoteUrl.passwordStoragePath = map.passwordStoragePath
        }

        if(map.keyName){
            configRemoteUrl.keyName = map.keyName
        }

        if(map.tokenStoragePath){
            configRemoteUrl.tokenStoragePath = map.tokenStoragePath
        }
        if(map.apiTokenReporter){
            configRemoteUrl.apiTokenReporter = ApiTokenReporter.valueOf(map.apiTokenReporter)
        }

        return configRemoteUrl
    }

    @Override
    Map toMap(){
        Map map = [:]
        if(authenticationType){
            map.authenticationType=authenticationType.name()
        }
        if(username){
            map.username=username
        }
        if(passwordStoragePath){
            map.passwordStoragePath=passwordStoragePath
        }
        if(keyName){
            map.keyName=keyName
        }
        if(tokenStoragePath){
            map.tokenStoragePath=tokenStoragePath
        }
        if(apiTokenReporter){
            map.apiTokenReporter=apiTokenReporter.name()
        }

        return map
    }

}
