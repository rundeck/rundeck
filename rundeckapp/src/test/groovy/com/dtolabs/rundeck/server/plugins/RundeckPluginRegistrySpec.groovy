package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.common.PropertyRetriever
import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification
import spock.lang.Unroll

class RundeckPluginRegistrySpec extends Specification implements GrailsUnitTest {
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

    @Unroll
    def "configure plugin by name with different property scopes"() {
        given:
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
            def sut = new RundeckPluginRegistry()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut.pluginRegistryMap = ['aplugin': 'testBeanBuilder']
            sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)
            def fwk = Mock(IFramework) {
                getFrameworkProjectMgr() >> Mock(ProjectManager) {
                    getFrameworkProject(project) >> Mock(IRundeckProject) {
                        getProperties() >> projProps
                    }
                }
                getPropertyRetriever() >> new mapRetriever(fwkProps)

            }
            def svc = Mock(PluggableProviderService) {
                getName() >> 'TestSvc'
            }
        when:
            def result = sut.configurePluginByName('aplugin', svc, fwk, 'AProject', [:])
        then:
            result
            result.configuration != null
            result.configuration[query] == value

        where:
            provider = 'aplugin'
            project = 'AProject'

            projProps | fwkProps | query | value
            [:] | [:] | 'prop1' | null
            [:] | [:] | 'prop1' | null
            ['project.plugin.TestSvc.aplugin.prop1': 'propval'] | [:] | 'prop1' | 'propval'
            [:] | ['framework.plugin.TestSvc.aplugin.prop1': 'fwkval'] | 'prop1' | 'fwkval'

    }

    @Unroll
    def "load plugin by name"() {
        given:
        def description1 = DescriptionBuilder.builder()
                .name('plugin1')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def testPlugin1 = new TestPluginWithAnnotation()
        testPlugin1.description = description1

        def description2 = DescriptionBuilder.builder()
                .name('plugin2')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def testPlugin2 = new TestPluginWithAnnotation()
        testPlugin2.description = description2

        def description3 = DescriptionBuilder.builder()
                .name('plugin1')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def testPlugin3 = new TestPluginWithAnnotation2()
        testPlugin3.description = description3

        Map pluginsMap = [:]
        pluginsMap[testPlugin1.description.name] = testPlugin1
        pluginsMap[testPlugin2.description.name] = testPlugin2
        pluginsMap['plugin3'] = testPlugin3

        def beanBuilder1 = new TestBuilder2(instance: testPlugin1)
        def beanBuilder3 = new TestBuilder2(instance: testPlugin3)

        defineBeans {
            testBeanBuilder(InstanceFactoryBean, beanBuilder1)
            testBeanBuilder3(InstanceFactoryBean, beanBuilder3)
        }
        def sut = new RundeckPluginRegistry()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.pluginRegistryMap = ['plugin1': 'testBeanBuilder', 'otherservice:plugin1': 'testBeanBuilder3']
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)

        def svc = Mock(PluggableProviderService) {
            getName() >> servicename
            providerOfType("plugin2") >> testPlugin2
        }

        when:
        def result = sut.loadPluginDescriptorByName(pluginname, svc)

        then:
        result
        result.name == pluginname
        result.instance == pluginsMap[pluginKey]

        where:
        provider = 'aplugin'
        project = 'AProject'

        pluginname | servicename        | pluginKey
        'plugin1'  | 'aservicename'     | 'plugin1'
        'plugin2'  | 'otherservicename' | 'plugin2'
        'plugin1'  | 'otherservice'     | 'plugin3'

    }

    @Unroll
    def "validate plugin by name with different property scopes"() {
        given:
            def description = DescriptionBuilder
                    .builder()
                    .name(provider)
                    .property(PropertyBuilder.builder().string('prop1').required(true).build())
                    .build()
            def testPlugin = new TestPlugin2()
            testPlugin.description = description
            def beanBuilder = new TestBuilder(instance: testPlugin)
            defineBeans {
                testBeanBuilder(InstanceFactoryBean, beanBuilder)
            }
            def sut = new RundeckPluginRegistry()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut.pluginRegistryMap = ['aplugin': 'testBeanBuilder']
            sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)
            def fwk = Mock(IFramework) {
                getFrameworkProjectMgr() >> Mock(ProjectManager) {
                    getFrameworkProject(project) >> Mock(IRundeckProject) {
                        getProperties() >> projProps
                    }
                }
                getPropertyRetriever() >> new mapRetriever(fwkProps)

            }
            def svc = Mock(PluggableProviderService) {
                getName() >> 'TestSvc'
            }
        when:
            def result = sut.validatePluginByName('aplugin', svc, fwk, 'AProject', iprops)
        then:
            result
            result.valid

        where:
            iprops            | projProps                                           | fwkProps
            ['prop1': 'ival'] | [:]                                                 | [:]
            [:]               | ['project.plugin.TestSvc.aplugin.prop1': 'propval'] | [:]
            [:]               | [:]                                                 | ['framework.plugin.TestSvc.aplugin.prop1': 'fwkval']
            provider = 'aplugin'
            project = 'AProject'
    }

    static class mapRetriever implements PropertyRetriever {
        private Map<String, String> map;

        mapRetriever(Map<String, String> map) {
            this.map = map;
        }

        @Override
        String getProperty(String name) {
            return map.get(name);
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

    @Plugin(service = "aservicename", name = 'providername')
    static class TestPluginWithAnnotation implements Configurable, Describable {
        Properties configuration
        Description description

        @Override
        void configure(final Properties configuration) throws ConfigurationException {
            this.configuration = configuration
        }
    }

    @Plugin(service = "otherservice", name = 'providername')
    static class TestPluginWithAnnotation2 extends TestPluginWithAnnotation implements Configurable, Describable {
        Properties configuration
        Description description

        @Override
        void configure(final Properties configuration) throws ConfigurationException {
            this.configuration = configuration
        }
    }

    static class TestBuilder2 implements PluginBuilder<TestPluginWithAnnotation> {
        TestPluginWithAnnotation instance

        @Override
        TestPluginWithAnnotation buildPlugin() {
            return instance
        }


        @Override
        Class<TestPluginWithAnnotation> getPluginClass() {
            TestPluginWithAnnotation
        }
    }
}
