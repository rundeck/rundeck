package com.dtolabs.rundeck.core.execution

import com.dtolabs.rundeck.core.common.NodeEntryImpl
import spock.lang.Specification

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
}
