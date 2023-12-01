package com.dtolabs.rundeck.core.execution.TestExectuionContextImpl

import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.nodes.ProjectNodeService
import spock.lang.Specification

class ExecutionContextImplSpec extends Specification {

    def "single node context should correctly set data context and node properties"() {
        given:
        def ctx1 = [test: 'value'] as Map<String, String>
        def map = [ctx1: ctx1] as Map<String, Map<String, String>>
        def testNode = new NodeEntryImpl('testNode')
        testNode.setDescription("desc 1");
        testNode.setHostname("host1");
        testNode.setUsername("user1");

        when:
        ExecutionContextImpl imp = ExecutionContextImpl.builder().dataContext(map).build()
        ExecutionContextImpl imp2 = ExecutionContextImpl.builder(imp).singleNodeContext(testNode, true).build()

        then:
        imp.dataContext != null
        imp.dataContext.get("node") == null
        imp.dataContext.get("ctx1").get("test") == 'value'

        imp2.dataContext != null
        imp2.dataContext.get("node").get("description") == 'desc 1'
        imp2.dataContext.get("node").get("hostname") == 'host1'
        imp2.dataContext.get("node").get("name") == 'testNode'
        imp2.dataContext.get("ctx1").get("test") == 'value'
    }

    def "ExecutionContext builder should create an ExecutionContext with a node service"() {
        given:
        def ctx1 = [test: 'value'] as Map<String, String>
        def map = [ctx1: ctx1] as Map<String, Map<String, String>>
        def pns = Mock(ProjectNodeService)

        when:
        ExecutionContextImpl imp = ExecutionContextImpl.builder().dataContext(map).nodeService(pns).build()

        then:
        imp.nodeService != null
    }
}