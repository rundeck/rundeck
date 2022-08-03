package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

class BaseSpringPluginRegistryComponentSpec extends Specification implements GrailsUnitTest {

    static class TestSpringPluginRegistryComponent extends BaseSpringPluginRegistryComponent {
        Map<String, Object> beans = [:]

        @Override
        def Object findProviderBean(final String type, final String name) {
            beans[type + ':' + name]
        }

        @Override
        Map<String, Object> getProviderBeans() {
            return beans
        }
    }

    def "load describable plugin using builder"() {
        given:
            def sut = new TestSpringPluginRegistryComponent()
            def description = DescriptionBuilder.builder()
                                                .name(provider)
                                                .property(PropertyBuilder.builder().string('prop1').build())
                                                .property(PropertyBuilder.builder().string('prop2').build())

                                                .build()
            def testPlugin = new TestPlugin2()
            testPlugin.description = description
            def beanBuilder = new TestBuilder(instance: testPlugin)

            def svc = Mock(PluggableProviderService) {
                getName() >> 'TestSvc'
            }
            sut.beans[svc.name + ':' + provider] = beanBuilder

        when:
            def result = sut.loadPluginDescriptorByName(provider, svc)
        then:
            result.description == description
            result.instance == testPlugin
            result.name == provider
        where:
            provider  | spec
            'aplugin' | 'aplugin'
            'aplugin' | 'TestSvc:aplugin'
    }

    def "load annotated plugin using builder"() {
        given:
            def sut = new TestSpringPluginRegistryComponent()

            def testPlugin = new TestPlugin3()
            def beanBuilder = new TestBuilder3(instance: testPlugin)

            def svc = Mock(PluggableProviderService) {
                getName() >> 'TestSvc'
            }
            sut.beans[svc.name + ':' + provider] = beanBuilder
        when:
            def result = sut.loadPluginDescriptorByName(provider, svc)
        then:
            result.description != null
            result.description.properties.size() == 2
            result.instance == testPlugin
            result.name == provider
        where:
            provider  | spec
            'aplugin' | 'aplugin'
            'aplugin' | 'TestSvc:aplugin'
    }

    def "load describable plugin direct"() {
        given:
            def sut = new TestSpringPluginRegistryComponent()
            def description = DescriptionBuilder.builder()
                                                .name(provider)
                                                .property(PropertyBuilder.builder().string('prop1').build())
                                                .property(PropertyBuilder.builder().string('prop2').build())

                                                .build()
            def testPlugin = new TestPlugin2()
            testPlugin.description = description


            def svc = Mock(PluggableProviderService) {
                getName() >> 'TestSvc'
            }
            sut.beans[svc.name + ':' + provider] = testPlugin
        when:
            def result = sut.loadPluginDescriptorByName(provider, svc)
        then:
            result.description == description
            result.instance == testPlugin
            result.name == provider
        where:
            provider  | spec
            'aplugin' | 'aplugin'
            'aplugin' | 'TestSvc:aplugin'
    }

    def "load annotated plugin direct"() {
        given:
            def sut = new TestSpringPluginRegistryComponent()

            def testPlugin = new TestPlugin3()

            def svc = Mock(PluggableProviderService) {
                getName() >> 'TestSvc'
            }
            sut.beans[svc.name + ':' + provider] = testPlugin
        when:
            def result = sut.loadPluginDescriptorByName(provider, svc)
        then:
            result.description != null
            result.description.properties.size() == 2
            result.instance == testPlugin
            result.name == provider
        where:
            provider  | spec
            'aplugin' | 'aplugin'
            'aplugin' | 'TestSvc:aplugin'
    }


    static class TestPlugin2 implements Configurable, Describable {
        Properties configuration
        Description description

        @Override
        void configure(final Properties configuration) throws ConfigurationException {
            this.configuration = configuration
        }
    }

    @Plugin(name = 'TestPlugin3', service = 'TestSvc')
    static class TestPlugin3 {
        @PluginProperty
        String prop1
        @PluginProperty
        String prop2
    }


    static class TestBuilder implements PluginBuilder<TestPlugin2> {
        TestPlugin2 instance

        @Override
        TestPlugin2 buildPlugin() {
            return instance
        }


        @Override
        Class<TestPlugin2> getPluginClass() {
            TestPlugin2
        }
    }

    static class TestBuilder3 implements PluginBuilder<TestPlugin3> {
        TestPlugin3 instance

        @Override
        TestPlugin3 buildPlugin() {
            return instance
        }


        @Override
        Class<TestPlugin3> getPluginClass() {
            TestPlugin3
        }
    }
}
