package com.dtolabs.rundeck.server.plugins


import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

class RundeckSpringPluginRegistryComponentSpec extends Specification implements GrailsUnitTest {

    def "load plugin using builder"() {
        given:
            def sut = new BaseSpringPluginRegistryComponent()
            sut.applicationContext = applicationContext
            def description = DescriptionBuilder.builder()
                                                .name(provider)
                                                .property(PropertyBuilder.builder().string('prop1').build())
                                                .property(PropertyBuilder.builder().string('prop2').build())

                                                .build()
            def testPlugin = new TestPlugin2()
            testPlugin.description = description
            def beanBuilder = new TestBuilder(instance: testPlugin)
            defineBeans {
                testBeanBuilder(InstanceFactoryBean, beanBuilder)
            }

            sut.pluginRegistryMap = [(spec): 'testBeanBuilder']

            def svc = Mock(PluggableProviderService) {
                getName() >> 'TestSvc'
            }
        when:
            sut.loadPluginDescriptorByName(provider, svc)
        then:
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
}
