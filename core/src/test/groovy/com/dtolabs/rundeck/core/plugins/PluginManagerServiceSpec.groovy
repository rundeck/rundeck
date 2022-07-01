package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import groovy.transform.CompileStatic
import spock.lang.Specification

class PluginManagerServiceSpec extends Specification {

    def "create pluggable service"() {
        given:
            def sut = new PluginManagerService()
        when:
            def result = sut.createPluggableService(type)

        then:

            result instanceof BasePluginProviderService

        where:
            type           | expectedName
            NodeExecutor   | 'NodeExecutor'
            NodeStepPlugin | 'WorkflowNodeStep'
//            TestPlugin     | 'Test'

    }
    def "service names"() {
        given:
            def sut = new PluginManagerService()
        when:
            def result = sut.createServiceName(input)

        then:
            result == expected

        where:
            input       | expected
            'Abc'       | 'Abc'
            'AbcPlugin' | 'Abc'

    }
}
