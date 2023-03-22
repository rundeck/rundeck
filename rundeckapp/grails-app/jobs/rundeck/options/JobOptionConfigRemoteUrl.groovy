package rundeck.options;

import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigEntry
import com.fasterxml.jackson.annotation.JsonIgnore;

class JobOptionConfigRemoteUrl implements JobOptionConfigEntry {

    RemoteUrlAuthenticationType authenticationType
    String username
    String passwordStoragePath
    String keyName
    String tokenStoragePath
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
