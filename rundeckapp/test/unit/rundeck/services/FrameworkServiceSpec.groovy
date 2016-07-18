package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * Created by greg on 8/14/15.
 */
@TestFor(FrameworkService)
class FrameworkServiceSpec extends Specification {
    def "summarize tags in nodeset"(){
        given:
        List<INodeEntry> n = nodeList([name:'a',tags:['x','y','z']],
                        [name:'z',tags:['y','a']],
                        [name:'x'])
        when:
        def result=service.summarizeTags(n)

        then:
        n.size()==3
        result == ['x': 1, 'y': 2, 'z': 1, 'a': 1]

    }

    List<INodeEntry> nodeList(Map<String, ?>... maps ) {
        maps.collect{
            def n=new NodeEntryImpl(it.name)
            if(it.tags){
                n.getTags().addAll(it.tags)
            }
            n
        }
    }

    def "get default input charset for project"() {
        given:
        String project = 'aproject'
        def manager = Mock(ProjectManager)
        service.rundeckFramework = Mock(Framework) {
            getFrameworkProjectMgr() >> manager
        }

        when:
        def result = service.getDefaultInputCharsetForProject(project)


        then:
        result == expected
        1 * manager.loadProjectConfig(project) >> Mock(IRundeckProjectConfig) {
            hasProperty(('framework.' + FrameworkService.REMOTE_CHARSET)) >> (fwk ? true : false)
            getProperty(('framework.' + FrameworkService.REMOTE_CHARSET)) >> fwk
            hasProperty(('project.' + FrameworkService.REMOTE_CHARSET)) >> (proj ? true : false)
            getProperty(('project.' + FrameworkService.REMOTE_CHARSET)) >> proj
        }

        where:
        fwk          | proj         | expected     | _
        null         | null         | null         | _
        'UTF-8'      | null         | 'UTF-8'      | _
        null         | 'UTF-8'      | 'UTF-8'      | _
        'UTF-8'      | 'UTF-8'      | 'UTF-8'      | _
        'ISO-8859-2' | 'UTF-8'      | 'UTF-8'      | _
        'UTF-8'      | 'ISO-8859-2' | 'ISO-8859-2' | _


    }
}
