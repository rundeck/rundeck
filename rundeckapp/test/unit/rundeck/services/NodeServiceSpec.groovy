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

/**
 * Created by greg on 2/3/16.
 */
@TestFor(NodeService)
class NodeServiceSpec extends Specification {
    def "get nodes project DNE"(){
        given:
        service.frameworkService=Mock(FrameworkService)
        when:
        def result = service.getNodes('test1')


        then:
        1*service.frameworkService.getRundeckFramework()>>Mock(Framework){
            1*getProjectManager()>>Mock(ProjectManager){
                1*existsFrameworkProject('test1')>>false
            }
        }
        IllegalArgumentException e = thrown()
        e.message=='Project does not exist: test1'
    }
    def "get nodes project exists"(){
        given:
        service.frameworkService=Mock(FrameworkService)
        service.configurationService=Mock(ConfigurationService){
            getCacheEnabledFor('nodeService','nodeCache', true)>>true
        }
        INodeSet nodeSet = new NodeSetImpl()
        nodeSet.putNode(new NodeEntryImpl('anode'))
        def properties = ['project.resources.file': '/tmp/test.xml']
        when:
        def result = service.getNodes('test1')


        then:
        service.frameworkService.getRundeckFramework()>>Mock(Framework){
            getProjectManager()>>Mock(ProjectManager){
                existsFrameworkProject('test1')>>true
                loadProjectConfig('test1')>>Mock(IRundeckProjectConfig){
                    getName()>>'test1'
                    hasProperty(_)>>{args-> properties.containsKey(args[0])}
                    getProperty(_)>>{args-> properties.get(args[0])}
                    getProperties()>> properties
                }
            }
            getResourceModelSourceService()>>Mock(ResourceModelSourceService){
                1 * getSourceForConfiguration('file',_)>>Mock(ResourceModelSource){
                    getNodes()>>nodeSet
                }
            }
        }
        null != result.nodeSet.getNode('anode')
        result.nodeSet.nodeNames as List==['anode']
    }
}
