package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageTreeFactory
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import grails.util.Holders
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ConfigurationService

@CompileStatic
class StorageTreeCreator implements TreeCreator{
    IPropertyLookup frameworkPropertyLookup
    PluginRegistry pluginRegistry
    PluggableProviderService<StoragePlugin> storagePluginProviderService
    PluggableProviderService<StorageConverterPlugin> storageConverterPluginProviderService
    Map<String, String> startupConfiguration

    @Autowired
    ConfigurationService configurationService

    def storageConfigPrefix
    def converterConfigPrefix
    def baseStorageType
    Map baseStorageConfig
    String appConfigString
    List<String> defaultConverters=[]
    def loggerName
    Map<String, String> configuration

    StorageTree create(Boolean startup){
        def factory = new StorageTreeFactory()
        factory.frameworkPropertyLookup = frameworkPropertyLookup
        factory.pluginRegistry=pluginRegistry
        factory.storagePluginProviderService=storagePluginProviderService
        factory.storageConverterPluginProviderService=storageConverterPluginProviderService
        factory.storageConfigPrefix=storageConfigPrefix
        factory.converterConfigPrefix=converterConfigPrefix
        factory.baseStorageType=baseStorageType
        factory.baseStorageConfig=baseStorageConfig
        factory.loggerName=loggerName
        factory.defaultConverters=defaultConverters.toSet()

        if(startup){
            factory.configuration=startupConfiguration
            configuration=startupConfiguration
        }
        else{
            Map<String, String> storageConfigMap = getStorageConfigMap()
            factory.configuration=storageConfigMap
            configuration=storageConfigMap
        }
        factory.createTree()
    }

    /**
     * Returns the base storage map, using the `appConfigString` to load a config map
     * @return
     */
    private Map<String, Map> getBaseConfigMap() {
        def value = configurationService.getValue(appConfigString)
        if(value instanceof Map){
            return (Map<String,Map>) value
        }else{
            throw new IllegalStateException("appConfigString did not evaluate to a Map: ${appConfigString}")
        }
    }

    Map<String, String> getStorageConfigMap(){
        Map<String, Map> storageMap = getBaseConfigMap()
        Map<String, String> finalconfigMap = [:]
        Map<String, Map> providerMap = storageMap.get(storageConfigPrefix) as Map<String, Map>
        Map<String, Map> converterMap = storageMap.get(converterConfigPrefix) as Map<String, Map>
        finalconfigMap.put("default", "deleteMe")
        providerMap?.each {
            if (it.key.toString().isInteger()) {
                int index = it.key.toInteger()
                Map<String, Map> finalMap = it.value as Map<String, Map>
                finalMap.each {
                    if (it.key == "config") {
                        Map<String, String> configMap = it.value as Map<String, String>
                        configMap.each {
                            finalconfigMap.put("provider." + index + "." + "config." + it.key, it.value.toString())
                        }
                    } else {
                        finalconfigMap.put("provider." + index + "." + it.key, it.value.toString())
                    }

                }
            }
        }
        converterMap?.each {
            if (it.key.toString().isInteger()) {
                int index = it.key.toInteger()
                Map<String, Map> finalMap = it.value as Map<String, Map>
                finalMap.each {
                    if (it.key == "config") {
                        Map<String, String> configMap = it.value as Map<String, String>
                        configMap.each {
                            finalconfigMap.put("converter." + index + "." + "config." + it.key, it.value.toString())
                        }
                    } else {
                        finalconfigMap.put("converter." + index + "." + it.key, it.value.toString())
                    }
                }
            }
        }
        return finalconfigMap
    }
}