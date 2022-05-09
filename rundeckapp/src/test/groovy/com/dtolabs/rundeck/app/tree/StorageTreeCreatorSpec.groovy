package com.dtolabs.rundeck.app.tree

import rundeck.services.ConfigurationService
import spock.lang.Specification

class StorageTreeCreatorSpec extends Specification{

    def "getStorageConfigMap"(){
        given:
        int index =1

        Map<String, String> configProps = ["address":"testaddress", "prefix":"somePrefix"]
        Map<String, Object> configMap = ["type":"test-type", "path": "testPath"]
        configMap.put("config", configProps)
        Map<String, Map> indexMap = ["1":configMap]
        Map<String, Map> providerMap = ["provider":indexMap]
        Map<String, Map> storageMap = ["storage":providerMap]


        StorageTreeCreator creator = new StorageTreeCreator()
        creator.configurationService = Mock(ConfigurationService){
            getAppConfig() >> storageMap
        }

        when:
           def result = creator.getStorageConfigMap()

        then:
            result.size()==5
            result.containsKey("provider.1.type")
            result.containsKey("provider.1.config.address")

    }

}
