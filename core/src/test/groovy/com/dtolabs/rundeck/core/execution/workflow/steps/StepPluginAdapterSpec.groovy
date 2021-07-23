package com.dtolabs.rundeck.core.execution.workflow.steps

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.DynamicProperties
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import org.rundeck.app.spi.Services
import spock.lang.Specification

class StepPluginAdapterSpec extends Specification {
    static class TestPlugin1 implements StepPlugin, DynamicProperties {
        Map<String, Object> projectAndFrameworkValues
        Map<String, Object> dpropsvals
        Services services

        @Override
        void executeStep(final PluginStepContext context, final Map<String, Object> configuration)
            throws StepException {

        }

        @Override
        Map<String, Object> dynamicProperties(final Map<String, Object> projectAndFrameworkValues) {
            this.projectAndFrameworkValues = projectAndFrameworkValues
            return dpropsvals
        }

        @Override
        Map<String, Object> dynamicProperties(
            final Map<String, Object> projectAndFrameworkValues,
            final Services services
        ) {
            this.projectAndFrameworkValues = projectAndFrameworkValues
            this.services = services
            return dpropsvals
        }
    }

    def "dynamicProperties"() {
        given:
            def plugin = new TestPlugin1(dpropsvals: [x: 'y'])
            def sut = new StepPluginAdapter(plugin)
        when:
            def result = sut.dynamicProperties([a: 'b'])
        then:
            result == [x: 'y']
            plugin.projectAndFrameworkValues == [a: 'b']
            plugin.services == null
    }

    def "dynamicProperties with services"() {
        given:
            def plugin = new TestPlugin1(dpropsvals: [x: 'y'])
            def svcs = Mock(Services)
            def sut = new StepPluginAdapter(plugin)
        when:
            def result = sut.dynamicProperties([a: 'b'], svcs)
        then:
            result == [x: 'y']
            plugin.projectAndFrameworkValues == [a: 'b']
            plugin.services == svcs
    }

    static class TestPlugin2 implements StepPlugin, Describable {
        Description testDescription

        @Override
        Description getDescription() {
            return testDescription
        }

        @Override
        void executeStep(final PluginStepContext context, final Map<String, Object> configuration)
            throws StepException {

        }
    }

    def "get description describable"() {
        given:
            Description testDescription = Mock(Description)
            def plugin = new TestPlugin2(testDescription: testDescription)
            def svcs = Mock(Services)
            def sut = new StepPluginAdapter(plugin)
        when:
            def result = sut.description
        then:
            result == testDescription
    }

    @Plugin(service = 'dummy', name = 'testplugin3')
    @PluginDescription(title = 'something')
    static class TestPlugin3 implements StepPlugin {

        @Override
        void executeStep(final PluginStepContext context, final Map<String, Object> configuration)
            throws StepException {

        }
    }

    def "get description annotation"() {
        given:
            Description testDescription = Mock(Description)
            def plugin = new TestPlugin3()
            def sut = new StepPluginAdapter(plugin)
        when:
            def result = sut.description
        then:
            result
            result.name == 'testplugin3'
            result.title == 'something'
    }


}
