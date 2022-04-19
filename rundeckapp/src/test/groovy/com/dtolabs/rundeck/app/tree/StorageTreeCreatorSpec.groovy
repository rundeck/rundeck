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

        StorageTreeCreator creator = Mock(StorageTreeCreator)
        creator.configurationService = Mock(ConfigurationService){
            getAppConfig() >> Mock(Map){
                get("storage") >> Mock(Map){
                    get("provider") >> Mock(Map){
                        each {
                            it.key.toString().isInteger() >> true
                            it.key.toInteger() >> index
                            it.value >> configMap
                        }
                    }
                }
            }
        }
        when:
           def result = creator.getStorageConfigMap()

        then:
            result.size()==4
            result.containsKey("provider.1.type")
            result.containsKey("provider.1.config.address")

    }

}
