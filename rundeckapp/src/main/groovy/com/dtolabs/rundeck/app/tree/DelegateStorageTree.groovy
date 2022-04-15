package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.StorageTree
import grails.events.annotation.Subscriber
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.beans.factory.InitializingBean

@CompileStatic
class DelegateStorageTree implements StorageTree, InitializingBean {
    @Delegate
    StorageTree delegate

    StorageTreeCreator creator


    def afterPropertiesSet() {
        
        delegate = creator.create()
    }

    @Subscriber('rundeck.configuration.change')
    @CompileDynamic
    def updateTreeConfig(def event) {
        List<String> keys = event.data*.key
        if(keys.any{(it.startsWith('rundeck.storage'))}){
            delegate = creator.create()
        }
    }
}
