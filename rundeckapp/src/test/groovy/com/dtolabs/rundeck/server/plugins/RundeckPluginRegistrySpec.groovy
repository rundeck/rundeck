package com.dtolabs.rundeck.server.plugins


import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.common.PropertyRetriever
import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.plugins.*
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.ServiceTypes
import com.dtolabs.rundeck.plugins.config.ConfiguredBy
import com.dtolabs.rundeck.plugins.config.Group
import com.dtolabs.rundeck.plugins.config.PluginGroup
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import org.rundeck.security.RundeckPluginBlocklist
import org.springframework.context.ApplicationContext
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

    @Plugin(service = "PluginGroup", name = 'testgroup')
    static class TestGroup implements PluginGroup{
        @PluginProperty
        String groupVal

    }
    @Plugin(service = "TestService", name = 'testprov1')
    @Group(TestGroup)
    static class TestPluginWithGroup implements ConfiguredBy<TestGroup> {
        TestGroup pluginGroup
        @PluginProperty
        String prop1
    }

    @Unroll
    def "configure plugin by name with group"() {
        given:

            def sut = new RundeckPluginRegistry()
            sut.pluginRegistryMap=[:]
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            def fwk = Mock(IFramework) {
                getFrameworkProjectMgr() >> Mock(ProjectManager) {
                    getFrameworkProject(project) >> Mock(IRundeckProject) {
                        getProperties() >> projProps
                    }
                }
                getPropertyRetriever() >> new mapRetriever(fwkProps)

            }
            def svc = Mock(PluggableProviderService) {
                _*getName() >> 'TestService'
                _*providerOfType('testprov1') >> { new TestPluginWithGroup() }

            }
            def grpSvc = Mock(PluggableProviderService) {
                _*getName() >> 'PluginGroup'
                _*providerOfType('testgroup') >> { new TestGroup() }
            }
            sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader){
                createPluginService(PluginGroup, 'PluginGroup') >> grpSvc
            }
            sut.rundeckPluginBlocklist=Mock(RundeckPluginBlocklist)
        when:
            def result = sut.configurePluginByName('testprov1', svc, fwk, project, [:])
        then:
            result
            result.instance instanceof TestPluginWithGroup
            TestPluginWithGroup plugin = result.instance
            plugin.prop1==propval
            plugin.pluginGroup
            plugin.pluginGroup.groupVal==groupVal


        where:
            project='AProject'
            projProps | fwkProps | propval | groupVal
            [:] | [:] | null | null
            [:] | ['framework.plugin.TestService.testprov1.prop1': 'prop1'] | 'prop1' | null
            ['project.plugin.TestService.testprov1.prop1': 'propval'] | [:] | 'propval' | null
            ['project.plugin.TestService.testprov1.prop1': 'propval'] | ['framework.plugin.TestService.testprov1.prop1': 'prop1'] | 'propval' | null

            ['project.plugin.PluginGroup.testgroup.groupVal': 'projval2'] | [:] | null | 'projval2'
            [:] | ['framework.plugin.PluginGroup.testgroup.groupVal': 'fwkval2'] | null | 'fwkval2'
            ['project.plugin.PluginGroup.testgroup.groupVal': 'projval2'] | ['framework.plugin.PluginGroup.testgroup.groupVal': 'fwkval2'] | null | 'projval2'

    }

    @Unroll
    def "load plugin by name, plugin blocklisted"() {
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
        def beanBuilder3 = new TestBuilder3(instance: testPlugin3)

        defineBeans {
            testBeanBuilder(InstanceFactoryBean, beanBuilder1)
            testBeanBuilder3(InstanceFactoryBean, beanBuilder3)
        }
        List<String> list = ["plugin2"]
        def map1 = ["NodeExecutor":list]
        List<Map> mapList = [map1]
        def map = ["providerNameEntries":mapList]
        def sut = new RundeckPluginRegistry()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.pluginRegistryMap = ['plugin1': 'testBeanBuilder', 'otherservice:plugin1': 'testBeanBuilder3']
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)
        sut.rundeckPluginBlocklist = Mock(RundeckPluginBlocklist){
            1 * isPluginProviderPresent('NodeExecutor','plugin2') >> true
        }
        def svc = Mock(PluggableProviderService){
            getName() >> "NodeExecutor"
        }

        when:
        def result = sut.loadPluginDescriptorByName("plugin2", svc)

        then:
        result == null

    }

    def "list plugin, one blocklisted"(){
        given:
        def description1 = DescriptionBuilder.builder()
                .name('plugin1')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def testPlugin1 = new TestPluginWithAnnotation()
        testPlugin1.description = description1

        def description2 = DescriptionBuilder.builder()
                .name('plugin3')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def description3 = DescriptionBuilder.builder()
                .name('plugin4')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def testPlugin2 = new TestPluginWithAnnotation2()
        testPlugin2.description = description2

        def testPlugin3 = new TestPluginWithAnnotation2()
        testPlugin3.description = description3

        def beanBuilder1 = new TestBuilder2(instance: testPlugin1)
        def beanBuilder2 = new TestBuilder3(instance: testPlugin2)
        def beanBuilder3 = new TestBuilder3(instance: testPlugin3)

        defineBeans {
            testBeanBuilder(InstanceFactoryBean, beanBuilder1)
            testBeanBuilder2(InstanceFactoryBean, beanBuilder2)
            testBeanBuilder3(InstanceFactoryBean, beanBuilder3)
        }
        List<String> list = ["plugin2"]
        def map1 = ["NodeExecutor":list]
        List<Map> mapList = [map1]
        def map = ["providerNameEntries":mapList]
        def sut = new RundeckPluginRegistry()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.pluginRegistryMap = ['aservicename:plugin1': 'testBeanBuilder', 'otherservice:plugin2': 'testBeanBuilder2', 'otherservice:plugin3': 'testBeanBuilder3']
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)
        FileReader reader = Mock(FileReader)
        sut.rundeckPluginBlocklist = Mock(RundeckPluginBlocklist){
            1 * isPluginProviderPresent(_,"plugin2") >> false
            1 * isPluginProviderPresent(_,"plugin1") >> true
            1 * isPluginProviderPresent(_,"plugin3") >> false
        }
        def svc = Mock(PluggableProviderService){
            getName() >> "NodeExecutor"
        }

        when:
        def result = sut.listPluginDescriptors(TestPluginWithAnnotation2, svc)

        then:
        result.size() == 2
        result["plugin3"].description == description3

    }

    def "retainPluginDescriptorByName, plugin blocklisted"(){
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
        def beanBuilder3 = new TestBuilder3(instance: testPlugin3)

        defineBeans {
            testBeanBuilder(InstanceFactoryBean, beanBuilder1)
            testBeanBuilder3(InstanceFactoryBean, beanBuilder3)
        }
        def sut = new RundeckPluginRegistry()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.pluginRegistryMap = ['plugin1': 'testBeanBuilder', 'otherservice:plugin1': 'testBeanBuilder3']
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)
        sut.rundeckPluginBlocklist = Mock(RundeckPluginBlocklist){
            1 * isPluginProviderPresent('NodeExecutor','plugin2') >> true
        }
        def svc = Mock(PluggableProviderService){
            getName() >> "NodeExecutor"
        }

        when:
        def result = sut.retainPluginDescriptorByName("plugin2", svc)

        then:
        result == null

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
        def beanBuilder3 = new TestBuilder3(instance: testPlugin3)

        defineBeans {
            testBeanBuilder(InstanceFactoryBean, beanBuilder1)
            testBeanBuilder3(InstanceFactoryBean, beanBuilder3)
        }
        def sut = new RundeckPluginRegistry()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.pluginRegistryMap = ['plugin1': 'testBeanBuilder', 'otherservice:plugin1': 'testBeanBuilder3']
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)
        sut.rundeckPluginBlocklist = Mock(RundeckPluginBlocklist)

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
    def "loadBeanDescriptor not describable"() {
        given:

        defineBeans{
            SomeBean(InstanceFactoryBean, 'test')
        }
        def sut = new RundeckPluginRegistry()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.pluginRegistryMap = [(pluginname):'SomeBean']
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)


        when:
        def result = sut.loadBeanDescriptor(pluginname, servicename)

        then:
        result==null

        where:

        pluginname | servicename
        'plugin1'  | 'aservicename'

    }
    @Unroll
    def "loadBeanDescriptor builder returns null"() {
        given:

            def builder = Mock(PluginBuilder){

            }
        defineBeans{
            SomeBean(InstanceFactoryBean, builder)
        }
        def sut = new RundeckPluginRegistry()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.pluginRegistryMap = [(pluginname):'SomeBean']
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)


        when:
        def result = sut.loadBeanDescriptor(pluginname, servicename)

        then:
        result==null

        where:

        pluginname | servicename
        'plugin1'  | 'aservicename'

    }
    @Unroll
    def "loadBeanDescriptor null from subcontext"() {
        given:

        def sut = new RundeckPluginRegistry()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.pluginRegistryMap = [(pluginname):'SomeBean']
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)
        sut.subContexts['SomeBean']=Mock(ApplicationContext){
            getBean('SomeBean')>>null
        }


        when:
        def result = sut.loadBeanDescriptor(pluginname, servicename)

        then:
        result==null

        where:

        pluginname | servicename
        'plugin1'  | 'aservicename'

    }

    @Unroll
    def "load plugin by name should load description by type"() {
        given:

        def description2 = DescriptionBuilder.builder()
                .name('plugin2')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def testPlugin2 = new TestPluginWithAnnotation()
        testPlugin2.description = description2

        def sut = new RundeckPluginRegistry()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.pluginRegistryMap = [:]
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)

        def svc = Mock(PluggableProviderService) {
            getName() >> "otherservicename"
            providerOfType("plugin2") >> testPlugin2
        }

        when:
        def result = sut.loadPluginDescription(svc, 'plugin2')

        then:

        result
        result.name == 'plugin2'
        result == description2
    }

    @Unroll
    def "list plugin by type"() {
        given:
        def description1 = DescriptionBuilder.builder()
                .name('plugin1')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def testPlugin1 = new TestPluginWithAnnotation()
        testPlugin1.description = description1

        def description2 = DescriptionBuilder.builder()
                .name('plugin3')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def description3 = DescriptionBuilder.builder()
                .name('plugin4')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def testPlugin2 = new TestPluginWithAnnotation2()
        testPlugin2.description = description2

        def testPlugin3 = new TestPluginWithAnnotation2()
        testPlugin3.description = description3

        def beanBuilder1 = new TestBuilder2(instance: testPlugin1)
        def beanBuilder2 = new TestBuilder3(instance: testPlugin2)
        def beanBuilder3 = new TestBuilder3(instance: testPlugin3)

        defineBeans {
            testBeanBuilder(InstanceFactoryBean, beanBuilder1)
            testBeanBuilder2(InstanceFactoryBean, beanBuilder2)
            testBeanBuilder3(InstanceFactoryBean, beanBuilder3)
        }
        def sut = new RundeckPluginRegistry()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.pluginRegistryMap = ['aservicename:plugin1': 'testBeanBuilder', 'otherservice:plugin2': 'testBeanBuilder2', 'otherservice:plugin3': 'testBeanBuilder3']
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)
        sut.rundeckPluginBlocklist = Mock(RundeckPluginBlocklist)

        def svc = Mock(PluggableProviderService)

        when:
        def result = sut.listPluginDescriptors(TestPluginWithAnnotation2, svc)

        then:
        result
        result.size() == 2
        result["plugin2"].description == description2
        result["plugin3"].description == description3

    }

    @Unroll
    def "get plugin metadata"() {
        given:
        def description1 = DescriptionBuilder.builder()
                .name('plugin1')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def testPlugin1 = new TestPluginMetaData()
        testPlugin1.description = description1

        def description2 = DescriptionBuilder.builder()
                .name('plugin3')
                .property(PropertyBuilder.builder().string('prop1').build())
                .property(PropertyBuilder.builder().string('prop2').build())
                .build()

        def testPlugin2 = new TestPluginMetaData()
        testPlugin2.description = description2

        def beanBuilder1 = new TestBuilderMetadata2(instance: testPlugin1)
        def beanBuilder2 = new TestBuilderMetadata2(instance: testPlugin2)

        defineBeans {
            testBeanBuilder(InstanceFactoryBean, beanBuilder1)
            testBeanBuilder2(InstanceFactoryBean, beanBuilder2)
        }
        GroovyMock(ServiceTypes, global: true)
        ServiceTypes.getPluginType(_) >> TestPluginMetaData
        def sut = new RundeckPluginRegistry()
        sut.pluginDirectory = File.createTempDir('test', 'dir')
        sut.applicationContext = applicationContext
        sut.pluginRegistryMap = ['plugin1': 'testBeanBuilder', 'otherservice:plugin2': 'testBeanBuilder2']
        sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)

        when:
        def result = sut.getPluginMetadata('otherservice',providerName)

        then:
        result
        result instanceof TestBuilderMetadata2

        where:
        providerName            | instanceClass
        'plugin1'               | TestBuilderMetadata2
        'plugin2'               | TestBuilderMetadata2
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
    @Unroll
    def "validate plugin by name instance data"() {
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

            def svc = Mock(PluggableProviderService) {
                getName() >> 'TestSvc'
            }
        when:
            def result = sut.validatePluginByName('aplugin', svc, iprops)
        then:
            result
            result.valid==valid

        where:
            iprops            |valid
            ['prop1': 'ival'] |true
            [:]               |false
            [prop2:'other']               |false
            provider = 'aplugin'
    }
    @Unroll
    def "validate plugin by name instance data with ignored scope"() {
        given:
            def description = DescriptionBuilder
                    .builder()
                    .name(provider)
                    .property(PropertyBuilder.builder().string('prop1').required(true).scope(PropertyScope.Instance).build())
                    .property(PropertyBuilder.builder().string('prop2').required(true).scope(PropertyScope.Project).build())
                    .property(PropertyBuilder.builder().string('prop3').required(true).scope(PropertyScope.Framework).build())
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

            def svc = Mock(PluggableProviderService) {
                getName() >> 'TestSvc'
            }
            def ignoredScope =ignored?PropertyScope.valueOf(ignored):null
        when:
            def result = sut.validatePluginByName('aplugin', svc, iprops, ignoredScope)
        then:
            result
            result.valid==valid

        where:
            iprops                             | ignored     | valid
            [:]                                | null        | false
            [:]                                | 'Framework' | false
            [:]                                | 'Project'   | false
            [:]                                | 'Instance'  | true
            ['prop1': 'ival']                  | null        | false
            ['prop1': 'ival']                  | 'Framework' | false
            ['prop1': 'ival', 'prop2': 'asdf'] | 'Framework' | false //can't supply project property via instance input
            ['prop1': 'ival']                  | 'Project'   | true
            [prop2: 'other']                   | null        | false
            [prop2: 'other']                   | 'Project'   | false
            provider = 'aplugin'
    }

    @Unroll
    def "getResourceLoader for UI plugin that implements PluginResourceLoader"() {
        setup:
        RundeckPluginRegistry registry = new RundeckPluginRegistry()
        registry.pluginRegistryMap = ["UI:MyUiPlugin":"myuipluginBean","UI:MyUiPluginWResLoader":"myresloaderBean"]
        registry.metaClass.findBean = { String beanName ->
            if(beanName == "myuipluginBean") return new MyUiPlugin()
            else if(beanName == "myresloaderBean") return new MyUiPluginWResLoader()
        }
        registry.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader) {
            getResourceLoader(_,_) >> new StandardPluginResourceLoader()
        }

        when:
        def result = registry.getResourceLoader("UI",plugin)


        then:
        result.getClass() == expected

        where:
        plugin                      | expected
        "notexist"                  | StandardPluginResourceLoader
        "MyUiPlugin"                | StandardPluginResourceLoader
        "MyUiPluginWResLoader"      | MyUiPluginWResLoader

    }


    def "Test UI plugin bean registry and resource loading."() {
        setup:

        RundeckPluginRegistry registry = new RundeckPluginRegistry()
        registry.pluginRegistryMap = new HashMap()
        registry.metaClass.findBean = { String beanName ->
            if(beanName == "testUIBean") return new MyUiPluginWResLoader() {
                @Override
                InputStream openResourceStreamFor(String name) throws PluginException, IOException {
                    return new ByteArrayInputStream(name.getBytes());
                }
            }
            else return null
        }

        when:
        registry.registerPlugin(ServiceNameConstants.UI, "test-resource-ui-plugin", "testUIBean")

        def result = registry.getResourceLoader(ServiceNameConstants.UI, "test-resource-ui-plugin")

        def name = "Test Resource Name"
        def stream = result.openResourceStreamFor(name)
        def deserializedName = new InputStreamReader(stream).readLine()

        then:
        result instanceof UIPlugin
        result instanceof PluginResourceLoader
        result instanceof MyUiPluginWResLoader
        name == deserializedName
    }



    def "Ask for a resource on a plugin without resource loader gives null, not exception."() {
        setup:

        RundeckPluginRegistry registry = new RundeckPluginRegistry()
        registry.pluginRegistryMap = new HashMap()
        registry.metaClass.findBean = { String beanName ->
            if (beanName == "testUIBean") return new MyUiPlugin()
            else return null
        }
        registry.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader) {
            getResourceLoader(_,_) >> null
        }


        when:
        registry.registerPlugin(ServiceNameConstants.UI, "test-resource-ui-plugin", "testUIBean")

        def result = registry.getResourceLoader(ServiceNameConstants.UI, "test-resource-ui-plugin")

        then:
        result == null
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
    static class TestPluginMetaData implements PluginMetadata {
        Description description

        @Override
        String getFilename() {
            return null
        }

        @Override
        File getFile() {
            return null
        }

        @Override
        String getPluginArtifactName() {
            return null
        }

        @Override
        String getPluginAuthor() {
            return null
        }

        @Override
        String getPluginFileVersion() {
            return null
        }

        @Override
        String getPluginVersion() {
            return null
        }

        @Override
        String getPluginUrl() {
            return null
        }

        @Override
        Date getPluginDate() {
            return null
        }

        @Override
        Date getDateLoaded() {
            return null
        }

        @Override
        String getPluginName() {
            return null
        }

        @Override
        String getPluginDescription() {
            return null
        }

        @Override
        String getPluginId() {
            return null
        }

        @Override
        String getRundeckCompatibilityVersion() {
            return null
        }

        @Override
        String getTargetHostCompatibility() {
            return null
        }

        @Override
        List<String> getTags() {
            return null
        }

        @Override
        String getPluginLicense() {
            return null
        }

        @Override
        String getPluginThirdPartyDependencies() {
            return null
        }

        @Override
        String getPluginSourceLink() {
            return null
        }

        @Override
        String getPluginDocsLink() {
            return null
        }

        @Override
        String getPluginType() {
            return null
        }
    }
    static class TestPluginWithAnnotation2 implements Configurable, Describable {
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

    static class TestBuilderMetadata2 implements PluginBuilder<TestPluginMetaData>, PluginMetadata {
        TestPluginMetaData instance

        @Override
        TestPluginMetaData buildPlugin() {
            return instance
        }


        @Override
        Class<TestPluginMetaData> getPluginClass() {
            TestPluginMetaData
        }

        @Override
        String getFilename() {
            return null
        }

        @Override
        File getFile() {
            return null
        }

        @Override
        String getPluginArtifactName() {
            return null
        }

        @Override
        String getPluginAuthor() {
            return null
        }

        @Override
        String getPluginFileVersion() {
            return null
        }

        @Override
        String getPluginVersion() {
            return null
        }

        @Override
        String getPluginUrl() {
            return null
        }

        @Override
        Date getPluginDate() {
            return null
        }

        @Override
        Date getDateLoaded() {
            return null
        }

        @Override
        String getPluginName() {
            return null
        }

        @Override
        String getPluginDescription() {
            return null
        }

        @Override
        String getPluginId() {
            return null
        }

        @Override
        String getRundeckCompatibilityVersion() {
            return null
        }

        @Override
        String getTargetHostCompatibility() {
            return null
        }

        @Override
        List<String> getTags() {
            return null
        }

        @Override
        String getPluginLicense() {
            return null
        }

        @Override
        String getPluginThirdPartyDependencies() {
            return null
        }

        @Override
        String getPluginSourceLink() {
            return null
        }

        @Override
        String getPluginDocsLink() {
            return null
        }

        @Override
        String getPluginType() {
            return null
        }
    }

    static class TestBuilder3 implements PluginBuilder<TestPluginWithAnnotation2> {
        TestPluginWithAnnotation2 instance

        @Override
        TestPluginWithAnnotation2 buildPlugin() {
            return instance
        }


        @Override
        Class<TestPluginWithAnnotation2> getPluginClass() {
            TestPluginWithAnnotation2
        }
    }

    static class StandardPluginResourceLoader implements PluginResourceLoader {
        List<String> listResources() throws PluginException, IOException { return null }
        InputStream openResourceStreamFor(final String name) throws PluginException, IOException { return null }
    }

    static class MyUiPlugin implements UIPlugin {

        @Override
        boolean doesApply(final String path) {
            return false
        }

        @Override
        List<String> resourcesForPath(final String path) {
            return null
        }

        @Override
        List<String> scriptResourcesForPath(final String path) {
            return null
        }

        @Override
        List<String> styleResourcesForPath(final String path) {
            return null
        }

        @Override
        List<String> requires(final String path) {
            return null
        }
    }

    static class MyUiPluginWResLoader implements UIPlugin, PluginResourceLoader {

        @Override
        List<String> listResources() throws PluginException, IOException {
            return null
        }

        @Override
        InputStream openResourceStreamFor(final String name) throws PluginException, IOException {
            return null
        }

        @Override
        boolean doesApply(final String path) {
            return false
        }

        @Override
        List<String> resourcesForPath(final String path) {
            return null
        }

        @Override
        List<String> scriptResourcesForPath(final String path) {
            return null
        }

        @Override
        List<String> styleResourcesForPath(final String path) {
            return null
        }

        @Override
        List<String> requires(final String path) {
            return null
        }
    }
}
