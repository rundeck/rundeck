package rundeck.services

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeEntryImpl
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
}
