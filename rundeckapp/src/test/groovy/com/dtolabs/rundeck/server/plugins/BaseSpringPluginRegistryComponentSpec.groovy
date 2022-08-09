package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import org.springframework.context.ApplicationContext
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.Charset

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
    static class Test2SpringPluginRegistryComponent extends BaseSpringPluginRegistryComponent {
        Map<String, String> beans = [:]
        ApplicationContext applicationContext

        @Override
        def Object findProviderBean(final String type, final String name) {
           applicationContext.getBean( beans[type + ':' + name])
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


    @Unroll
    def "loadBeanDescriptor not describable"() {
        given:

            defineBeans {
                SomeBean(InstanceFactoryBean, 'test')
            }
            def sut = new Test2SpringPluginRegistryComponent()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut.beans[servicename + ':' + pluginname] = 'SomeBean'


        when:
            def result = sut.loadBeanDescriptor(pluginname, servicename)

        then:
            result == null

        where:

            pluginname | servicename
            'plugin1'  | 'aservicename'

    }

    @Unroll
    def "loadBeanDescriptor builder returns null"() {
        given:

            def builder = Mock(PluginBuilder) {

            }
            defineBeans {
                SomeBean(InstanceFactoryBean, builder)
            }
            def sut = new Test2SpringPluginRegistryComponent()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut.beans[servicename + ':' + pluginname] = 'SomeBean'



        when:
            def result = sut.loadBeanDescriptor(pluginname, servicename)

        then:
            result == null

        where:

            pluginname | servicename
            'plugin1'  | 'aservicename'

    }

    @Unroll
    def "loadBeanDescriptor bean unmatched annotation service name"() {
        given: "bean annotation service name is TestSvc != aservicename"

            def bean = new TestPlugin3()
            defineBeans {
                SomeBean(InstanceFactoryBean, bean)
            }
            def sut = new Test2SpringPluginRegistryComponent()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut.beans[servicename + ':' + pluginname] = 'SomeBean'



        when:
            def result = sut.loadBeanDescriptor(pluginname, servicename)

        then:
            result == null

        where:

            pluginname | servicename
            'plugin1'  | 'aservicename'

    }

    def "get resource loader"(){
        given:
        def servicename='ASvc'
        def bean = new TestPlugin3()
        def bean2 = new TestPluginLoader()
        defineBeans {
            SomeBean(InstanceFactoryBean, bean)
            SomeBean2(InstanceFactoryBean, bean2)
        }
        def sut = new Test2SpringPluginRegistryComponent()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.beans[servicename + ':' + 'plugin1'] = 'SomeBean'
        sut.beans[servicename + ':' + 'plugin2'] = 'SomeBean2'



        when:
            def result = sut.getResourceLoader(servicename, pluginname)

        then:
            (result == null) == isnull
            (result == bean2) == !isnull

        where:

            pluginname | isnull
            'plugin1'  | true
            'plugin2'  | false

    }

    def "test resource loader"(){
        given:
            def bean2 = new TestPluginLoader(){
                @Override
                InputStream openResourceStreamFor(String name) throws PluginException, IOException {
                    return new ByteArrayInputStream(name.getBytes(Charset.forName( 'UTF-8')));
                }
            }
            defineBeans {
                testUIBean(InstanceFactoryBean, bean2)
            }
            def sut = new Test2SpringPluginRegistryComponent()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut.beans[ServiceNameConstants.UI + ':' + 'test-resource-ui-plugin'] = 'testUIBean'



        when:
            def result = sut.getResourceLoader(ServiceNameConstants.UI, "test-resource-ui-plugin")

            def stream = result.openResourceStreamFor(resname)
            def deserializedName = new InputStreamReader(stream).readLine()

        then:
            result == bean2
            resname == deserializedName
        where:

             resname = "Test Resource Name"

    }


    static class TestPluginLoader implements PluginResourceLoader {
        @Override
        List<String> listResources() throws PluginException, IOException {
            return null
        }

        @Override
        InputStream openResourceStreamFor(final String name) throws PluginException, IOException {
            return null
        }
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
