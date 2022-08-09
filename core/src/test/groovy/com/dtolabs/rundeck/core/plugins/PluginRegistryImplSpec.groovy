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
                    _ * isAllowed(*_) >> true
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
            def result = sut.configurePluginByName('aplugin', svc, fwk, 'AProject', iconfig)
        then:
            result
            result.configuration != null
            result.configuration[query] == value

        where:
            provider = 'aplugin'
            project = 'AProject'

            iconfig|projProps | fwkProps | query | value
            [:]| [:] | [:] | 'prop1' | null
            [prop1:'aval']| [:] | [:] | 'prop1' | 'aval'
            [:]|['project.plugin.TestSvc.aplugin.prop1': 'propval'] | [:] | 'prop1' | 'propval'
            [:]|[:] | ['framework.plugin.TestSvc.aplugin.prop1': 'fwkval'] | 'prop1' | 'fwkval'
            [:]|['project.plugin.TestSvc.aplugin.prop1': 'propval'] | ['framework.plugin.TestSvc.aplugin.prop1': 'fwkval'] | 'prop1' | 'propval'
            [prop1:'aval']|['project.plugin.TestSvc.aplugin.prop1': 'propval'] | ['framework.plugin.TestSvc.aplugin.prop1': 'fwkval'] | 'prop1' | 'aval'
            [prop1:'aval']|[:] | ['framework.plugin.TestSvc.aplugin.prop1': 'fwkval'] | 'prop1' | 'aval'
            [prop1:'aval']|['project.plugin.TestSvc.aplugin.prop1': 'propval'] | [:] | 'prop1' | 'aval'

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


            def sut = new PluginRegistryImpl()

            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * isAllowed('plugin2', _) >> false
                },
                Mock(PluginRegistryComponent) {
                    0 * loadPluginDescriptorByName('plugin2', _) >> new DescribedPlugin<>(
                        testPlugin1,
                        description1,
                        'plugin2'
                    )
                    _ * isAllowed(*_) >> true
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


            def sut = new PluginRegistryImpl()

            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * isAllowed('plugin2', _) >> false
                },
                Mock(PluginRegistryComponent) {
                    0 * loadPluginDescriptorByName('plugin2', _) >> new DescribedPlugin<>(
                        testPlugin1,
                        description1,
                        'plugin2'
                    )
                    _ * isAllowed(*_) >> true
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



            def sut = new PluginRegistryImpl()

            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * isAllowed("plugin2", _) >> true
                    1 * isAllowed("plugin1", _) >> false
                    1 * isAllowed("plugin3", _) >> true
                    1 * listPluginDescriptors(TestPluginWithAnnotation2, _)>>[:]
                },
                Mock(PluginRegistryComponent) {
                    1 * listPluginDescriptors(TestPluginWithAnnotation2, _) >> new HashMap<>([
                        plugin1: new DescribedPlugin(testPlugin1,description1,'plugin1'),
                        plugin2: new DescribedPlugin(testPlugin2,description2,'plugin2')
                    ])
                    _ * isAllowed(_, _) >> true
                },
                Mock(PluginRegistryComponent) {

                    1 * listPluginDescriptors(TestPluginWithAnnotation2, _) >> new HashMap<>([
                        plugin3: new DescribedPlugin(testPlugin3,description3,'plugin3')
                    ])
                    _ * isAllowed(_, _) >> true
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
                                                 .name('plugin3')
                                                 .property(PropertyBuilder.builder().string('prop1').build())
                                                 .property(PropertyBuilder.builder().string('prop2').build())
                                                 .build()

            def testPlugin3 = new TestPluginWithAnnotation2()
            testPlugin3.description = description3

            Map pluginsMap = [:]
            pluginsMap[testPlugin1.description.name] = testPlugin1
            pluginsMap[testPlugin2.description.name] = testPlugin2
            pluginsMap['plugin3'] = testPlugin3


            def sut = new PluginRegistryImpl()

            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * isAllowed(pluginname, _) >> true
                },
                Mock(PluginRegistryComponent) {
                    _ * loadPluginDescriptorByName('plugin1', _) >> new DescribedPlugin(testPlugin1,description1,'plugin1')
                    _ * loadPluginDescriptorByName('plugin2', _) >> new DescribedPlugin(testPlugin2,description2,'plugin2')
                    _ * isAllowed(pluginname, _) >> true
                },
                Mock(PluginRegistryComponent) {
                    _ * loadPluginDescriptorByName('plugin3', _) >> new DescribedPlugin(testPlugin3,description3,'plugin3')

                    _ * isAllowed(pluginname, _) >> true
                }
            ]
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
            project = 'AProject'

            pluginname | servicename | pluginKey
            'plugin1' | 'aservicename' | 'plugin1'
            'plugin2' | 'otherservicename' | 'plugin2'
            'plugin3' | 'otherservice' | 'plugin3'

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
                                                 .name('plugin2')
                                                 .property(PropertyBuilder.builder().string('prop1').build())
                                                 .property(PropertyBuilder.builder().string('prop2').build())
                                                 .build()

            def description3 = DescriptionBuilder.builder()
                                                 .name('plugin3')
                                                 .property(PropertyBuilder.builder().string('prop1').build())
                                                 .property(PropertyBuilder.builder().string('prop2').build())
                                                 .build()

            def testPlugin2 = new TestPluginWithAnnotation2()
            testPlugin2.description = description2

            def testPlugin3 = new TestPluginWithAnnotation2()
            testPlugin3.description = description3

            def sut = new PluginRegistryImpl()
            sut.components = [
                Mock(PluginRegistryComponent) {
                    _ * listPluginDescriptors(TestPluginWithAnnotation, _) >> [
                        plugin1: new DescribedPlugin(testPlugin1, description1, 'plugin1'),
                    ]
                    _ * listPluginDescriptors(TestPluginWithAnnotation2, _) >> [
                        plugin2: new DescribedPlugin(testPlugin2, description2, 'plugin2'),
                    ]
                    _ * isAllowed(*_) >> true
                },
                Mock(PluginRegistryComponent) {
                    _ * listPluginDescriptors(TestPluginWithAnnotation2, _) >> [
                        plugin3: new DescribedPlugin(testPlugin3, description3, 'plugin3')
                    ]
                    _ * isAllowed(*_) >> true
                },
            ]

            def svc = Mock(PluggableProviderService)

        when:
            def result = sut.listPluginDescriptors(type, svc)

        then:
            result
            result.size() == names.size()
            names.containsAll result.keySet()
        where:
            type                      | names
            TestPluginWithAnnotation2 | ['plugin2', 'plugin3']
            TestPluginWithAnnotation  | ['plugin1']

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


            GroovyMock(ServiceTypes, global: true)
            ServiceTypes.getPluginType(_) >> TestPluginMetaData
            def sut = new PluginRegistryImpl()
            sut.components = [
                Mock(PluginRegistryComponent) {
                    _ * getPluginMetadata('otherservice', 'plugin1') >> Mock(PluginMetadata)
                    _ * isAllowed(*_) >> true
                },
                Mock(PluginRegistryComponent) {
                    _ * getPluginMetadata('otherservice', 'plugin2') >> Mock(PluginMetadata)
                    _ * isAllowed(*_) >> true
                }
            ]


        when:
            def result = sut.getPluginMetadata('otherservice', providerName)

        then:
            result
            result instanceof PluginMetadata

        where:
            providerName | _
            'plugin1'    | _
            'plugin2'    | _
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

            def sut = new PluginRegistryImpl()
            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * loadPluginDescriptorByName('aplugin', _) >> new DescribedPlugin<>(
                        testPlugin,
                        description,
                        'aplugin'
                    )
                    _ * isAllowed(*_) >> true
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

            def sut = new PluginRegistryImpl()
            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * loadPluginDescriptorByName('aplugin', _) >> new DescribedPlugin<>(
                        testPlugin,
                        description,
                        'aplugin'
                    )
                    _ * isAllowed(*_) >> true
                }
            ]

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

            def sut = new PluginRegistryImpl()
            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * loadPluginDescriptorByName('aplugin', _) >> new DescribedPlugin<>(
                        testPlugin,
                        description,
                        'aplugin'
                    )
                    _ * isAllowed(*_) >> true
                }
            ]

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




    def "Ask for a resource on a plugin without resource loader gives null, not exception."() {
        setup:

            PluginRegistryImpl sut = new PluginRegistryImpl()
            sut.components = [
                Mock(PluginRegistryComponent) {
                    1 * getResourceLoader(*_)
                    _ * isAllowed(*_) >> true
                }
            ]


        when:

            def result = sut.getResourceLoader(ServiceNameConstants.UI, "test-resource-ui-plugin")

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
}
