package org.rundeck.tests.functional.api.plugin

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class PluginSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
    }

    def "Does not return v40 fields on older versions" () {
        given:
            def client = clientProvider.client
            client.apiVersion = 39
            def plugin = client.get("/plugin/list", List<Map>)
        expect:
            def result = plugin.find { it.iconUrl || it.providerMetadata }
            result == null
        cleanup:
            client.apiVersion = client.finalApiVersion
    }

    def "Returns v40 field" () {
        given:
            def client = clientProvider.client
            def plugin = client.get("/plugin/list", List<Map>)
        expect:
            def result = plugin.find { it.iconUrl || it.providerMetadata }
            result != null
    }

}
