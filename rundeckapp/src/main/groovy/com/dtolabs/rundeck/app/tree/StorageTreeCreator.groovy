package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageTreeFactory
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ConfigurationService

@CompileStatic
class StorageTreeCreator {
    @Autowired
    IPropertyLookup frameworkPropertyLookup
    @Autowired
    pluginRegistry
    @Autowired
    def storagePluginProviderService
    @Autowired
    def storageConverterPluginProviderService

    @Autowired
    ConfigurationService configurationService

    def storageConfigPrefix='provider'
    def converterConfigPrefix='converter'
    def baseStorageType='file'
    Map baseStorageConfig
    def defaultConverters=['StorageTimestamperConverter','KeyStorageLayer']
    def loggerName='org.rundeck.storage.events'

    StorageTree create(){
        def factory = new StorageTreeFactory(

        )
        factory.frameworkPropertyLookup = frameworkPropertyLookup
        //todo: read from configurationService
        factory.configuration=[:]

        factory.createTree()
    }
}
