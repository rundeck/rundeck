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
import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

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
        def properties = ['project.resources.file': '/tmp/test.xml']

        def projConfig = new PropsConfig(
                projectProperties: properties,
                properties: properties,
                name: 'test1',
                configLastModifiedTime: new Date()
        )
        def modelSource=Mock(ResourceModelSource) {
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
    class PropsConfig implements IRundeckProjectConfig{
        Map<String,String> properties
        Map<String,String> projectProperties
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
        if(null!=isenabled){
            properties['project.nodeCache.enabled']= isenabled
        }
        def projConfig = new PropsConfig(
                projectProperties: properties,
                properties: properties,
                name: 'test1',
                configLastModifiedTime: new Date()
        )
        def modelSource = Mock(ResourceModelSource)

        service.frameworkService.getRundeckFramework() >> Mock(Framework) {
            getProjectManager() >> Mock(ProjectManager) {
                existsFrameworkProject('test1') >> true
                1*loadProjectConfig('test1') >> projConfig
            }
            getResourceModelSourceService() >> Mock(ResourceModelSourceService) {
                _ * getSourceForConfiguration('file', _) >> modelSource
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
        result1.doCache==('false'!=isenabled)
        expectedCount * modelSource.getNodes() >> nodeSet

        where:
        isenabled | expectedCount
        'true'    | 1
        'false'   | 3
        null      | 1
    }
}
