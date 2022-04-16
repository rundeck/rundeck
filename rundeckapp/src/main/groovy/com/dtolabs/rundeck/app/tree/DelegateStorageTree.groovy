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
        delegate = creator.createOnStartup()
        configuration=creator.configuration
    }

    @Subscriber('rundeck.configuration.refreshed')
    @CompileDynamic
    def updateTreeConfig(def event) {
        Map<String, String> config = creator.getStorageConfigMap()
        if(configuration != config){
            delegate = creator.create()
            configuration = config
        }
    }
}