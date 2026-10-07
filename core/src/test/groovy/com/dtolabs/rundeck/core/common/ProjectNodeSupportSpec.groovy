/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.common

import com.dtolabs.rundeck.core.plugins.ExtPluginConfiguration
import com.dtolabs.rundeck.core.plugins.PluginConfiguration
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.core.plugins.Closeables
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.core.utils.FileUtils
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ProjectNodeSupportSpec extends Specification {
    static final String PROJECT_NAME = 'ProjectNodeSupportSpec'
    Framework framework
    FrameworkProject testProject
    File directory

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
        directory = new File(testProject.getBaseDir(), "testGetNodesMultiFile");
        FileUtils.deleteDir(directory)
        directory.mkdirs();
    }

    def cleanup() {
        if (directory.exists()) {
            FileUtils.deleteDir(directory)
        }
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }

    def "reloads after closed"() {
        given:
        Date modifiedTime = new Date()
        File varDir = File.createTempFile("ProjectNodeSupportSpec-varDir", "-tmp")
        def config = Mock(IRundeckProjectConfig) {
            getName() >> PROJECT_NAME
            getProperties() >> ([
                    'resources.source.1.type'                            : 'file',
                    'resources.source.1.config.file'                     : '/tmp/file',
                    'resources.source.1.config.requireFileExists'        : 'false',
                    'resources.source.1.config.includeServerNode'        : 'true',
                    'resources.source.1.config.generateFileAutomatically': 'false',
                    'resources.source.1.config.format'                   : 'resourcexml',
            ] as Properties)
            getConfigLastModifiedTime() >> modifiedTime
        }
        def generatorService = ResourceFormatGeneratorService.getInstanceForFramework(framework,framework)
        def sourceService = ResourceModelSourceService.getInstanceForFramework(framework,framework)

        def support = new ProjectNodeSupport(varDir, config, generatorService, sourceService)

        when:

        def result = support.getNodeSet()
        support.close()
        def result2 = support.getNodeSet()

        then:
        result != null

        result.nodeNames != null
        result.nodeNames.size() == 1

        result2 != null
        result2.nodeNames != null
        result2.nodeNames.size() == 1

    }

    def "supports factory function for source"() {

        given:
        Date modifiedTime = new Date()
        File varDir = File.createTempFile("ProjectNodeSupportSpec-varDir", "-tmp")
        def config = Mock(IRundeckProjectConfig) {
            getName() >> PROJECT_NAME
            getProperties() >> (
                [
                    'resources.source.1.type'                            : 'file',
                    'resources.source.1.config.file'                     : '/tmp/file',
                    'resources.source.1.config.requireFileExists'        : 'false',
                    'resources.source.1.config.includeServerNode'        : 'true',
                    'resources.source.1.config.generateFileAutomatically': 'false',
                    'resources.source.1.config.format'                   : 'resourcexml',
                ] as Properties
            )
            getConfigLastModifiedTime() >> modifiedTime
        }
        def generatorService = ResourceFormatGeneratorService.getInstanceForFramework(framework,framework)
        def sourceService = ResourceModelSourceService.getInstanceForFramework(framework,framework)

        def support = new ProjectNodeSupport(varDir, config, generatorService, sourceService, null)

        when:

        def result = support.getNodeSet()
        support.close()
        def result2 = support.getNodeSet()

        then:
        result != null

        result.nodeNames != null
        result.nodeNames.size() == 1

        result2 != null
        result2.nodeNames != null
        result2.nodeNames.size() == 1
    }

    def "serialize plugin config"() {
        given:
            def prefix = "test1.abc"
            List<PluginConfiguration> configs = [
                    new SimplePluginConfiguration.SimplePluginConfigurationBuilder()
                            .service('AService')
                            .provider('provider1')
                            .configuration(
                            [
                                    a: 'b',
                                    c: 'd'
                            ]
                    ).build(),

                    new SimplePluginConfiguration.SimplePluginConfigurationBuilder()
                            .service('AService')
                            .provider('provider2')
                            .configuration(
                            [
                                    x: 'y',
                                    z: 'w'
                            ]
                    ).build(),

            ]
        when:
            def result = ProjectNodeSupport.serializePluginConfigurations(prefix, configs)
        then:
            result
            result.size() == 6
            result == [
                    (prefix + '.1.type'): 'provider1',
                    (prefix + '.1.config.a'): 'b',
                    (prefix + '.1.config.c'): 'd',
                    (prefix + '.2.type'): 'provider2',
                    (prefix + '.2.config.x'): 'y',
                    (prefix + '.2.config.z'): 'w',

            ]
    }

    def "serialize plugin config with extra"() {
        given:
            def prefix = "test1.abc"
            List<ExtPluginConfiguration> configs = [
                    new SimplePluginConfiguration.SimplePluginConfigurationBuilder()
                            .service('AService')
                            .provider('provider1')
                            .extra([q: 't', r: 'v', type: 'nogo', 'config.blah': 'alsonot'])
                            .configuration(
                            [
                                    a: 'b',
                                    c: 'd'
                            ]
                    ).build(),

                    new SimplePluginConfiguration.SimplePluginConfigurationBuilder()
                            .service('AService')
                            .provider('provider2')
                            .configuration(
                            [
                                    x: 'y',
                                    z: 'w'
                            ]
                    ).build(),

            ]
        when:
            def result = ProjectNodeSupport.serializePluginConfigurations(prefix, configs, true)
        then:
            result
            result.size() == 9
            result == [
                    (prefix + '.1.type')    : 'provider1',
                    (prefix + '.1.config.a'): 'b',
                    (prefix + '.1.config.c'): 'd',
                    (prefix + '.1.config.blah'): 'alsonot',
                    (prefix + '.1.q')       : 't',
                    (prefix + '.1.r')       : 'v',
                    (prefix + '.2.type')    : 'provider2',
                    (prefix + '.2.config.x'): 'y',
                    (prefix + '.2.config.z'): 'w',

            ]
    }

    def "read plugin configs with extra"() {

        given:
            def prefix = "xyz"
            def props = [
                    (prefix + '.1.type')    : 'provider1',
                    (prefix + '.1.config.a'): 'b',
                    (prefix + '.1.config.c'): 'd',
                    (prefix + '.1.q')       : 't',
                    (prefix + '.1.r')       : 'v',
                    (prefix + '.2.type')    : 'provider2',
                    (prefix + '.2.config.x'): 'y',
                    (prefix + '.2.config.z'): 'w',

            ]
            def svc = "asdf"
        when:

            def result = ProjectNodeSupport.listPluginConfigurations(props, prefix, svc, true)
        then:
            result.size() == 2
            result[0].service == svc
            result[0].provider == 'provider1'
            result[0].configuration == [
                    a: 'b',
                    c: 'd'
            ]
            result[0].extra == [
                    q: 't',
                    r: 'v'
            ]


            result[1].service == svc
            result[1].provider == 'provider2'
            result[1].configuration == [
                    x: 'y',
                    z: 'w'
            ]
            result[1].extra == [:]


    }

    def "read plugin configs with extra 2"() {

        given:
        def prefix = "xyz"
        def props = [
                (prefix + '.1.type')    : 'provider1',
                (prefix + '.1.config.a'): 'b',
                (prefix + '.1.config.c'): 'd',
                (prefix + '.1.q')       : 't',
                (prefix + '.1.r')       : 'v',
                (prefix + '.1.z.y.a')       : 'config1',
                (prefix + '.1.z.y.b')   : 'config2',
                (prefix + '.1.r')       : 'v',
                (prefix + '.2.type')    : 'provider2',
                (prefix + '.2.config.x'): 'y',
                (prefix + '.2.config.z'): 'w',

        ]
        def svc = "asdf"
        when:

        def result = ProjectNodeSupport.listPluginConfigurations(props, prefix, svc, true)
        then:
        result.size() == 2
        result[0].service == svc
        result[0].provider == 'provider1'
        result[0].configuration == [
                a: 'b',
                c: 'd'
        ]
        result[0].extra == [
                q: 't',
                r: 'v',
                z: [
                    y: [
                            a: 'config1',
                            b: 'config2',
                    ]
                ]

            ]

        result[1].service == svc
        result[1].provider == 'provider2'
        result[1].configuration == [
                x: 'y',
                z: 'w'
        ]
        result[1].extra == [:]


    }


    def "load listResourceModelConfigurations"(){
        given:
        def prefix = "resources.source"
        def props = [
                (prefix + '.1.type')    : 'provider1',
                (prefix + '.1.config.a'): 'b',
                (prefix + '.1.config.c'): 'd',
                (prefix + '.1.z.y.a')       : 'config1',
                (prefix + '.1.z.y.b')   : 'config2',
                (prefix + '.2.type')    : 'provider2',
                (prefix + '.2.config.x'): 'y',
                (prefix + '.2.config.z'): 'w',

        ]
        Properties properties = new Properties()
        properties.putAll(props)


        when:
        def result = ProjectNodeSupport.listResourceModelConfigurations(properties)

        then:
        result!=null
        result[0].type == "provider1"
        result[0].props == ['a':'b','c':'d']
        result[0].extraProps == ['z.y.a':'config1','z.y.b':'config2']
        result[1].type == "provider2"
        result[1].props == ['x':'y','z':'w']
        result[1].extraProps == [:]
    }


    /**
     * Create a support whose n sources are the given closures, each invoked by getNodes()
     */
    private ProjectNodeSupport supportWithSources(Map extraProps, List<Closure<INodeSet>> loaders) {
        def props = new Properties()
        loaders.eachWithIndex { c, i ->
            props["resources.source.${i + 1}.type".toString()] = 'file'
            props["resources.source.${i + 1}.config.file".toString()] = "/tmp/file${i}".toString()
        }
        extraProps.each { k, v -> props[k] = v }
        def config = Mock(IRundeckProjectConfig) {
            getName() >> PROJECT_NAME
            getProperties() >> props
            hasProperty(_) >> { String k -> props.containsKey(k) }
            getProperty(_) >> { String k -> props.getProperty(k) }
            getConfigLastModifiedTime() >> new Date()
        }
        def sourceService = Mock(ResourceModelSourceService) {
            getCloseableSourceForConfiguration('file', _) >> { String type, Properties p ->
                int idx = (p.getProperty('file') - '/tmp/file') as int
                // not a Spock Mock: mock invocations are serialized, which would defeat parallel loading
                def source = new ResourceModelSource() {
                    @Override
                    INodeSet getNodes() throws ResourceModelSourceException {
                        loaders[idx].call()
                    }
                }
                Closeables.closeableProvider(source)
            }
        }
        def generatorService = ResourceFormatGeneratorService.getInstanceForFramework(framework, framework)
        new ProjectNodeSupport(File.createTempFile("ProjectNodeSupportSpec-varDir", "-tmp"), config, generatorService, sourceService)
    }

    private static INodeSet nodes(String name, String value) {
        def set = new NodeSetImpl()
        def node = new NodeEntryImpl(name)
        node.setAttribute('x', value)
        set.putNode(node)
        set
    }

    def "sources are queried in parallel and merged in source order"() {
        given:
        // every source waits for all the others to start, which can only complete if they run concurrently
        def allStarted = new CountDownLatch(4)
        def loaders = (1..4).collect { int n ->
            { ->
                allStarted.countDown()
                if (!allStarted.await(10, TimeUnit.SECONDS)) {
                    throw new ResourceModelSourceException('sources did not run concurrently')
                }
                nodes('a', "v${n}")
            } as Closure<INodeSet>
        }
        def support = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '4'], loaders)

        when:
        def result = support.getNodeSet()

        then:
        support.getResourceModelSourceExceptionsMap().isEmpty()
        result.getNode('a').getAttributes().x == 'v4'
    }

    def "sources are queried serially by default and when loadThreads is #threads"() {
        given:
        def active = new AtomicInteger()
        def maxActive = new AtomicInteger()
        def loaders = (1..4).collect { int n ->
            { ->
                maxActive.accumulateAndGet(active.incrementAndGet(), { a, b -> Math.max(a, b) })
                sleep(50)
                active.decrementAndGet()
                nodes("n${n}", 'v')
            } as Closure<INodeSet>
        }
        def support = supportWithSources(
                threads == null ? [:] : [(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): threads],
                loaders
        )

        when:
        def result = support.getNodeSet()

        then:
        maxActive.get() == 1
        result.nodeNames as List == ['n1', 'n2', 'n3', 'n4']

        where:
        threads << [null, '1', '0', 'not-a-number']
    }

    def "concurrency is capped at the shared maximum even if loadThreads is higher"() {
        given:
        def total = ProjectNodeSupport.MAX_LOAD_THREADS + 10
        def active = new AtomicInteger()
        def maxActive = new AtomicInteger()
        def loaders = (1..total).collect { int n ->
            { ->
                maxActive.accumulateAndGet(active.incrementAndGet(), { a, b -> Math.max(a, b) })
                sleep(30)
                active.decrementAndGet()
                nodes("n${n}", 'v')
            } as Closure<INodeSet>
        }
        def support = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '1000'], loaders)

        when:
        def result = support.getNodeSet()

        then:
        result.nodeNames.size() == total
        maxActive.get() > 1
        maxActive.get() <= ProjectNodeSupport.MAX_LOAD_THREADS
    }

    def "failure merging one source is reported by source index and later sources still load"() {
        given:
        def broken = Mock(INodeSet) {
            iterator() >> { throw new IllegalStateException('bad node data') }
        }
        def loaders = [
                { -> nodes('a', '1') } as Closure<INodeSet>,
                { -> broken } as Closure<INodeSet>,
                { -> nodes('c', '3') } as Closure<INodeSet>
        ]
        def support = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '3'], loaders)

        when:
        def result = support.getNodeSet()
        def errors = support.getResourceModelSourceExceptionsMap()

        then:
        result.nodeNames as List == ['a', 'c']
        errors.keySet() == ['2.source'] as Set
        errors['2.source'].message == 'bad node data'
    }

    def "failure of one source is reported by source index and others still load"() {
        given:
        def loaders = [
                { -> nodes('a', '1') } as Closure<INodeSet>,
                { -> throw new ResourceModelSourceException('boom') } as Closure<INodeSet>,
                { -> nodes('c', '3') } as Closure<INodeSet>
        ]
        def support = supportWithSources([:], loaders)

        when:
        def result = support.getNodeSet()
        def errors = support.getResourceModelSourceExceptionsMap()

        then:
        result.nodeNames as List == ['a', 'c']
        errors.keySet() == ['2.source'] as Set
        errors['2.source'].message == 'boom'
    }
}
