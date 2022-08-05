package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.PluginBlocklist
import groovy.transform.CompileStatic
import spock.lang.Specification

class BlocklistPluginRegistryComponentSpec extends Specification {

    def "is allowed"() {
        given:
            def name = 'aPlugin'
            def svcName = 'ServiceName'
            def sut = new BlocklistPluginRegistryComponent()
            sut.rundeckPluginBlocklist = Mock(PluginBlocklist)
            def svc = Mock(PluggableProviderService) {
                getName() >> svcName
            }
        when:
            def result = sut.isAllowed(name, svc)
        then:
            1 * sut.rundeckPluginBlocklist.isPluginProviderPresent(svcName, name) >> blocked
            result == allowed

        where:
            blocked || allowed
            false   || true
            true    || false
    }
}
