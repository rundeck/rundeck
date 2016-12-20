package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService
import grails.test.mixin.TestFor
import org.springframework.core.task.AsyncListenableTaskExecutor
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Future

/**
 * Created by greg on 2/3/16.
 */
@TestFor(NodeService)
class NodeServiceSpec extends Specification {
    def "get nodes project DNE"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        when:
        def result = service.getNodes('test1')


        then:
        1 * service.frameworkService.getRundeckFramework() >> Mock(Framework) {
            1 * getProjectManager() >> Mock(ProjectManager) {
                1 * existsFrameworkProject('test1') >> false
            }
        }
        IllegalArgumentException e = thrown()
        e.message == 'Project does not exist: test1'
    }

    def "get nodes project exists"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService) {
            getCacheEnabledFor('nodeService', 'nodeCache', true) >> true
        }
        INodeSet nodeSet = new NodeSetImpl()
        nodeSet.putNode(new NodeEntryImpl('anode'))
        def properties = ['project.resources.file': '/tmp/test.xml','project.nodeCache.enabled':'false']

        def projConfig = new PropsConfig(
                projectProperties: properties,
                properties: properties,
                name: 'test1',
                configLastModifiedTime: new Date()
        )
        def modelSource = Mock(ResourceModelSource) {
            getNodes() >> nodeSet
        }
        when:
        def result = service.getNodes('test1')
        def nodes1 = result.getNodeSet()


        then:
        service.frameworkService.getRundeckFramework() >> Mock(Framework) {
            getProjectManager() >> Mock(ProjectManager) {
                existsFrameworkProject('test1') >> true
                loadProjectConfig('test1') >> projConfig
            }
            getResourceModelSourceService() >> Mock(ResourceModelSourceService) {
                1 * getSourceForConfiguration('file', _) >> modelSource
            }
        }
        _ * modelSource.getNodes() >> nodeSet
        null != nodes1.getNode('anode')
        nodes1.nodeNames as List == ['anode']
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
        String getProperty(final String property) {
            projectProperties.get(property)
        }
    }

    @Unroll
    def "get nodes when project cache #isenabled"(String isenabled, int expectedCount) {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService) {
            getCacheEnabledFor('nodeService', 'nodeCache', true) >> true
        }
        INodeSet nodeSet = new NodeSetImpl()
        nodeSet.putNode(new NodeEntryImpl('anode'))
        def properties = [
                'project.resources.file': '/tmp/test.xml',

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

        service.frameworkService.getRundeckFramework() >> Mock(Framework) {
            getProjectManager() >> Mock(ProjectManager) {
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
                _ * getGeneratorForFormat('xml')>>Mock(ResourceFormatGenerator){

                }
            }
        }
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
    def "get nodes when project cache with preload ignores config asynch"( ) {
        given:
        service.nodeTaskExecutor = Mock(AsyncListenableTaskExecutor)
        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService) {
            getCacheEnabledFor('nodeService', 'nodeCache', true) >> true
            getBoolean('nodeService.nodeCache.firstLoadAsynch',true)>>defFirstLoadAsynch
        }
        INodeSet preloadedNodes = new NodeSetImpl()
        preloadedNodes.putNode(new NodeEntryImpl('bnode'))

        def properties = [
                'project.resources.file': '/tmp/test.xml',

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
            getProjectManager() >> Mock(ProjectManager) {
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
                _ * getGeneratorForFormat('xml')>>Mock(ResourceFormatGenerator){

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

    def "get nodes with project cache without preload with forced synchronous behavior"( ) {
        given:
        service.nodeTaskExecutor = Mock(AsyncListenableTaskExecutor)
        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService) {
            getCacheEnabledFor('nodeService', 'nodeCache', true) >> true
            getBoolean('nodeService.nodeCache.firstLoadAsynch',true)>>false
        }
        INodeSet modelNodes = new NodeSetImpl()
        modelNodes.putNode(new NodeEntryImpl('anode'))

        def properties = [
                'project.resources.file': '/tmp/test.xml',

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
            getProjectManager() >> Mock(ProjectManager) {
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
                _ * getGeneratorForFormat('xml')>>Mock(ResourceFormatGenerator){

                }
            }
        }
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
        service.nodeTaskExecutor = Mock(AsyncListenableTaskExecutor)
        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService) {
            getCacheEnabledFor('nodeService', 'nodeCache', true) >> true
            getBoolean('nodeService.nodeCache.firstLoadAsynch',true)>>true
        }
        INodeSet modelNodes = new NodeSetImpl()
        modelNodes.putNode(new NodeEntryImpl('anode'))

        def properties = [
                'project.resources.file': '/tmp/test.xml',

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
            getProjectManager() >> Mock(ProjectManager) {
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
                _ * getGeneratorForFormat('xml')>>Mock(ResourceFormatGenerator){

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
            getCacheEnabledFor('nodeService', 'nodeCache', true) >> globalconfval
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
