package com.dtolabs.rundeck.core.execution

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.NodesSelector
import com.dtolabs.rundeck.core.common.OrchestratorConfig
import com.dtolabs.rundeck.core.common.PluginControlService
import com.dtolabs.rundeck.core.common.SelectorUtils
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.workflow.SharedOutputContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener
import com.dtolabs.rundeck.core.jobs.JobService
import com.dtolabs.rundeck.core.logging.LoggingManager
import com.dtolabs.rundeck.core.nodes.ProjectNodeService
import com.dtolabs.rundeck.core.storage.StorageTree
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 8/1/17
 */
class ExecutionContextImplSpec extends Specification {
    def "original step context not modified"() {
        given:
        def orig = ExecutionContextImpl.builder()
                                       .stepContext([1, 2])
                                       .stepNumber(3)
                                       .build()


        when:
        def newctx = ExecutionContextImpl.builder(orig)
                                         .pushContextStep(4)
                                         .build()

        then:
        orig.stepContext == [1, 2]
        orig.stepNumber == 3

        newctx.stepContext == [1, 2, 3]
        newctx.stepNumber == 4
    }

    def "original node context not modified"() {
        given:
        def orig = ExecutionContextImpl.builder()
                                       .stepContext([1, 2])
                                       .stepNumber(3)
                                       .build()
        def nodea = new NodeEntryImpl("nodea.host", "nodea")
        nodea.setAttribute("zamboni", "thief")
        nodea.setAttribute("rapscallion", "balogna")
        def nodeContext = ExecutionContextImpl.builder(orig)
                                              .singleNodeContext(nodea, true)
                                              .build();

        def nodeb = new NodeEntryImpl("nodeb.host", "nodeb")
        nodeb.setAttribute("zamboni", "cheese")
        nodeb.setAttribute("rutabega", "turnip")

        when:
        def newctx = ExecutionContextImpl.builder(nodeContext)
                                         .singleNodeContext(nodeb, true)
                                         .build()

        then:
        orig.nodes.nodes.size() == 0
        nodeContext.nodes.nodes.size() == 1
        nodeContext.nodes.nodeNames == ['nodea'] as Set
        nodeContext.dataContext.node?.name == 'nodea'
        nodeContext.dataContext.node?.hostname == 'nodea.host'
        nodeContext.dataContext.node?.zamboni == 'thief'
        nodeContext.dataContext.node?.rapscallion == 'balogna'
        nodeContext.dataContext.node?.rutabega == null

        newctx.nodes.nodes.size() == 1
        newctx.nodes.nodeNames == ['nodeb'] as Set
        newctx.dataContext.node?.name == 'nodeb'
        newctx.dataContext.node?.hostname == 'nodeb.host'
        newctx.dataContext.node?.zamboni == 'cheese'
        newctx.dataContext.node?.rapscallion == null
        newctx.dataContext.node?.rutabega == 'turnip'
    }

    def "filtered node by selector"() {
        given:
        def orig = ExecutionContextImpl.builder()
                                       .stepContext([1, 2])
                                       .stepNumber(3)
                                       .build()
        final NodeSetImpl set1 = new NodeSetImpl();
        def nodea = new NodeEntryImpl("nodea.host", "nodea")
        def nodeb = new NodeEntryImpl("nodeb.host", "nodeb")

        set1.putNode(nodea)
        set1.putNode(nodeb)
        HashSet<String> failedNodeList = new HashSet<String>();
        failedNodeList.add("nodea")

        when:
        def nodeContext = ExecutionContextImpl.builder(orig)
                                         .nodeSelector(SelectorUtils.nodeList(failedNodeList))
                                         .nodes(set1)
                                         .build()

        then:
        orig.nodes.nodes.size() == 0
        nodeContext.nodes.nodes.size() == 2
        nodeContext.filteredNodes()
        nodeContext.filteredNodes().size() == 1
        nodeContext.filteredNodes().nodeNames?.equals(failedNodeList)
    }

    def "merge builder merges components"() {
        given:
            def ec1 = ExecutionContextImpl.builder()
                                          .addComponent('test1', 'Test1B', String)
                                          .addComponent('test2', 'Test2', String)
                                          .build()
            def ec2 = ExecutionContextImpl.builder()
                                          .addComponent('test1', 'Test1A', String)
                                          .build()
            def b1 = ExecutionContextImpl.builder(ec1)
            def b2 = ExecutionContextImpl.builder(ec2)
        when:
            b1.merge(b2)
            def result = b1.build()
        then:
            result.getComponentList().size() == 2
            result.componentsForType(String).size() == 2
            (result.componentsForType(String) as Set) == (['Test2', 'Test1A'] as Set)
    }

    def "merge builder merges components2"() {
        given:
            def ec1 = ExecutionContextImpl.builder()
                                          .addComponent('test1', 'Test1B', String)
                                          .addComponent('test2', 'Test2', String)
                                          .build()
            def ec2 = ExecutionContextImpl.builder()
                                          .addComponent('test1', 'Test1A', String)
                                          .addComponent('test3', 'Test3', String)
                                          .build()
            def b1 = ExecutionContextImpl.builder(ec1)
            def b2 = ExecutionContextImpl.builder(ec2)
        when:
            b2.merge(b1)
            def result = b2.build()
        then:
            result.getComponentList().size() == 3
            result.componentsForType(String).size() == 3
            (result.componentsForType(String) as Set) == (['Test2', 'Test1B', 'Test3'] as Set)
    }

    def "component of type"() {
        given:
            def ec1 = ExecutionContextImpl.builder()
                                          .addComponent('test1', 'Test1B', String)
                                          .addComponent('test2', false, Boolean)
                                          .build()
        when:
            def result = ec1.componentForType(type)
        then:
            result.isPresent()
            result.get() == expect
        where:
            type    | expect
            String  | 'Test1B'
            Boolean | false
    }

    def "components of type"() {
        given:
            def ec1 = ExecutionContextImpl.builder()
                                          .addComponent('test1', 'Test1B', String)
                                          .addComponent('test2', false, Boolean)
                                          .addComponent('test2b', true, Boolean)
                                          .addComponent('test3', 'Test2', String)
                                          .build()
        when:
            def result = ec1.componentsForType(type)
        then:
            result
            result.size() == 2
            result == expect
        where:
            type    | expect
            String  | ['Test1B', 'Test2']
            Boolean | [false, true]
    }

    @Unroll
    def "use single component of type"() {
        given:
            def ec1 = ExecutionContextImpl.builder()
                                          .addComponent('test1', expect, type, useOnce)
                                          .build()
        when:
            def result = ec1.useSingleComponentOfType(type)
            def result2 = ec1.componentForType(type)
        then:
            result
            result.present
            result.get() == expect
            result2.present == present
        where:
            type    | expect   | useOnce | present
            String  | 'Test1B' | true    | false
            String  | 'Test1B' | false   | true
            Boolean | false    | true    | false
            Boolean | false    | false   | true
    }

    @Unroll
    def "use all components of type"() {
        given:
            def ec1 = ExecutionContextImpl.builder()
                                          .addComponent('test1', 'Test1', String, useOnceA)
                                          .addComponent('test2', 'Test2', String, useOnceB)
                                          .build()
            def list = []
        when:
            def result = ec1.useAllComponentsOfType(type, list.&add)
            def result2 = ec1.componentsForType(type)
        then:
            result
            list == ['Test1', 'Test2']
            result2 == expect
        where:
            type   | expect             | useOnceA | useOnceB
            String | []                 | true     | true
            String | ['Test2']          | true     | false
            String | ['Test1']          | false    | true
            String | ['Test1', 'Test2'] | false    | false
    }

    def "merge builder merges settings"() {
        def selector1 = Mock(NodesSelector)
        def selector2 = Mock(NodesSelector)
        def nodeset1 = Mock(INodeSet)
        def nodeset2 = Mock(INodeSet)
        def execListener1 = Mock(ExecutionListener)
        def execListener2 = Mock(ExecutionListener)
        def wflistener1 = Mock(WorkflowExecutionListener)
        def wflistener2 = Mock(WorkflowExecutionListener)
        def execLogger1 = Mock(ExecutionLogger)
        def execLogger2 = Mock(ExecutionLogger)
        def fwk1 = Mock(Framework)
        def fwk2 = Mock(Framework)
        def auth1 = Mock(UserAndRolesAuthContext)
        def auth2 = Mock(UserAndRolesAuthContext)
        def storage1 = Mock(StorageTree)
        def storage2 = Mock(StorageTree)
        def jobservice1 = Mock(JobService)
        def jobservice2 = Mock(JobService)
        def nodeservice1 = Mock(ProjectNodeService)
        def nodeservice2 = Mock(ProjectNodeService)
        def outcontext1 = Mock(SharedOutputContext)
        def outcontext2 = Mock(SharedOutputContext)
        def logmanager1 = Mock(LoggingManager)
        def logmanager2 = Mock(LoggingManager)
        def plugincontrol1 = Mock(PluginControlService)
        def plugincontrol2 = Mock(PluginControlService)
        def node1 = new NodeEntryImpl('anode')
        def node2 = new NodeEntryImpl('bnode')
        def orc1 = new OrchestratorConfig('a', [b: 'c'])
        def orc2 = new OrchestratorConfig('b', [e: 'f'])
        def execRef1 = Mock(ExecutionReference)
        def execRef2 = Mock(ExecutionReference)
        def sharedctx1 = SharedDataContextUtils.sharedContext()
        sharedctx1.merge(ContextView.global(), DataContextUtils.context('data', [bob: 'data', blah: 'blee']))
        def sharedctx2 = SharedDataContextUtils.sharedContext()
        sharedctx2.merge(ContextView.global(), DataContextUtils.context('data', [bob: 'data2', blee: 'blah']))
        given:
            def b1 = ExecutionContextImpl.builder()
                                         .frameworkProject('AProject')
                                         .user('aUser')
                                         .nodeSelector(selector1)
                                         .loglevel(1)
                                         .nodes(nodeset1)
                                         .charsetEncoding('asdf')
                                         .dataContext(DataContextUtils.context('a', [b: 'c']))
                                         .privateDataContext([z: [q: 'r']])
                                         .executionListener(execListener1)
                                         .workflowExecutionListener(wflistener1)
                                         .executionLogger(execLogger1)
                                         .framework(fwk1)
                                         .authContext(auth1)
                                         .threadCount(1)
                                         .nodeRankAttribute('asdfattr')
                                         .nodeRankOrderAscending(false)
                                         .storageTree(storage1)
                                         .jobService(jobservice1)
                                         .nodeService(nodeservice1)
                                         .orchestrator(orc1)
                                         .outputContext(outcontext1)
                                         .sharedDataContext(sharedctx1)
                                         .loggingManager(logmanager1)
                                         .execution(execRef1)
//                                         .singleNodeContext(node1, false)
                                         .pluginControlService(plugincontrol1)


            def b2 = ExecutionContextImpl.builder()
                                         .frameworkProject('BProject')
                                         .user('bUser')
                                         .nodeSelector(selector2)
                                         .loglevel(2)
                                         .nodes(nodeset2)
                                         .charsetEncoding('bsdf')
                                         .dataContext(DataContextUtils.context('b', [c: 'd']))
                                         .privateDataContext([w: [h: 'j']])
                                         .executionListener(execListener2)
                                         .workflowExecutionListener(wflistener2)
                                         .executionLogger(execLogger2)
                                         .framework(fwk2)
                                         .authContext(auth2)
                                         .threadCount(2)
                                         .nodeRankAttribute('bsdfattr')
                                         .nodeRankOrderAscending(true)
                                         .storageTree(storage2)
                                         .jobService(jobservice2)
                                         .nodeService(nodeservice2)
                                         .orchestrator(orc2)
                                         .outputContext(outcontext2)
                                         .sharedDataContext(sharedctx2)
                                         .loggingManager(logmanager2)
                                         .execution(execRef2)
//                                         .singleNodeContext(node2, false)
                                         .pluginControlService(plugincontrol2)

        when:
            def result = b1.merge(b2).build()
        then:
            result.frameworkProject == 'BProject'
            result.user == 'bUser'
            result.nodeSelector == selector2
            result.loglevel == 2
            result.nodes == nodeset2
            result.charsetEncoding == 'bsdf'
            result.dataContext == [a: [b: 'c'], b: [c: 'd']]
            result.privateDataContext == [w: [h: 'j'], z: [q: 'r']]
            result.executionListener == execListener2
            result.workflowExecutionListener == (wflistener2)
            result.executionLogger == (execLogger2)
            result.framework == (fwk2)
            result.authContext == (auth2)
            result.threadCount == (2)
            result.nodeRankAttribute == ('bsdfattr')
            result.nodeRankOrderAscending == (true)
            result.storageTree == (storage2)
            result.jobService == (jobservice2)
            result.nodeService == (nodeservice2)
            result.orchestrator == (orc2)
            result.outputContext == (outcontext2)
            result.sharedDataContext.consolidate().getData(ContextView.global()).data == [bob: 'data2', blee: 'blah', blah: 'blee']
            result.loggingManager == (logmanager2)
            result.execution == (execRef2)
            result.singleNodeContext == null
            result.pluginControlService == (plugincontrol2)
    }
}
