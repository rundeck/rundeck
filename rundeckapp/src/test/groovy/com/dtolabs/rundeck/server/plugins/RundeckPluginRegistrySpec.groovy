package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import spock.lang.Specification

class RundeckPluginRegistrySpec extends Specification {
    def "service names"() {
        given:
        def sut = new RundeckPluginRegistry()
        when:
        def result = sut.createServiceName(input)

        then:
        result == expected

        where:
        input       | expected
        'Abc'       | 'Abc'
        'AbcPlugin' | 'Abc'

    }

    static interface TestPlugin {

    }

    def "create pluggable service"() {
        given:
        def sut = new RundeckPluginRegistry()
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)
        when:
        def result = sut.createPluggableService(type)

        then:

        1 * sut.rundeckServerServiceProviderLoader.createPluginService(type, expectedName)

        where:
        type           | expectedName
        NodeExecutor   | 'NodeExecutor'
        NodeStepPlugin | 'WorkflowNodeStep'
        TestPlugin     | 'Test'

    }
}
