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
import com.dtolabs.rundeck.core.resources.FileResourceModelSource
import com.dtolabs.rundeck.core.resources.DelegateResourceModelSource
import com.dtolabs.rundeck.core.resources.ExceptionCatchingResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.dtolabs.rundeck.core.resources.ResourceModelSourceErrors
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.core.resources.SourceType
import com.dtolabs.rundeck.core.resources.WriteableModelSource
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.core.utils.FileUtils
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import groovy.transform.CompileStatic
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.WriterAppender
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.core.layout.PatternLayout
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ProjectNodeSupportSpec extends Specification {
    static final String PROJECT_NAME = 'ProjectNodeSupportSpec'
    /**
     * Project name for sources from supportWithSources; cleanup removes its load pool, so no feature inherits another's
     */
    static final String loadProject = "${PROJECT_NAME}-load".toString()
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

    def "loaded source toString identifies project, index and type"() {
        given:
        def source = new ProjectNodeSupport.LoadedSource('Foo', 3, 'aws-ec2', Mock(ResourceModelSource))

        expect:
        source.toString() == 'project=Foo source #3 (aws-ec2)'
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
        //the first load's workers alone release the latch, so also wait for the second load to queue on the same pool
        def pool = ProjectNodeSupport.LOAD_POOLS[loadProject]
        new PollingConditions(timeout: 10).eventually {
            assert pool.queue.size() == 20
        }
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

        //start every pool thread up front so both loads run on reused threads, not new ones that inherit the loader
        ProjectNodeSupport.LOAD_POOLS.computeIfAbsent(loadProject, ProjectNodeSupport.&createLoadPool).prestartAllCoreThreads()

        when: "both loads run on pool threads created before either load"
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

    def "an interrupted load removes its queued tasks from the project's pool"() {
        given: "one load's sources ignore interruption and keep every pool thread busy"
        def max = ProjectNodeSupport.MAX_LOAD_THREADS
        def active = new AtomicInteger()
        def release = new CountDownLatch(1)
        def stuck = (1..max).collect { int n ->
            { ->
                active.incrementAndGet()
                while (release.count > 0) {
                    try {
                        release.await(10, TimeUnit.SECONDS)
                    } catch (InterruptedException ignored) {
                    }
                }
                nodes("s${n}", 'v')
            } as Closure<INodeSet>
        }
        def queued = (1..max).collect { int n -> { -> nodes("q${n}", 'v') } as Closure<INodeSet> }
        def props = [(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): "${max}".toString()]
        def busy = supportWithSources(props, stuck)
        def waiting = supportWithSources(props, queued)

        when: "a second load queues its sources, then is interrupted"
        def busyLoad = Thread.start { busy.getNodeSet() }
        new PollingConditions(timeout: 10).eventually {
            assert active.get() == max
        }
        def pool = ProjectNodeSupport.LOAD_POOLS[loadProject]
        def waitingLoad = Thread.start { waiting.getNodeSet() }
        new PollingConditions(timeout: 10).eventually {
            assert pool.queue.size() == max
        }
        waitingLoad.interrupt()
        waitingLoad.join(10000)

        then: "its cancelled tasks are no longer queued"
        !waitingLoad.alive
        pool.queue.isEmpty()

        cleanup:
        release.countDown()
        busyLoad?.join(10000)
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
        def label2 = "project=${loadProject} source #2 (file)".toString()
        def result = null

        when:
        def lines = captureLogs { result = support.getNodeSet() }
        def errors = support.getResourceModelSourceExceptionsMap()

        then:
        result.nodeNames as List == ['a', 'c']
        errors.keySet() == ['2.source'] as Set
        errors['2.source'].message == 'bad node data'

        and: "counting nodes for the log does not fail the load, and the merge error names the source"
        lines.any { it.contains("Loaded unknown nodes from ${label2} in ") }
        !lines.any { it.contains("Cannot get nodes from [${label2}]") }
        lines.any { it.startsWith('ERROR [') && it.endsWith("Cannot merge nodes from [${label2}]: bad node data") }
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

    /**
     * The delegating source describes itself with its delegate, and a named class even when subclassed anonymously.
     */
    def "delegate source toString includes class and delegate"() {
        given:
        def delegate = Mock(ResourceModelSource) {
            toString() >> 'inner-source'
        }
        def source = new DelegateResourceModelSource(delegate) {}

        expect:
        source.toString().startsWith(source.getClass().name + "{")
        source.toString().endsWith('{delegate=inner-source}')
    }

    /**
     * Log messages for a failing source carry the project name.
     */
    def "source context names the project, index and type; detail names the delegate"() {
        given:
        def support = supportWithSources([:], [])
        def delegate = Mock(ResourceModelSource) {
            toString() >> 'inner'
        }
        def loaded = new ProjectNodeSupport.LoadedSource('Foo', 3, 'file', delegate)

        expect:
        support.sourceContext(loaded) == '[project=Foo source #3 (file)]'
        ProjectNodeSupport.sourceDetail(loaded) == ' (source: inner)'
    }

    /**
     * A source whose toString() throws must not break error handling in querySource.
     */
    def "source context survives a failing toString"() {
        given:
        def support = supportWithSources([:], [])
        def source = new ResourceModelSource() {
            INodeSet getNodes() { null }

            String toString() { throw new IllegalStateException('boom') }
        }

        expect:
        support.sourceContext(source) == "[${source.getClass().name}]".toString()
    }

    /**
     * A file source with an unparseable YAML file fails, and its description names the file, so the
     * logged node source identifies the offending file.
     */
    def "failing file source description names its file"() {
        given:
        def nodesFile = new File(directory, 'bad-nodes.yaml')
        nodesFile.text = 'node1: [unclosed'
        def fileSource = new FileResourceModelSource(framework)
        fileSource.configure(
                [
                        project: PROJECT_NAME,
                        file   : nodesFile.absolutePath,
                        format : 'resourceyaml'
                ] as Properties
        )
        def loaded = new ProjectNodeSupport.LoadedSource(PROJECT_NAME, 1, 'file', fileSource)

        when:
        loaded.getNodes()

        then:
        thrown(ResourceModelSourceException)
        ProjectNodeSupport.sourceDetail(loaded).contains(nodesFile.path)
    }

    /**
     * Capture log output of the named loggers, ProjectNodeSupport by default, at DEBUG and above while the closure runs
     *
     * @return captured lines, formatted as "LEVEL [thread] message"
     */
    private static List<String> captureLogs(Closure action, List<String> names = [ProjectNodeSupport.name]) {
        def writer = new StringWriter()
        def ctx = (LoggerContext) LogManager.getContext(false)
        def config = ctx.getConfiguration()
        def appender = WriterAppender.newBuilder()
                .setName('ProjectNodeSupportSpecCapture')
                .setTarget(writer)
                .setLayout(PatternLayout.newBuilder().withPattern('%level [%t] %m%n').build())
                .build()
        appender.start()
        names.each { name ->
            def loggerConfig = LoggerConfig.newBuilder()
                    .withLoggerName(name)
                    .withLevel(Level.DEBUG)
                    .withAdditivity(false)
                    .withConfig(config)
                    .build()
            loggerConfig.addAppender(appender, Level.DEBUG, null)
            config.addLogger(name, loggerConfig)
        }
        ctx.updateLoggers()
        try {
            action.call()
        } finally {
            names.each { config.removeLogger(it) }
            ctx.updateLoggers()
            appender.stop()
        }
        writer.toString().readLines()
    }

    def "source load is logged with the source label, node count and duration when loadThreads is #loadThreads"() {
        given:
        def support = supportWithSources(
                loadThreads == null ? [:] : [(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): loadThreads],
                [
                        { -> nodes('a', '1') } as Closure<INodeSet>,
                        { -> throw new ResourceModelSourceException('boom') } as Closure<INodeSet>
                ]
        )
        def label1 = "project=${loadProject} source #1 (file)".toString()
        def label2 = "project=${loadProject} source #2 (file)".toString()
        def expectedThread = parallel ? "ProjectNodeSupport-load-${loadProject}-".toString() : Thread.currentThread().name

        when:
        def lines = captureLogs { support.getNodeSet() }

        then: "the successful source logs start and finish lines"
        def start1 = lines.find { it.endsWith("Loading nodes from ${label1}") }
        start1.startsWith('DEBUG [')
        def done1 = lines.find { it.contains("Loaded 1 nodes from ${label1} in ") }
        done1 ==~ /DEBUG \[.+\] Loaded 1 nodes from \Q${label1}\E in \d+ ms/

        and: "the throwing source logs a start line and an error, but no finish line"
        def start2 = lines.find { it.endsWith("Loading nodes from ${label2}") }
        start2.startsWith('DEBUG [')
        def error2 = lines.find { it.contains("Cannot get nodes from [${label2}]: boom") }
        error2.startsWith('ERROR [')
        !lines.any { it.contains("nodes from ${label2} in ") }

        and: "lines for a source are logged on the thread that loads it"
        [start1, done1, start2, error2].every { it.startsWith("DEBUG [${expectedThread}") || it.startsWith("ERROR [${expectedThread}") }
        !lines.any { it.contains('LoadedSource@') }

        where:
        loadThreads | parallel
        null        | false
        '2'         | true
    }

    /**
     * Create a support with one source of a type that uses the file cache, whose cached nodes are node "cached"
     */
    private ProjectNodeSupport supportWithCachedSource(ResourceModelSource plugin) {
        supportWithCachedSources([:], [plugin])
    }

    /**
     * Create a support whose sources are of a type that uses the file cache, one per plugin, whose cached nodes
     * are node "cached"
     */
    private ProjectNodeSupport supportWithCachedSources(Map extraProps, List<ResourceModelSource> plugins) {
        def props = new Properties()
        plugins.eachWithIndex { p, i ->
            props["resources.source.${i + 1}.type".toString()] = 'cachedtype'
            props["resources.source.${i + 1}.config.id".toString()] = "${i}".toString()
        }
        extraProps.each { k, v -> props[k] = v }
        def cacheFile = new ResourceModelSource() {
            @Override
            INodeSet getNodes() throws ResourceModelSourceException {
                ProjectNodeSupportSpec.nodes('cached', 'v')
            }
        }
        def sourceService = Mock(ResourceModelSourceService) {
            getCloseableSourceForConfiguration('cachedtype', _) >> { String type, Properties p ->
                Closeables.closeableProvider(plugins[p.getProperty('id') as int])
            }
            getSourceForConfiguration('file', _) >> cacheFile
        }
        newSupport(props, sourceService)
    }

    /**
     * Create a support for project loadProject with the given project properties and source service
     */
    private ProjectNodeSupport newSupport(Properties props, ResourceModelSourceService sourceService) {
        def config = Mock(IRundeckProjectConfig) {
            getName() >> loadProject
            getProperties() >> props
            hasProperty(_) >> { String k -> props.containsKey(k) }
            getProperty(_) >> { String k -> props.getProperty(k) }
            getConfigLastModifiedTime() >> new Date()
        }
        def generatorService = ResourceFormatGeneratorService.getInstanceForFramework(framework, framework)
        def varDir = File.createTempFile("ProjectNodeSupportSpec-varDir", "-tmp")
        varDir.delete()
        varDir.mkdirs()
        new ProjectNodeSupport(varDir, config, generatorService, sourceService)
    }

    def "a cached source that fails logs a cache fallback warning instead of a load line"() {
        given: "a source type that uses the file cache, whose plugin throws, with cached nodes available"
        def failing = new ResourceModelSource() {
            @Override
            INodeSet getNodes() throws ResourceModelSourceException {
                throw new ResourceModelSourceException('not authorized')
            }
        }
        def support = supportWithCachedSource(failing)
        def label = "project=${loadProject} source #1 (cachedtype)".toString()
        def result = null

        when:
        def lines = captureLogs({ result = support.getNodeSet() }, [ProjectNodeSupport.name, ExceptionCatchingResourceModelSource.name])

        then: "cached nodes are served and the failure is recorded"
        result.nodeNames as List == ['cached']
        support.getResourceModelSourceExceptionsMap().keySet() == ['1.source'] as Set

        and: "the log says the load failed and cached nodes are used, not that nodes were loaded"
        lines.any { it.startsWith('DEBUG [') && it.endsWith("Loading nodes from ${label}") }
        lines.any {
            it ==~ /WARN \[.+\] Loading nodes from \Q${label}\E failed after \d+ ms, using 1 cached nodes: not authorized/
        }
        !lines.any { it.contains("Loaded 1 nodes from ${label}") }

        and: "the caching source's own error line includes the exception message"
        lines.any {
            it.startsWith('ERROR [') &&
            it.endsWith("[ResourceModelSource: 1.source (cachedtype), project: ${loadProject}] not authorized")
        }
    }

    /**
     * Source that returns nodes and also reports errors, like the EC2 plugin when some regions fail
     */
    @CompileStatic
    static class PartialErrorsSource implements ResourceModelSource, ResourceModelSourceErrors {
        List<String> modelSourceErrors

        @Override
        INodeSet getNodes() throws ResourceModelSourceException {
            ProjectNodeSupportSpec.nodes('a', '1')
        }
    }

    def "source errors are reported one per line"() {
        given:
        def props = new Properties()
        props['resources.source.1.type'] = 'file'
        props['resources.source.1.config.file'] = '/tmp/file0'
        def source = new PartialErrorsSource(modelSourceErrors: ['region 1 failed', 'region 2 failed'])
        def sourceService = Mock(ResourceModelSourceService) {
            getCloseableSourceForConfiguration('file', _) >> Closeables.closeableProvider(source)
        }
        def support = newSupport(props, sourceService)

        when:
        def result = support.getNodeSet()

        then: "nodes are still loaded and the errors are joined with newlines"
        result.nodeNames as List == ['a']
        support.getResourceModelSourceExceptionsMap()['1.source'].message == 'region 1 failed\nregion 2 failed'
    }

    def "a failure recorded by an overlapping load of the same source does not cause a cache fallback warning"() {
        given: "a cached source that succeeds, while another load of the same project records a failure for it"
        ProjectNodeSupport support = null
        def plugin = new ResourceModelSource() {
            @Override
            INodeSet getNodes() throws ResourceModelSourceException {
                support.nodesSourceExceptions.put('1.source', new ResourceModelSourceException('other load failed'))
                ProjectNodeSupportSpec.nodes('fresh', 'v')
            }
        }
        support = supportWithCachedSource(plugin)
        def label = "project=${loadProject} source #1 (cachedtype)".toString()
        def result = null

        when:
        def lines = captureLogs { result = support.getNodeSet() }

        then:
        result.nodeNames as List == ['fresh']
        lines.any { it.contains("Loaded 1 nodes from ${label} in ") }
        !lines.any { it.contains('cached nodes') }
    }

    def "an empty result is logged with the source label"() {
        given:
        def support = supportWithSources([:], [{ -> null } as Closure<INodeSet>])
        def label = "project=${loadProject} source #1 (file)".toString()

        when:
        def lines = captureLogs { support.getNodeSet() }

        then:
        lines.any { it.startsWith('WARN [') && it.endsWith("Empty nodes result from [${label}]") }
        lines.any { it.contains("Loaded 0 nodes from ${label} in ") }
    }

    def "a failing cached source logs the cache fallback warning on its loading thread when loadThreads is #loadThreads"() {
        given: "two cached sources: the first fails, the second succeeds"
        def failing = new ResourceModelSource() {
            @Override
            INodeSet getNodes() throws ResourceModelSourceException {
                throw new ResourceModelSourceException('not authorized')
            }
        }
        def working = new ResourceModelSource() {
            @Override
            INodeSet getNodes() throws ResourceModelSourceException {
                ProjectNodeSupportSpec.nodes('fresh', 'v')
            }
        }
        def support = supportWithCachedSources(
                loadThreads == null ? [:] : [(ProjectNodeSupport.PROJECT_RESOURCES_LOAD_THREADS): loadThreads],
                [failing, working]
        )
        def label1 = "project=${loadProject} source #1 (cachedtype)".toString()
        def label2 = "project=${loadProject} source #2 (cachedtype)".toString()
        def expectedThread = parallel ? "ProjectNodeSupport-load-${loadProject}-".toString() : Thread.currentThread().name
        def result = null

        when:
        def lines = captureLogs { result = support.getNodeSet() }

        then: "the failing source falls back to cached nodes and the other source loads"
        result.nodeNames as Set == ['cached', 'fresh'] as Set
        support.getResourceModelSourceExceptionsMap().keySet() == ['1.source'] as Set

        and: "the warning is logged for the failing source only, on the thread that loaded it"
        def warn1 = lines.find { it.contains("Loading nodes from ${label1} failed after ") }
        warn1 ==~ /WARN \[\Q${expectedThread}\E.*\] Loading nodes from \Q${label1}\E failed after \d+ ms, using 1 cached nodes: not authorized/
        !lines.any { it.contains("Loaded 1 nodes from ${label1}") }
        lines.any { it.startsWith("DEBUG [${expectedThread}") && it.contains("Loaded 1 nodes from ${label2} in ") }
        !lines.any { it.contains("Loading nodes from ${label2} failed") }

        where:
        loadThreads | parallel
        null        | false
        '2'         | true
    }

    def "a cached source that loads another project's nodes before failing still logs the cache fallback warning"() {
        given: "the outer source's plugin loads an inner project's nodes on the same thread, then fails"
        def inner = supportWithSources([:], [{ -> nodes('inner', 'v') } as Closure<INodeSet>])
        def outerPlugin = new ResourceModelSource() {
            @Override
            INodeSet getNodes() throws ResourceModelSourceException {
                inner.getNodeSet()
                throw new ResourceModelSourceException('not authorized')
            }
        }
        def outer = supportWithCachedSource(outerPlugin)
        def label = "project=${loadProject} source #1 (cachedtype)".toString()
        def result = null

        when:
        def lines = captureLogs { result = outer.getNodeSet() }

        then: "the inner query does not discard the outer query's failure"
        result.nodeNames as List == ['cached']
        lines.any { it.contains("Loading nodes from ${label} failed after ") }
        !lines.any { it.contains("Loaded 1 nodes from ${label}") }
    }
}
