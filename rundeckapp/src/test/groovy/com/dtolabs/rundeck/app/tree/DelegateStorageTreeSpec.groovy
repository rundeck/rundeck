package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.StorageTree
import spock.lang.Specification

class DelegateStorageTreeSpec extends Specification {

        def "updateTreeConfig no storage updates"(){

            given:
                StorageTreeCreator creator = Mock(StorageTreeCreator){
                    getStorageConfigMap() >> ["config1": "config1Def", "config2": "config2Def"]
                }
                DelegateStorageTree tree = new DelegateStorageTree()
                tree.configuration = ["config1": "config1Def", "config2": "config2Def"]
                tree.creator=creator

            when:
                tree.updateTreeConfig(null)

            then:
            0 * creator.create()

        }

    def "updateTreeConfig w storage updates if refreshable #refreshable"(){

        given:
        DelegateStorageTree tree = new DelegateStorageTree()
        tree.configuration = ["config1": "config1Def", "config3": "config3Def"]
        tree.creator = Mock(StorageTreeCreator)
        tree.refreshable=refreshable
        (refreshable?1:0) * tree.creator.getStorageConfigMap() >> ["test.converter.path": "config1Def", "test.converter.type": "config2Def"]

        when:
        tree.updateTreeConfig(null)

        then:
        (refreshable?1:0) * tree.creator.create(false)
        where:
        refreshable << [true, false]
    }

    def "updateTreeConfig does not propagate a tree creation failure and keeps the previous tree"() {

        given:
        Map<String, String> startupConfig = ["provider.1.type": "db", "provider.1.path": "keys"]
        StorageTree previousTree = Mock(StorageTree)
        StorageTreeCreator creator = Mock(StorageTreeCreator) {
            create(true) >> previousTree
            getConfiguration() >> startupConfig
            getStorageConfigMap() >> [
                "provider.1.type": "db", "provider.1.path": "keys",
                "provider.2.type": "db", "provider.2.path": "keys"
            ]
        }
        DelegateStorageTree tree = new DelegateStorageTree()
        tree.creator = creator
        tree.refreshable = true
        tree.getDelegate()

        when:
        tree.updateTreeConfig(null)

        then:
        1 * creator.create(false) >> {
            throw new IllegalArgumentException("Cannot create TreeStack: multiple subpaths defined for: keys")
        }
        noExceptionThrown()
        tree.getDelegate().is(previousTree)
        tree.configuration == startupConfig
    }

}
