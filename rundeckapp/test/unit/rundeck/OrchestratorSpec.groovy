package rundeck

import spock.lang.Specification

/**
 * Created by greg on 4/14/15.
 */
class OrchestratorSpec extends Specification {

    def "to map with config"(){
        given:
        def orch=new Orchestrator(type:'abc',configuration:['xyz':'123'])
        expect:
        orch.toMap()==[type:'abc',configuration:[xyz:'123']]
    }
    def "to map without config"(){
        given:
        def orch=new Orchestrator(type:'abc',configuration:null)
        expect:
        orch.toMap()==[type:'abc']
    }
    def "to map empty config"(){
        given:
        def orch=new Orchestrator(type:'abc',configuration:[:])
        expect:
        orch.toMap()==[type:'abc']
    }
    def "from map with configuration"(){
        given:
        def orch = Orchestrator.fromMap([type:'abc',configuration: ['xyz':'123']])
        expect:
        orch.type=='abc'
        orch.configuration==['xyz':'123']
        orch.content!=null
    }
    def "from map with non map"(){
        given:
        def orch = Orchestrator.fromMap([type:'abc',configuration: 'biscuits'])
        expect:
        orch.type=='abc'
        orch.configuration==[:]
        orch.content!=null
    }
    def "from map without map"(){
        given:
        def orch = Orchestrator.fromMap([type:'abc'])
        expect:
        orch.type=='abc'
        orch.configuration==[:]
        orch.content!=null
    }
}
