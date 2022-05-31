package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.StorageTree
import grails.events.annotation.Subscriber
import grails.util.Holders
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class DelegateStorageTree implements StorageTree, InitializingBean {
    @Delegate
    StorageTree delegate

    StorageTreeCreator creator
    Map<String,String> configuration

    @Override
    void afterPropertiesSet() {
        delegate = creator.create(true)
        configuration=creator.configuration
    }

    @Subscriber('rundeck.configuration.refreshed')
    @CompileDynamic
    def updateTreeConfig(def event) {
        Map<String, String> config = creator.getStorageConfigMap()
        Boolean providerTypeSet = false
        Boolean providerPathSet = false
        Boolean converterTypeSet = false
        Boolean converterPathSet = false
        Boolean providerPresent = false
        Boolean converterPresent = false
        for(entry in config){
            if(entry.getKey().toString().contains("provider")){
                providerPresent=true
                if(entry.getKey().toString().endsWith("path")){
                    providerPathSet=true
                }
                else if(entry.getKey().toString().endsWith("type")){
                    providerTypeSet=true
                }
            }
            if(entry.getKey().toString().contains("converter")){
                converterPresent=true
                if(entry.getKey().toString().endsWith("path")){
                    converterPathSet=true
                }
                else if(entry.getKey().toString().endsWith("type")){
                    converterTypeSet=true
                }
            }
        }
        if(configuration != config && (converterPresent && converterPathSet && converterTypeSet) || (providerPresent && providerPathSet && providerTypeSet)){
            delegate = creator.create(false)
            configuration = config
        }
    }
}