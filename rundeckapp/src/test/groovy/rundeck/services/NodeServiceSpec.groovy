/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.plugins.Closeables
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.spi.Services
import org.springframework.core.task.AsyncTaskExecutor
import rundeck.services.nodes.CachedProjectNodes
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 2/3/16.
 */
class NodeServiceSpec extends Specification implements ServiceUnitTest<NodeService> {
    private static final String RESOURCE_TMP_DIR = '/tmp/rundeckNodeServiceSpec'

    def setup(){

        service.configurationService = Mock(ConfigurationService) {
            getBoolean('nodeService.nodeCache.enabled', true) >> true
        }
    }
    def "get nodes project DNE"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        when:
        def result = service.getNodes('test1')


        then:
        1 * service.frameworkService.getRundeckFramework() >> Mock(Framework) {
            1 * getFrameworkProjectMgr() >> Mock(ProjectManager) {
                1 * existsFrameworkProject('test1') >> false
            }
        }
        IllegalArgumentException e = thrown()
        e.message == 'Project does not exist: test1'
    }

    def "get nodes project exists"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        INodeSet nodeSet = new NodeSetImpl()
        nodeSet.putNode(new NodeEntryImpl('anode'))
        def properties = [

                'framework.var.dir'                                  : RESOURCE_TMP_DIR,
                'resources.source.1.type'                            : 'file',
                'resources.source.1.config.file'                     : '/tmp/test.xml',
                'resources.source.1.config.generateFileAutomatically': 'false',
                'resources.source.1.config.includeServerNode'        : 'true',
                          'project.nodeCache.enabled':'false'
        ]

        def projConfig = new PropsConfig(
                projectProperties: properties,
                properties: properties,
                name: 'test1',
                configLastModifiedTime: new Date()
        )
        def modelSourceFactory = Mock(ResourceModelSourceFactory)
        def modelSource = Mock(ResourceModelSource)

        def frameWorkService = Mock(FrameworkService)
        def pluginService = Mock(PluginService)
        def nodeSourceLoaderService =  new NodeSourceLoaderService()
        def projectManagerService = Mock(ProjectManagerService)

        nodeSourceLoaderService.pluginService = pluginService
        nodeSourceLoaderService.frameworkService = frameWorkService
        nodeSourceLoaderService.projectManagerService = projectManagerService
        nodeSourceLoaderService.rundeckSpiBaseServicesProvider = Mock(Services)

        service.nodeSourceLoaderService = nodeSourceLoaderService
        service.frameworkService = frameWorkService

        when:
        def result = service.getNodes('test1')
        def nodes1 = result.getNodeSet()


        then:
        frameWorkService.getRundeckFramework() >> Mock(Framework) {
            getFrameworkProjectMgr() >> Mock(ProjectManager) {
                existsFrameworkProject('test1') >> true
                loadProjectConfig('test1') >> projConfig
            }
            getResourceModelSourceService() >> Mock(ResourceModelSourceService) {
                0 * getCloseableSourceForConfiguration('file', _) >> Closeables.closeableProvider(modelSource)
            }
        }
        1 * projectManagerService.getNonAuthorizingProjectServicesForPlugin('test1','ResourceModelSource','file')
        1 * pluginService.getRundeckPluginRegistry()>>Mock(PluginRegistry){
            1 * retainConfigurePluginByName('file', _, _, _) >>
            new ConfiguredPlugin(modelSourceFactory, [:], Closeables.closeableProvider(modelSourceFactory, null))
        }
        0 * pluginService.retainPlugin('file', _) >> Closeables.closeableProvider(modelSourceFactory)
        1 * modelSourceFactory.createResourceModelSource(_,_) >> modelSource
        _ * modelSource.getNodes() >> nodeSet
        null != nodes1.getNode('anode')
        nodes1.nodeNames as List == ['anode']
    }

    def "refresh project nodes invalidates the cached entry"() {
        given:
        def cache = Mock(LoadingCache)
        service.nodeCache = cache

        when:
        service.refreshProjectNodes('test1')

        then:
        1 * cache.invalidate('test1')
        0 * cache.refresh(_)
    }

    def "refresh project nodes in background refreshes cached entry so old nodes keep being served"() {
        given:
        def cache = Mock(LoadingCache)
        def cached = new CachedProjectNodes()
        service.nodeCache = cache

        when:
        service.refreshProjectNodesInBackground('test1')

        then:
        1 * cache.getIfPresent('test1') >> cached
        1 * cache.refresh('test1')
        0 * cache.invalidate(_)
        service.lastReloadRequest('test1') == 1
    }

    def "refresh project nodes in background invalidates when nothing cached, to avoid synchronous load"() {
        given:
        def cache = Mock(LoadingCache)
        service.nodeCache = cache

        when:
        service.refreshProjectNodesInBackground('test1')

        then:
        1 * cache.getIfPresent('test1') >> null
        1 * cache.invalidate('test1')
        0 * cache.refresh(_)
    }

    def "cached nodes keep being served while a background refresh is still loading, then are replaced"() {
        given:
        def pending = SettableFuture.<CachedProjectNodes> create()
        service.nodeCache = CacheBuilder.newBuilder().build(new CacheLoader<String, CachedProjectNodes>() {
            CachedProjectNodes load(String key) { throw new IllegalStateException('should not load synchronously') }

            ListenableFuture<CachedProjectNodes> reload(String key, CachedProjectNodes oldValue) { pending }
        })
        def old = new CachedProjectNodes(cacheTime: new Date())
        def replacement = new CachedProjectNodes(cacheTime: new Date())
        service.nodeCache.put('test1', old)

        when: "a background refresh starts and has not completed"
        service.refreshProjectNodesInBackground('test1')

        then: "the old nodes are still served"
        service.lastReloadRequest('test1') == 1
        service.nodeCache.getIfPresent('test1').is(old)
        service.nodeCache.get('test1').is(old)

        when: "the reload completes"
        pending.set(replacement)

        then: "the new nodes replace the old"
        service.nodeCache.getIfPresent('test1').is(replacement)
    }

    def "reload request is marked as handled once the reload starts so a failed reload does not retry forever"() {
        given:
        service.configurationService = Mock(ConfigurationService)
        service.nodeTaskExecutor = Mock(AsyncTaskExecutor)
        service.frameworkService = Mock(FrameworkService) {
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager) {
                    loadProjectConfig('test1') >> new PropsConfig(
                            projectProperties: [:],
                            properties: [:],
                            name: 'test1',
                            configLastModifiedTime: new Date(System.currentTimeMillis() - 60000)
                    )
                }
            }
        }
        service.afterPropertiesSet()
        def old = new CachedProjectNodes(cacheTime: new Date())
        service.nodeCache.put('test1', old)

        when:
        service.refreshProjectNodesInBackground('test1')

        then:
        1 * service.nodeTaskExecutor.execute(_)
        old.reloadRequestSequence == 1
    }

    def "needs reload when a reload was requested after the cached nodes were loaded, even if config is unchanged and cache is fresh"(boolean requested) {
        given:
        service.nodeCache = Mock(LoadingCache)
        service.frameworkService = Mock(FrameworkService) {
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager) {
                    loadProjectConfig('test1') >> new PropsConfig(
                            projectProperties: [:],
                            properties: [:],
                            name: 'test1',
                            configLastModifiedTime: new Date(System.currentTimeMillis() - 60000)
                    )
                }
            }
        }
        def cached = new CachedProjectNodes(cacheTime: new Date())
        if (requested) {
            service.refreshProjectNodesInBackground('test1')
        }

        expect:
        service.needsReload('test1', cached) == requested

        where:
        requested | _
        true      | _
        false     | _
    }

    def "a reload requested while another reload is running is not lost"() {
        given:
        def pending = SettableFuture.<CachedProjectNodes> create()
        service.nodeCache = CacheBuilder.newBuilder().build(new CacheLoader<String, CachedProjectNodes>() {
            CachedProjectNodes load(String key) { throw new IllegalStateException('should not load synchronously') }

            ListenableFuture<CachedProjectNodes> reload(String key, CachedProjectNodes oldValue) { pending }
        })
        service.frameworkService = Mock(FrameworkService) {
            getRundeckFramework() >> Mock(Framework) {
                getProjectManager() >> Mock(ProjectManager) {
                    loadProjectConfig('test1') >> new PropsConfig(
                            projectProperties: [:],
                            properties: [:],
                            name: 'test1',
                            // same coarse timestamp for both config saves, so timestamps cannot tell them apart
                            configLastModifiedTime: new Date(System.currentTimeMillis() - 60000)
                    )
                }
            }
        }
        service.nodeCache.put('test1', new CachedProjectNodes(cacheTime: new Date()))

        when: "the first save starts a reload, and a second save arrives before it finishes"
        service.refreshProjectNodesInBackground('test1')
        // the running reload loaded its nodes after the first request but before the second
        def reloaded = new CachedProjectNodes(cacheTime: new Date(), reloadRequestSequence: 1)
        service.refreshProjectNodesInBackground('test1')
        pending.set(reloaded)

        then: "the completed reload is known to be stale, so the next check reloads again"
        service.nodeCache.getIfPresent('test1').is(reloaded)
        service.needsReload('test1', reloaded)
        !service.needsReload('test1', new CachedProjectNodes(cacheTime: new Date(), reloadRequestSequence: 2))
    }

    def "forced refresh stops tracking reload requests, and later requests are still newer than earlier node sets"() {
        given:
        service.nodeCache = Mock(LoadingCache)
        service.refreshProjectNodesInBackground('test1')
        def loadedBefore = new CachedProjectNodes(cacheTime: new Date(), reloadRequestSequence: service.lastReloadRequest('test1'))

        when:
        service.refreshProjectNodes('test1')

        then:
        service.lastReloadRequest('test1') == 0

        when: "the project is saved again, for example after being re-created with the same name"
        service.refreshProjectNodesInBackground('test1')

        then:
        service.lastReloadRequest('test1') > loadedBefore.reloadRequestSequence
    }

    def "project load threads config is exposed as a project property"() {
        expect:
        service.propertiesMapping['loadThreads'] == 'project.resources.loadThreads'
        service.projectConfigProperties.find { it.name == 'loadThreads' }.defaultValue == '1'
    }

    class PropsConfig implements IRundeckProjectConfig {
        Map<String, String> properties
        Map<String, String> projectProperties
        String name
        Date configLastModifiedTime


        @Override
        boolean hasProperty(final String key) {
            projectProperties.containsKey(key)
        }

        @Override
        Date getConfigCreatedTime() {
            return null
        }

        @Override
        String getProperty(final String property) {
            projectProperties.get(property)
        }
    }

    @Unroll
    def "get nodes when project cache #isenabled"(String isenabled, int expectedCount) {
        given:
        INodeSet nodeSet = new NodeSetImpl()
        nodeSet.putNode(new NodeEntryImpl('anode'))
        def properties = [
                'framework.var.dir'                                  : RESOURCE_TMP_DIR,
                'resources.source.1.type'                            : 'file',
                'resources.source.1.config.file'                     : '/tmp/test.xml',
                'resources.source.1.config.generateFileAutomatically': 'false',
                'resources.source.1.config.includeServerNode'        : 'true',

        ]
        if (null != isenabled) {
            properties['project.nodeCache.enabled'] = isenabled
        }
        def projConfig = new PropsConfig(
                projectProperties: properties,
                properties: properties,
                name: 'test1',
                configLastModifiedTime: new Date()
        )
        def modelSource = Mock(ResourceModelSource)
        def cacheModelsource = Mock(ResourceModelSource)

        def frameWorkService = Mock(FrameworkService)
        def pluginService = Mock(PluginService)
        def nodeSourceLoaderService =  new NodeSourceLoaderService()
        def projectManagerService = Mock(ProjectManagerService)

        nodeSourceLoaderService.pluginService = pluginService
        nodeSourceLoaderService.frameworkService = frameWorkService
        nodeSourceLoaderService.projectManagerService = projectManagerService
        nodeSourceLoaderService.rundeckSpiBaseServicesProvider = Mock(Services)

        service.nodeSourceLoaderService = nodeSourceLoaderService
        service.frameworkService = frameWorkService

        frameWorkService.getRundeckFramework() >> Mock(Framework) {
            getFrameworkProjectMgr() >> Mock(ProjectManager) {
                existsFrameworkProject('test1') >> true
                2 * loadProjectConfig('test1') >> projConfig
            }
            getResourceModelSourceService() >> Mock(ResourceModelSourceService) {
                _ * getCloseableSourceForConfiguration('file', {args->
                    args['file']!='/tmp/test.xml'
                }) >> Closeables.closeableProvider(cacheModelsource)
            }
            getResourceFormatGeneratorService()>>Mock(ResourceFormatGeneratorService){
                _ * getGeneratorForFormat('resourcexml')>>Mock(ResourceFormatGenerator){

                }
            }
        }
        def modelSourceFactory = Mock(ResourceModelSourceFactory)
        0 * pluginService.retainPlugin('file', _) >> Closeables.closeableProvider(modelSourceFactory)
        1 * pluginService.getRundeckPluginRegistry()>>Mock(PluginRegistry){
            1 * retainConfigurePluginByName('file', _, _, _) >>
            new ConfiguredPlugin(modelSourceFactory, [:], Closeables.closeableProvider(modelSourceFactory, null))
        }
        1 * modelSourceFactory.createResourceModelSource(_,{args->
            args['file']=='/tmp/test.xml'
        }) >> modelSource

        when:
        def result1 = service.getNodes('test1')
        def nodes1 = result1.getNodeSet()
        def result2 = service.getNodes('test1')
        def nodes2 = result2.getNodeSet()

        then:
        null != nodes1.getNode('anode')
        null != nodes2.getNode('anode')
        nodes1.nodeNames as List == ['anode']
        nodes2.nodeNames as List == ['anode']
        result1.doCache == ('false' != isenabled)
        expectedCount * modelSource.getNodes() >> nodeSet

        where:
        isenabled | expectedCount
        'true'    | 1
        'false'   | 3
        null      | 1
    }

    @Unroll
    def "get nodes when project cache with preload #defFirstLoadAsynch ignores config asynch"() {
        given:
        service.nodeTaskExecutor = Mock(AsyncTaskExecutor)
        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('nodeService.nodeCache.enabled', true) >> true
            getBoolean('nodeService.nodeCache.firstLoadAsynch', false) >> defFirstLoadAsynch
            0 * _(*_)
        }
        INodeSet preloadedNodes = new NodeSetImpl()
        preloadedNodes.putNode(new NodeEntryImpl('bnode'))

        def properties = [
                'framework.var.dir'                                  : RESOURCE_TMP_DIR,
                'resources.source.1.type'                            : 'file',
                'resources.source.1.config.file'                     : '/tmp/test.xml',
                'resources.source.1.config.generateFileAutomatically': 'false',
                'resources.source.1.config.includeServerNode'        : 'true',

        ]
        properties['project.nodeCache.enabled'] = 'true'
        def projConfig = new PropsConfig(
                projectProperties: properties,
                properties: properties,
                name: 'test1',
                configLastModifiedTime: new Date()
        )
        def modelSource = Mock(ResourceModelSource)
        def cacheModelsource = Mock(ResourceModelSource)

        service.frameworkService.getRundeckFramework() >> Mock(Framework) {
            getFrameworkProjectMgr() >> Mock(ProjectManager) {
                existsFrameworkProject('test1') >> true
                1 * loadProjectConfig('test1') >> projConfig
            }
            getResourceModelSourceService() >> Mock(ResourceModelSourceService) {
                _ * getSourceForConfiguration('file', {args->
                    args['file']=='/tmp/test.xml'
                }) >> modelSource
                _ * getSourceForConfiguration('file', {args->
                    args['file']!='/tmp/test.xml'
                }) >> cacheModelsource
            }
            getResourceFormatGeneratorService()>>Mock(ResourceFormatGeneratorService){
                _ * getGeneratorForFormat('resourcexml')>>Mock(ResourceFormatGenerator){

                }
            }
        }
        when:
        def result1 = service.getNodes('test1')
        def nodes1 = result1.getNodeSet()
        def result2 = service.getNodes('test1')
        def nodes2 = result2.getNodeSet()

        then:
        null != nodes1.getNode('bnode')
        null != nodes2.getNode('bnode')
        nodes1.nodeNames as List == ['bnode']
        nodes2.nodeNames as List == ['bnode']
        result1.doCache == true
        1 * service.nodeTaskExecutor.execute(_) >> {args->
            //no op. do not call closure, simulates delay in loading nodes
        }
        0 * modelSource.getNodes()
        1 * cacheModelsource.getNodes() >> preloadedNodes

        where:
        defFirstLoadAsynch | _
        true | _
        false | _

    }

    @Unroll
    def "get nodes with first load asynch defAsynch project value projAsynch"() {
        given:
        service.nodeTaskExecutor = Mock(AsyncTaskExecutor)
        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('nodeService.nodeCache.enabled', true) >> true
            getBoolean('nodeService.nodeCache.firstLoadAsynch', false) >> defAsynch
            0 * _(*_)
        }
        INodeSet preloadedNodes = new NodeSetImpl()
        INodeSet sourceNodes = new NodeSetImpl()
        sourceNodes.putNode(new NodeEntryImpl("bnode"))

        def properties = [
            'framework.var.dir'                                  : RESOURCE_TMP_DIR,
            'resources.source.1.type'                            : 'file',
            'resources.source.1.config.file'                     : '/tmp/test.xml',
            'resources.source.1.config.generateFileAutomatically': 'false',
            'resources.source.1.config.includeServerNode'        : 'true',

        ]
        properties['project.nodeCache.enabled'] = 'true'
        if (null != projSynch) {
            properties['project.nodeCache.firstLoadSynch'] = projSynch.toString()
        }
        def projConfig = new PropsConfig(
            projectProperties: properties,
            properties: properties,
            name: 'test1',
            configLastModifiedTime: new Date()
        )
        def modelSource = Mock(ResourceModelSource)
        def cacheModelsource = Mock(ResourceModelSource)
        def modelSourceFactory = Mock(ResourceModelSourceFactory)

        def frameWorkService = Mock(FrameworkService)
        def pluginService = Mock(PluginService)
        def nodeSourceLoaderService =  new NodeSourceLoaderService()
        def projectManagerService = Mock(ProjectManagerService)

        nodeSourceLoaderService.pluginService = pluginService
        nodeSourceLoaderService.frameworkService = frameWorkService
        nodeSourceLoaderService.projectManagerService = projectManagerService
        nodeSourceLoaderService.rundeckSpiBaseServicesProvider = Mock(Services)

        service.nodeSourceLoaderService = nodeSourceLoaderService
        service.frameworkService = frameWorkService


        frameWorkService.getRundeckFramework() >> Mock(Framework) {
            getFrameworkProjectMgr() >> Mock(ProjectManager) {
                existsFrameworkProject('test1') >> true
                loadProjectConfig('test1') >> projConfig
            }
            getResourceModelSourceService() >> Mock(ResourceModelSourceService) {
                _ * getSourceForConfiguration(
                    'file', { args ->
                    args['file'] == '/tmp/test.xml'
                }
                ) >> modelSource
                _ * getSourceForConfiguration(
                    'file', { args ->
                    args['file'] != '/tmp/test.xml'
                }
                ) >> cacheModelsource
            }
            getResourceFormatGeneratorService() >> Mock(ResourceFormatGeneratorService) {
                _ * getGeneratorForFormat('resourcexml') >> Mock(ResourceFormatGenerator) {

                }
            }
        }

        pluginService.getRundeckPluginRegistry()>>Mock(PluginRegistry){
            retainConfigurePluginByName('file', _, _, _) >>
                    new ConfiguredPlugin(modelSourceFactory, [:], Closeables.closeableProvider(modelSourceFactory, null))
        }
        modelSourceFactory.createResourceModelSource(_,{args->
            args['file']=='/tmp/test.xml'
        }) >> modelSource

        when:
        def result1 = service.getNodes('test1')

        then:
        //cached first-run
        if (expectAsynch) {
            assert result1.nodes.nodeNames as List == []
        } else {
            assert result1.nodes != null
            assert null != result1.nodes.getNode('bnode')
            assert result1.nodes.nodeNames as List == ['bnode']
        }

        result1.doCache == true
        (expectAsynch ? 1 : 0) * service.nodeTaskExecutor.execute(_) >> { args ->
            //no op. do not call closure, simulates delay in loading nodes
        }
        (expectAsynch ? 0 : 1) * modelSource.getNodes() >> sourceNodes
        1 * cacheModelsource.getNodes() >> preloadedNodes

        where:
        defAsynch | projSynch | expectAsynch
        false     | null      | false
        true      | null      | true
        false     | true     | false
        false     | false      | true
        true      | true     | false
        true      | false      | true

    }

    def "get nodes with project cache without preload with forced synchronous behavior"( ) {
        given:
        service.nodeTaskExecutor = Mock(AsyncTaskExecutor)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('nodeService.nodeCache.enabled', true) >> true
            getBoolean('nodeService.nodeCache.firstLoadAsynch', false) >> false
            0 * _(*_)
        }
        INodeSet modelNodes = new NodeSetImpl()
        modelNodes.putNode(new NodeEntryImpl('anode'))

        def properties = [
                'framework.var.dir'                                  : RESOURCE_TMP_DIR,
                'resources.source.1.type'                            : 'file',
                'resources.source.1.config.file'                     : '/tmp/test.xml',
                'resources.source.1.config.generateFileAutomatically': 'false',
                'resources.source.1.config.includeServerNode'        : 'true',

        ]
        properties['project.nodeCache.enabled'] = 'true'
        def projConfig = new PropsConfig(
                projectProperties: properties,
                properties: properties,
                name: 'test1',
                configLastModifiedTime: new Date()
        )
        def modelSource = Mock(ResourceModelSource)
        def modelSourceFactory = Mock(ResourceModelSourceFactory)
        def cacheModelsource = Mock(ResourceModelSource)

        def frameWorkService = Mock(FrameworkService)
        def pluginService = Mock(PluginService)
        def nodeSourceLoaderService =  new NodeSourceLoaderService()
        def projectManagerService = Mock(ProjectManagerService)

        nodeSourceLoaderService.pluginService = pluginService
        nodeSourceLoaderService.frameworkService = frameWorkService
        nodeSourceLoaderService.projectManagerService = projectManagerService
        nodeSourceLoaderService.rundeckSpiBaseServicesProvider = Mock(Services)

        service.nodeSourceLoaderService = nodeSourceLoaderService
        service.frameworkService = frameWorkService

        frameWorkService.getRundeckFramework() >> Mock(Framework) {
            getFrameworkProjectMgr() >> Mock(ProjectManager) {
                existsFrameworkProject('test1') >> true
                2 * loadProjectConfig('test1') >> projConfig
            }
            getResourceModelSourceService() >> Mock(ResourceModelSourceService) {

                _ * getSourceForConfiguration('file', {args->
                    args['file']!='/tmp/test.xml'
                }) >> cacheModelsource
            }
            getResourceFormatGeneratorService()>>Mock(ResourceFormatGeneratorService){
                _ * getGeneratorForFormat('resourcexml')>>Mock(ResourceFormatGenerator){

                }
            }
        }
        0 * pluginService.retainPlugin('file', _) >> Closeables.closeableProvider(modelSourceFactory)
        1 * pluginService.getRundeckPluginRegistry()>>Mock(PluginRegistry){
            1 * retainConfigurePluginByName('file', _, _, _) >>
            new ConfiguredPlugin(modelSourceFactory, [:], Closeables.closeableProvider(modelSourceFactory, null))
        }
        1 * modelSourceFactory.createResourceModelSource(_,{args->
            args['file']=='/tmp/test.xml'
        }) >> modelSource

        when:
        def result1 = service.getNodes('test1')
        def nodes1 = result1.getNodeSet()
        def result2 = service.getNodes('test1')
        def nodes2 = result2.getNodeSet()

        then:
        null != nodes1.getNode('anode')
        null != nodes2.getNode('anode')
        nodes1.nodeNames as List == ['anode']
        nodes2.nodeNames as List == ['anode']
        result1.doCache == true
        0 * service.nodeTaskExecutor.execute(_)>>{
            //never load model nodes
        }
        1 * modelSource.getNodes() >> modelNodes
        1 * cacheModelsource.getNodes() >> null

    }

    def "get nodes with project cache without preload with forced asynchronous behavior"( ) {
        given:
        service.nodeTaskExecutor = Mock(AsyncTaskExecutor)
        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService) {
            getBoolean('nodeService.nodeCache.enabled', true) >> true
            getBoolean('nodeService.nodeCache.firstLoadAsynch', false) >> true
            0 * _(*_)
        }
        INodeSet modelNodes = new NodeSetImpl()
        modelNodes.putNode(new NodeEntryImpl('anode'))

        def properties = [
                'framework.var.dir'                                  : RESOURCE_TMP_DIR,
                'resources.source.1.type'                            : 'file',
                'resources.source.1.config.file'                     : '/tmp/test.xml',
                'resources.source.1.config.generateFileAutomatically': 'false',
                'resources.source.1.config.includeServerNode'        : 'true',

        ]
        properties['project.nodeCache.enabled'] = 'true'
        def projConfig = new PropsConfig(
                projectProperties: properties,
                properties: properties,
                name: 'test1',
                configLastModifiedTime: new Date()
        )
        def modelSource = Mock(ResourceModelSource)
        def cacheModelsource = Mock(ResourceModelSource)

        service.frameworkService.getRundeckFramework() >> Mock(Framework) {
            getFrameworkProjectMgr() >> Mock(ProjectManager) {
                existsFrameworkProject('test1') >> true
                1 * loadProjectConfig('test1') >> projConfig
            }
            getResourceModelSourceService() >> Mock(ResourceModelSourceService) {
                _ * getSourceForConfiguration('file', {args->
                    args['file']=='/tmp/test.xml'
                }) >> modelSource
                _ * getSourceForConfiguration('file', {args->
                    args['file']!='/tmp/test.xml'
                }) >> cacheModelsource
            }
            getResourceFormatGeneratorService()>>Mock(ResourceFormatGeneratorService){
                _ * getGeneratorForFormat('resourcexml')>>Mock(ResourceFormatGenerator){

                }
            }
        }
        when:
        def result1 = service.getNodes('test1')
        def nodes1 = result1.getNodeSet()
        def result2 = service.getNodes('test1')
        def nodes2 = result2.getNodeSet()

        then:
        null == nodes1
        null == nodes2
        result1.doCache == true
        1 * service.nodeTaskExecutor.execute(_)>>{
            //never load model nodes
        }
        0 * modelSource.getNodes() >> modelNodes
        1 * cacheModelsource.getNodes() >> null

    }

    def "project nodecache delay"(String confval, long expected) {
        given:
        def properties = [:]
        if (null != confval) {
            properties['project.nodeCache.delay'] = confval
        }
        def config = new PropsConfig(name: 'test1', properties: properties, projectProperties: properties)

        when:
        def result = service.projectNodeCacheDelayConfig(config)

        then:
        result == expected

        where:
        confval | expected
        '10'    | 10000
        null    | 30000
    }

    def "project nodecache enabled"(String confval, boolean expected) {
        given:
        def properties = [:]
        if (null != confval) {
            properties['project.nodeCache.enabled'] = confval
        }
        def config = new PropsConfig(name: 'test1', properties: properties, projectProperties: properties)

        when:
        def result = service.projectNodeCacheEnabledConfig(config)

        then:
        result == expected

        where:
        confval | expected
        'true'  | true
        'false' | false
        null    | true
    }

    def "is cache enabled for project"(String confval, boolean globalconfval, boolean expected) {
        given:
        def properties = [:]
        if (null != confval) {
            properties['project.nodeCache.enabled'] = confval
        }
        def config = new PropsConfig(name: 'test1', properties: properties, projectProperties: properties)

        service.configurationService = Mock(ConfigurationService) {
            getBoolean('nodeService.nodeCache.enabled', true) >> globalconfval
        }

        when:
        def result = service.isCacheEnabled(config)

        then:
        result == expected

        where:
        confval | globalconfval | expected
        'true'  | true          | true
        'true'  | false         | false
        'false' | true          | false
        'false' | false         | false
        null    | true          | true
        null    | false         | false
    }
}
