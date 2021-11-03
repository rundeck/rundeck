package org.rundeck.plugin.azureobjectstore

import org.rundeck.plugin.azureobjectstore.AzureObjectStorePlugin
import spock.lang.Specification

class AzureObjectStorePluginSpec extends Specification {

    def "test azure object store no container set"(){
        given:
        AzureObjectStorePlugin plugin = new AzureObjectStorePlugin()
        plugin.storageAccount = "test-account"
        plugin.accessKey = "test-key"

        when:

        plugin.initTree()

        then:

        Exception e = thrown()
        e.message == "container property is required"
    }

    def "test azure object store no key set"(){
        given:
        AzureObjectStorePlugin plugin = new AzureObjectStorePlugin()
        plugin.storageAccount = "test-account"
        plugin.container = "test-container"

        when:

        plugin.initTree()

        then:

        Exception e = thrown()
        e.message == "accessKey property is required"
    }

    def "test azure object store no account set"(){
        given:
        AzureObjectStorePlugin plugin = new AzureObjectStorePlugin()
        plugin.accessKey = "test-key"
        plugin.container = "test-container"

        when:

        plugin.initTree()

        then:

        Exception e = thrown()
        e.message == "storageAccount property is required"
    }

}