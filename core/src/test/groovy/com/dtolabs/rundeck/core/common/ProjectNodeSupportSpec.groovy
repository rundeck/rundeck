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
import com.dtolabs.rundeck.core.resources.SourceType
import com.dtolabs.rundeck.core.resources.WriteableModelSource
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.core.utils.FileUtils
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ProjectNodeSupportSpec extends Specification {
    static final String PROJECT_NAME = 'ProjectNodeSupportSpec'
    /**
     * Project name for sources from supportWithSources, unique per feature so no feature inherits another's load pool
     */
    String loadProject = "${PROJECT_NAME}-${UUID.randomUUID()}".toString()
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
        ProjectNodeSupport.removeLoadPool(loadProject)
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
            getName() >> loadProject
            getProperties() >> props
            hasProperty(_) >> { String k -> props.containsKey(k) }
            getProperty(_) >> { String k -> props.getProperty(k) }
            getConfigLastModifiedTime() >> new Date()
        }
        def sourceService = Mock(ResourceModelSourceService) {
            getCloseableSourceForConfiguration('file', _) >> { String type, Properties p ->
                int idx = (p.getProperty('file') - '/tmp/file') as int
                if (null == loaders[idx]) {
                    throw new ExecutionServiceException("failed to initialize source ${idx + 1}")
                }
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

    def "a slow early source holds back later sources so at most loadThreads results wait to be merged"() {
        given:
        def firstStarted = new CountDownLatch(1)
        def releaseFirst = new CountDownLatch(1)
        def secondDone = new CountDownLatch(1)
        def started = Collections.synchronizedList([])
        def loaders = (1..4).collect { int n ->
            { ->
                started << n
                if (n == 1) {
                    firstStarted.countDown()
                    releaseFirst.await(10, TimeUnit.SECONDS)
                }
                def result = nodes("n${n}", 'v')
                if (n == 2) {
                    secondDone.countDown()
                }
                result
            } as Closure<INodeSet>
        }
        def support = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '2'], loaders)
        def results = []

        when:
        def loading = Thread.start { results << support.getNodeSet() }
        //sources 1 and 2 run concurrently, so wait for both before checking that no later source started
        firstStarted.await(10, TimeUnit.SECONDS)
        secondDone.await(10, TimeUnit.SECONDS)
        def startedWhileFirstBlocked = new ArrayList(started).sort()
        releaseFirst.countDown()
        loading.join(10000)

        then:
        startedWhileFirstBlocked == [1, 2]
        results[0].nodeNames as List == ['n1', 'n2', 'n3', 'n4']
        support.getResourceModelSourceExceptionsMap().isEmpty()
    }

    def "sources are queried serially by default and when loadThreads is #loadThreads"() {
        given:
        def threads = Collections.synchronizedList([])
        def loaders = (1..4).collect { int n ->
            { -> threads << Thread.currentThread(); nodes("n${n}", 'v') } as Closure<INodeSet>
        }
        def support = supportWithSources(
                loadThreads == null ? [:] : [(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): loadThreads],
                loaders
        )

        when:
        def result = support.getNodeSet()

        then: "every source ran on the calling thread, one after another"
        threads == [Thread.currentThread()] * 4
        result.nodeNames as List == ['n1', 'n2', 'n3', 'n4']

        where:
        loadThreads << [null, '1', '0', 'not-a-number']
    }

    def "concurrency is capped at the per-project maximum even if loadThreads is higher"() {
        given:
        def total = ProjectNodeSupport.MAX_LOAD_THREADS + 10
        def active = new AtomicInteger()
        def maxActive = new AtomicInteger()
        // sources block until released, so the cap is reached and held without relying on timing
        def release = new CountDownLatch(1)
        def loaders = (1..total).collect { int n ->
            { ->
                maxActive.accumulateAndGet(active.incrementAndGet(), { a, b -> Math.max(a, b) })
                release.await(10, TimeUnit.SECONDS)
                active.decrementAndGet()
                nodes("n${n}", 'v')
            } as Closure<INodeSet>
        }
        def support = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '1000'], loaders)
        def results = []

        when:
        def loading = Thread.start { results << support.getNodeSet() }

        then:
        new PollingConditions(timeout: 10).eventually {
            assert active.get() == ProjectNodeSupport.MAX_LOAD_THREADS
        }

        when:
        release.countDown()
        loading.join(10000)

        then:
        results[0].nodeNames.size() == total
        maxActive.get() == ProjectNodeSupport.MAX_LOAD_THREADS
    }

    def "a serial load on an interrupted thread still queries every source"() {
        given:
        def support = supportWithSources([:], [
                { -> nodes('a', '1') } as Closure<INodeSet>,
                { -> nodes('b', '2') } as Closure<INodeSet>
        ])
        def results = []

        when:
        def loading = Thread.start {
            Thread.currentThread().interrupt()
            results << support.getNodeSet()
        }
        loading.join(10000)

        then:
        results[0].nodeNames as Set == ['a', 'b'] as Set
        support.getResourceModelSourceExceptionsMap().isEmpty()
    }

    def "a load started from a source query on a pool thread runs serially on that thread"() {
        given:
        def innerThreads = Collections.synchronizedList([])
        def inner = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '2'], [
                { -> innerThreads << Thread.currentThread(); nodes('i1', '1') } as Closure<INodeSet>,
                { -> innerThreads << Thread.currentThread(); nodes('i2', '2') } as Closure<INodeSet>
        ])
        def outerThread = null
        def outer = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '2'], [
                { -> outerThread = Thread.currentThread(); inner.getNodeSet() } as Closure<INodeSet>,
                { -> nodes('o2', '2') } as Closure<INodeSet>
        ])

        when:
        def result = outer.getNodeSet()

        then:
        result.nodeNames as Set == ['i1', 'i2', 'o2'] as Set
        outerThread.name.startsWith('ProjectNodeSupport-load-')
        innerThreads == [outerThread, outerThread]
    }

    def "parallel loads use the project's own pool, which is stopped when the project is deleted"() {
        given:
        def threads = Collections.synchronizedList([])
        def loaders = [
                { -> threads << Thread.currentThread(); nodes('a', '1') } as Closure<INodeSet>,
                { -> threads << Thread.currentThread(); nodes('b', '2') } as Closure<INodeSet>
        ]
        def support = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '2'], loaders)

        when:
        support.getNodeSet()
        ProjectNodeSupport.removeLoadPool(loadProject)
        threads.each { it.join(10000) }
        def reloaded = support.getNodeSet()

        then:
        threads.size() == 4
        threads.every { it.name.startsWith("ProjectNodeSupport-load-${loadProject}-".toString()) }
        !threads[0].alive
        !threads[1].alive
        reloaded.nodeNames as List == ['a', 'b']
    }

    def "loads waiting on queued tasks finish when the project's pool is stopped"() {
        given: "two loads of 20 slow sources each, so 20 tasks are queued behind the pool's 20 threads"
        def started = new CountDownLatch(20)
        def loaders = (1..20).collect {
            { -> started.countDown(); new CountDownLatch(1).await(30, TimeUnit.SECONDS); nodes('n', '1') } as Closure<INodeSet>
        }
        def supports = (1..2).collect {
            supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '20'], loaders)
        }
        def results = Collections.synchronizedList([])

        when:
        def loads = supports.collect { support -> Thread.start { results << support.getNodeSet() } }
        started.await(10, TimeUnit.SECONDS)
        ProjectNodeSupport.removeLoadPool(loadProject)
        loads.each { it.join(10000) }

        then:
        loads.every { !it.alive }
        results.size() == 2
        supports.every { it.getResourceModelSourceExceptionsMap().size() == 20 }
    }

    def "a load that submits to the project's stopped pool reports the remaining sources as failed"() {
        given: "the first source is still running when the pool is stopped, so the third source is submitted after"
        def firstStarted = new CountDownLatch(1)
        def loaders = [
                { -> firstStarted.countDown(); new CountDownLatch(1).await(30, TimeUnit.SECONDS); nodes('a', '1') } as Closure<INodeSet>,
                { -> nodes('b', '2') } as Closure<INodeSet>,
                { -> nodes('c', '3') } as Closure<INodeSet>
        ]
        def support = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '2'], loaders)
        def results = []

        when:
        def loading = Thread.start { results << support.getNodeSet() }
        firstStarted.await(10, TimeUnit.SECONDS)
        ProjectNodeSupport.removeLoadPool(loadProject)
        loading.join(10000)

        then:
        !loading.alive
        results.size() == 1
        support.getResourceModelSourceExceptionsMap().keySet().containsAll(['1.source', '3.source'])
    }

    def "pool threads use each loading thread's context classloader, also when reused by a later load"() {
        given:
        def firstLoader = new URLClassLoader(new URL[0])
        def secondLoader = new URLClassLoader(new URL[0])
        def seen = Collections.synchronizedList([])
        def loaders = [
                { -> seen << Thread.currentThread().contextClassLoader; nodes('a', '1') } as Closure<INodeSet>,
                { -> seen << Thread.currentThread().contextClassLoader; nodes('b', '2') } as Closure<INodeSet>
        ]
        def support = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '2'], loaders)
        def loadWith = { ClassLoader loader ->
            def loading = Thread.start {
                Thread.currentThread().contextClassLoader = loader
                support.getNodeSet()
            }
            loading.join(10000)
        }

        when: "the second load reuses the pool threads created by the first"
        loadWith(firstLoader)
        loadWith(secondLoader)

        then:
        seen.size() == 4
        seen[0..1].every { it.is(firstLoader) }
        seen[2..3].every { it.is(secondLoader) }
    }

    def "a load waiting on a running source that ignores interruption finishes when the project's pool is stopped"() {
        given:
        def started = new CountDownLatch(1)
        def release = new CountDownLatch(1)
        def loaders = [
                { ->
                    started.countDown()
                    //ignores interruption, like a source blocked on a monitor
                    while (release.count > 0) {
                        try {
                            release.await(10, TimeUnit.SECONDS)
                        } catch (InterruptedException ignored) {
                        }
                    }
                    nodes('a', '1')
                } as Closure<INodeSet>,
                { -> nodes('b', '2') } as Closure<INodeSet>
        ]
        def support = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '2'], loaders)
        def results = []

        when:
        def loading = Thread.start { results << support.getNodeSet() }
        started.await(10, TimeUnit.SECONDS)
        ProjectNodeSupport.removeLoadPool(loadProject)
        loading.join(10000)

        then:
        !loading.alive
        results.size() == 1
        support.getResourceModelSourceExceptionsMap().keySet() == ['1.source', '2.source'] as Set

        cleanup:
        release.countDown()
    }

    def "a reload after an interrupted load shares the project's pool with sources still running from that load"() {
        given: "the first load's sources ignore interruption and keep every pool thread busy"
        def max = ProjectNodeSupport.MAX_LOAD_THREADS
        def active = new AtomicInteger()
        def maxActive = new AtomicInteger()
        def release = new CountDownLatch(1)
        def track = { Closure<INodeSet> body ->
            { ->
                maxActive.accumulateAndGet(active.incrementAndGet(), { a, b -> Math.max(a, b) })
                try {
                    body.call()
                } finally {
                    active.decrementAndGet()
                }
            } as Closure<INodeSet>
        }
        def stuck = (1..max).collect { int n ->
            track { ->
                while (release.count > 0) {
                    try {
                        release.await(10, TimeUnit.SECONDS)
                    } catch (InterruptedException ignored) {
                    }
                }
                nodes("s${n}", 'v')
            }
        }
        def quick = (1..max).collect { int n -> track { -> nodes("q${n}", 'v') } }
        def props = [(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): "${max}".toString()]
        def first = supportWithSources(props, stuck)
        def second = supportWithSources(props, quick)
        def results = []

        when: "the first load is interrupted while its sources run, then the project reloads"
        def firstLoad = Thread.start { first.getNodeSet() }
        new PollingConditions(timeout: 10).eventually {
            assert active.get() == max
        }
        firstLoad.interrupt()
        firstLoad.join(10000)
        def reload = Thread.start { results << second.getNodeSet() }
        reload.join(500)

        then: "the reload waits for a pool thread instead of starting more queries"
        !firstLoad.alive
        reload.alive
        active.get() == max

        when:
        release.countDown()
        reload.join(10000)

        then:
        !reload.alive
        results[0].nodeNames.size() == max
        maxActive.get() == max

        cleanup:
        release.countDown()
    }

    def "pool threads do not inherit the loading thread's inheritable thread-local values"() {
        given:
        def bound = new InheritableThreadLocal<String>()
        def seen = Collections.synchronizedList([])
        def loaders = [
                { -> seen << bound.get(); nodes('a', '1') } as Closure<INodeSet>,
                { -> seen << bound.get(); nodes('b', '2') } as Closure<INodeSet>
        ]
        def support = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '2'], loaders)

        when:
        def loading = Thread.start {
            bound.set('execution-output')
            support.getNodeSet()
        }
        loading.join(10000)

        then:
        seen == [null, null]
    }

    def "an interrupted load keeps the nodes already loaded and reports the sources that did not complete"() {
        given:
        def firstThread = null
        def firstDone = new CountDownLatch(1)
        def secondStarted = new CountDownLatch(1)
        def loaders = [
                { -> firstThread = Thread.currentThread(); firstDone.countDown(); nodes('a', '1') } as Closure<INodeSet>,
                { -> secondStarted.countDown(); new CountDownLatch(1).await(10, TimeUnit.SECONDS); nodes('b', '2') } as Closure<INodeSet>
        ]
        def support = supportWithSources([(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): '2'], loaders)
        def results = []

        when:
        def loading = Thread.start { results << support.getNodeSet() }
        firstDone.await(10, TimeUnit.SECONDS)
        secondStarted.await(10, TimeUnit.SECONDS)
        // the first source's task has completed once its pool thread is idle again, so the interrupt can only
        // arrive while the load waits for the second source
        new PollingConditions(timeout: 10).eventually {
            assert firstThread.state == Thread.State.TIMED_WAITING
        }
        loading.interrupt()
        loading.join(10000)

        then:
        results[0].nodeNames as List == ['a']
        support.getResourceModelSourceExceptionsMap().keySet() == ['2.source'] as Set
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

    def "a source failure after a source that failed to initialize is reported by its configured index"() {
        given: "source 1 fails to initialize, so source 2 is the first loaded source"
        def support = supportWithSources([:], [
                null,
                { -> throw new RuntimeException('query failed') } as Closure<INodeSet>,
                { -> nodes('c', '3') } as Closure<INodeSet>
        ])

        when:
        def result = support.getNodeSet()

        then:
        result.nodeNames as List == ['c']
        def errors = support.getResourceModelSourceExceptionsMap()
        errors.keySet() == ['1.source', '2.source'] as Set
        errors['2.source'].message.contains('query failed')
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
