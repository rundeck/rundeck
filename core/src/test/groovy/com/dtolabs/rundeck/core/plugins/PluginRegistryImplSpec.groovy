package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.common.PropertyRetriever
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.ServiceTypes
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import spock.lang.Specification
import spock.lang.Unroll

class PluginRegistryImplSpec extends Specification {


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
            def sut = new PluginRegistryImpl()
            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * loadPluginDescriptorByName('aplugin', _) >> new DescribedPlugin<>(
                        testPlugin,
                        description,
                        'aplugin'
                    )
                }
            ]


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
    def "load plugin by name, plugin blocklisted"() {
        given:
            def description1 = DescriptionBuilder.builder()
                                                 .name('plugin1')
                                                 .property(PropertyBuilder.builder().string('prop1').build())
                                                 .property(PropertyBuilder.builder().string('prop2').build())
                                                 .build()

            def testPlugin1 = new TestPluginWithAnnotation()
            testPlugin1.description = description1


            def sut = new PluginRegistryImpl()

            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * loadPluginDescriptorByName('plugin2', _) >> {
                        throw new Exception("plugin blocked")
                    }
                },
                Mock(PluginRegistryComponent) {
                    1 * loadPluginDescriptorByName('plugin2', _) >> new DescribedPlugin<>(
                        testPlugin1,
                        description,
                        'plugin2'
                    )
                }
            ]
            def svc = Mock(PluggableProviderService) {
                getName() >> "NodeExecutor"
            }

        when:
            def result = sut.loadPluginDescriptorByName("plugin2", svc)

        then:
            result == null

    }
    @Unroll
    def "retain plugin by name, plugin blocklisted"() {
        given:
            def description1 = DescriptionBuilder.builder()
                                                 .name('plugin1')
                                                 .property(PropertyBuilder.builder().string('prop1').build())
                                                 .property(PropertyBuilder.builder().string('prop2').build())
                                                 .build()

            def testPlugin1 = new TestPluginWithAnnotation()
            testPlugin1.description = description1


            def sut = new PluginRegistryImpl()

            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * loadPluginDescriptorByName('plugin2', _) >> {
                        throw new Exception("plugin blocked")
                    }
                },
                Mock(PluginRegistryComponent) {
                    1 * loadPluginDescriptorByName('plugin2', _) >> new DescribedPlugin<>(
                        testPlugin1,
                        description,
                        'plugin2'
                    )
                }
            ]
            def svc = Mock(PluggableProviderService) {
                getName() >> "NodeExecutor"
            }

        when:
            def result = sut.retainPluginDescriptorByName("plugin2", svc)

        then:
            result == null

    }

    def "list plugin, one blocklisted"() {
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

            def sut = new PluginRegistryImpl()

            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * isAllowed("plugin2", _) >> true
                    1 * isAllowed("plugin1", _) >> false
                    1 * isAllowed("plugin3", _) >> true
                },
                Mock(PluginRegistryComponent) {
                    1 * listPluginDescriptors(_, _) >> [
                        plugin1: testPlugin1,
                        plugin2: testPlugin2
                    ]
                    1 * isAllowed(_, _) >> true
                },
                Mock(PluginRegistryComponent) {
                    1 * listPluginDescriptors(_, _) >> [
                        plugin3: testPlugin3
                    ]
                    1 * isAllowed(_, _) >> true
                }
            ]
            def svc = Mock(PluggableProviderService) {
                getName() >> "NodeExecutor"
            }

        when:
            def result = sut.listPluginDescriptors(TestPluginWithAnnotation2, svc)

        then:
            result.size() == 2
            result["plugin3"].description == description3
            result["plugin2"].description == description2

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
            def sut = new PluginRegistryImpl()
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

            pluginname | servicename | pluginKey
            'plugin1' | 'aservicename' | 'plugin1'
            'plugin2' | 'otherservicename' | 'plugin2'
            'plugin1' | 'otherservice' | 'plugin3'

    }

    @Unroll
    def "loadBeanDescriptor not describable"() {
        given:

            defineBeans {
                SomeBean(InstanceFactoryBean, 'test')
            }
            def sut = new PluginRegistryImpl()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut.pluginRegistryMap = [(pluginname): 'SomeBean']
            sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)


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
            def sut = new PluginRegistryImpl()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut.pluginRegistryMap = [(pluginname): 'SomeBean']
            sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)


        when:
            def result = sut.loadBeanDescriptor(pluginname, servicename)

        then:
            result == null

        where:

            pluginname | servicename
            'plugin1'  | 'aservicename'

    }

    @Unroll
    def "loadBeanDescriptor null from subcontext"() {
        given:

            def sut = new PluginRegistryImpl()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut.pluginRegistryMap = [(pluginname): 'SomeBean']
            sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)
            sut.subContexts['SomeBean'] = Mock(ApplicationContext) {
                getBean('SomeBean') >> null
            }


        when:
            def result = sut.loadBeanDescriptor(pluginname, servicename)

        then:
            result == null

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

            def sut = new PluginRegistryImpl()
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
            def sut = new PluginRegistryImpl()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut
                .pluginRegistryMap = ['aservicename:plugin1': 'testBeanBuilder', 'otherservice:plugin2':
                'testBeanBuilder2', 'otherservice:plugin3'  : 'testBeanBuilder3']
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
            def sut = new PluginRegistryImpl()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut.pluginRegistryMap = ['plugin1': 'testBeanBuilder', 'otherservice:plugin2': 'testBeanBuilder2']
            sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)

        when:
            def result = sut.getPluginMetadata('otherservice', providerName)

        then:
            result
            result instanceof TestBuilderMetadata2

        where:
            providerName | instanceClass
            'plugin1'    | TestBuilderMetadata2
            'plugin2'    | TestBuilderMetadata2
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
            def sut = new PluginRegistryImpl()
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
            [:]               | [:]                                                 |
            ['framework.plugin.TestSvc.aplugin.prop1': 'fwkval']
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
            def sut = new PluginRegistryImpl()
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
            result.valid == valid

        where:
            iprops            | valid
            ['prop1': 'ival'] | true
            [:]               | false
            [prop2: 'other']  | false
            provider = 'aplugin'
    }

    @Unroll
    def "validate plugin by name instance data with ignored scope"() {
        given:
            def description = DescriptionBuilder
                .builder()
                .name(provider)
                .property(
                    PropertyBuilder.builder().string('prop1').required(true).scope(PropertyScope.Instance).build()
                )
                .property(PropertyBuilder.builder().string('prop2').required(true).scope(PropertyScope.Project).build())
                .property(
                    PropertyBuilder.builder().string('prop3').required(true).scope(PropertyScope.Framework).build()
                )
                .build()
            def testPlugin = new TestPlugin2()
            testPlugin.description = description
            def beanBuilder = new TestBuilder(instance: testPlugin)
            defineBeans {
                testBeanBuilder(InstanceFactoryBean, beanBuilder)
            }
            def sut = new PluginRegistryImpl()
            sut.pluginDirectory = File.createTempDir('test', 'dir')
            sut.applicationContext = applicationContext
            sut.pluginRegistryMap = ['aplugin': 'testBeanBuilder']
            sut.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader)

            def svc = Mock(PluggableProviderService) {
                getName() >> 'TestSvc'
            }
            def ignoredScope = ignored ? PropertyScope.valueOf(ignored) : null
        when:
            def result = sut.validatePluginByName('aplugin', svc, iprops, ignoredScope)
        then:
            result
            result.valid == valid

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
            PluginRegistryImpl registry = new PluginRegistryImpl()
            registry
                .pluginRegistryMap = ["UI:MyUiPlugin": "myuipluginBean", "UI:MyUiPluginWResLoader": "myresloaderBean"]
            registry.metaClass.findBean = { String beanName ->
                if (beanName == "myuipluginBean") {
                    return new MyUiPlugin()
                } else if (beanName == "myresloaderBean") {
                    return new MyUiPluginWResLoader()
                }
            }
            registry.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader) {
                getResourceLoader(_, _) >> new StandardPluginResourceLoader()
            }

        when:
            def result = registry.getResourceLoader("UI", plugin)


        then:
            result.getClass() == expected

        where:
            plugin                 | expected
            "notexist"             | StandardPluginResourceLoader
            "MyUiPlugin"           | StandardPluginResourceLoader
            "MyUiPluginWResLoader" | MyUiPluginWResLoader

    }


    def "Test UI plugin bean registry and resource loading."() {
        setup:

            PluginRegistryImpl registry = new PluginRegistryImpl()
            registry.pluginRegistryMap = new HashMap()
            registry.metaClass.findBean = { String beanName ->
                if (beanName == "testUIBean") {
                    return new MyUiPluginWResLoader() {
                        @Override
                        InputStream openResourceStreamFor(String name) throws PluginException, IOException {
                            return new ByteArrayInputStream(name.getBytes());
                        }
                    }
                } else {
                    return null
                }
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

            PluginRegistryImpl registry = new PluginRegistryImpl()
            registry.pluginRegistryMap = new HashMap()
            registry.metaClass.findBean = { String beanName ->
                if (beanName == "testUIBean") {
                    return new MyUiPlugin()
                } else {
                    return null
                }
            }
            registry.rundeckServerServiceProviderLoader = Mock(ServiceProviderLoader) {
                getResourceLoader(_, _) >> null
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
