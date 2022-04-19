package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.StorageTree
import spock.lang.Specification

class DelegateStorageTreeSpec extends Specification {

        def "updateTreeConfig no storage updates"(){

            given:
                StorageTreeCreator creator = Mock(StorageTreeCreator){
                    getStorageConfigMap() >> ["config1": "config1Def", "config2": "config2Def"]
                }
                creator.configuration = ["config1": "config1Def", "config2": "config2Def"]
                DelegateStorageTree tree = new DelegateStorageTree()
                tree.configuration = ["config1": "config1Def", "config2": "config2Def"]

            when:
                tree.updateTreeConfig(null)

            then:
                tree.delegate == null
        }

    def "updateTreeConfig w storage updates"(){

        given:
        StorageTreeCreator creator = Mock(StorageTreeCreator){
            getStorageConfigMap() >> ["config1": "config1Def", "config2": "config2Def"]
        }
        DelegateStorageTree tree = new DelegateStorageTree()
        tree.creator = creator
        tree.configuration = ["config1": "config1DefUpdate", "config2": "config2Def"]

        when:
        tree.updateTreeConfig(null)

        then:
        tree.delegate == null
    }

}
