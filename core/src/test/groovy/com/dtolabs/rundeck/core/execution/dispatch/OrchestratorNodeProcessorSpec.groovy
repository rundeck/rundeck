package com.dtolabs.rundeck.core.execution.dispatch

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl
import com.dtolabs.rundeck.plugins.orchestrator.Orchestrator
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by greg on 4/9/15.
 */
class OrchestratorNodeProcessorSpec extends Specification {

    def setup() {

    }

    def teardown() {
    }

    def "no nodes"() {
        given:
        def orchestrator = new Orchestrator() {
            @Override
            INodeEntry nextNode() {
                return null
            }

            @Override
            void returnNode(final INodeEntry node, boolean success, NodeStepResult result) {

            }
            @Override
            boolean isComplete() {
                return true
            }
        }
        Map<INodeEntry, Callable<NodeStepResult>> executions = new HashMap<>()

        def onp = new OrchestratorNodeProcessor(1, false, orchestrator, executions)

        when:
        def result = onp.execute()

        then:
        result
    }

    def "single thread"() {
        given:
        def node1 = new NodeEntryImpl("node1")
        def node2 = new NodeEntryImpl("node2")
        def sent = [node1, node2]
        def returned = []

        def orchestrator = new Orchestrator() {
            @Override
            INodeEntry nextNode() {
                return sent.size() > 0 ? sent.remove(0) : null
            }

            @Override
            void returnNode(final INodeEntry node, boolean success, NodeStepResult result) {
                returned << node
            }
            @Override
            boolean isComplete() {
                return sent.size()==0
            }
        }
        Map<INodeEntry, Callable<NodeStepResult>> executions = new HashMap<>()
        sent.each{node->
            executions.put(node, { -> new NodeStepResultImpl(node) })
        }

        def onp = new OrchestratorNodeProcessor(1, false, orchestrator, executions)

        when:
        def result = onp.execute()

        then:
        result
        returned == [node1, node2]
    }
    def "invalid threadcount"() {
        given:
        def node1 = new NodeEntryImpl("node1")
        def node2 = new NodeEntryImpl("node2")
        def sent = [node1, node2]
        def returned = []

        def orchestrator = new Orchestrator() {
            @Override
            INodeEntry nextNode() {
                return sent.size() > 0 ? sent.remove(0) : null
            }

            @Override
            void returnNode(final INodeEntry node, boolean success, NodeStepResult result) {
                returned << node
            }
            @Override
            boolean isComplete() {
                return sent.size()==0
            }
        }
        Map<INodeEntry, Callable<NodeStepResult>> executions = new HashMap<>()
        sent.each{node->
            executions.put(node, { -> new NodeStepResultImpl(node) })
        }

        when:
        def onp = new OrchestratorNodeProcessor(-12, false, orchestrator, executions)

        then:
        IllegalArgumentException e = thrown()
        e.message.startsWith('threadCount must be greater than 0')
    }

    def "single thread fewer nodes"() {
        given:
        def node1 = new NodeEntryImpl("node1")
        def node2 = new NodeEntryImpl("node2")
        def sent = [node1, node2]
        def returned = []

        def orchestrator = new Orchestrator() {
            @Override
            INodeEntry nextNode() {
                return sent.size() > 1 ? sent.remove(0) : null
            }

            @Override
            void returnNode(final INodeEntry node, boolean success, NodeStepResult result) {
                returned << node
            }
            @Override
            boolean isComplete() {
                return sent.size()==1
            }
        }
        Map<INodeEntry, Callable<NodeStepResult>> executions = new HashMap<>()
        sent.each{node->
            executions.put(node, { -> new NodeStepResultImpl(node) })
        }

        def onp = new OrchestratorNodeProcessor(1, false, orchestrator, executions)

        when:
        def result = onp.execute()

        then:
        result
        returned == [node1]
    }

    /**
     * one node in first batch, delay start for second batch
     * @return
     */
    def "single thread wait for nodes"() {
        given:
        def node1 = new NodeEntryImpl("node1")
        def node2 = new NodeEntryImpl("node2")
        def batch1 = [node1]
        def batch2 = [node2]
        def returned = []
        def batch=batch1
        def complete = new CountDownLatch(1)
        def node1return = new CountDownLatch(1)
        def node2return = new CountDownLatch(2)
        def orchestrator = new Orchestrator() {
            @Override
            INodeEntry nextNode() {
                return batch.size() > 0 ? batch.remove(0) : null
            }

            @Override
            void returnNode(final INodeEntry node, boolean success, NodeStepResult result) {
                returned << node
                node2return.countDown()
                node1return.countDown()
            }
            @Override
            boolean isComplete() {
                return returned.size()==2
            }
        }
        def ran=[]
        Map<INodeEntry, Callable<NodeStepResult>> executions = new HashMap<>()
        batch1.each{node->
            executions.put(node, { ->
                ran<<node
                new NodeStepResultImpl(node)
            })
        }
        batch2.each{node->
            executions.put(node, { ->
                ran<<node
                new NodeStepResultImpl(node)
            })
        }

        def onp = new OrchestratorNodeProcessor(1, false, orchestrator, executions)

        expect:
        def result = false

        //start processor in other thread
        new Thread({
            result = onp.execute()
            complete.countDown()
        }
        ).start()

        node1return.await(10, TimeUnit.SECONDS)
        //first node has been returned
        returned==[node1]
        ran==[node1]

        batch1.addAll(batch2)
        node2return.await(10, TimeUnit.SECONDS)

        complete.await(10,TimeUnit.SECONDS)
        result
        returned == [node1,node2]
        ran == [node1,node2]
    }

    /**
     * Two nodes in 1 batch run on two threads
     * @return
     */
    def "multi thread"() {
        given:
        def node1 = new NodeEntryImpl("node1")
        def node2 = new NodeEntryImpl("node2")
        def batch1 = [node1, node2]
        def returned = []
        def callablesReady = new CountDownLatch(2)
        def proceed = new CountDownLatch(1)
        def finish = new CountDownLatch(1)
        def runningcount = new AtomicInteger(0)

        def orchestrator = new Orchestrator() {
            @Override
            INodeEntry nextNode() {
                return batch1.size() > 0 ? batch1.remove(0) : null
            }

            @Override
            void returnNode(final INodeEntry node, boolean success, NodeStepResult result) {
                returned << node
            }

            @Override
            boolean isComplete() {
                return batch1.size()==0
            }
        }
        Map<INodeEntry, Callable<NodeStepResult>> executions = new HashMap<>()

        batch1.each { node ->
            executions.put(node, { ->
                //increment running threads
                runningcount.incrementAndGet()
                //send ready
                callablesReady.countDown()
                //wait for proceed
                proceed.await(10, TimeUnit.SECONDS)
                //finish
                new NodeStepResultImpl(node)
            })
        }

        def onp = new OrchestratorNodeProcessor(2, false, orchestrator, executions)

        expect:

        def result = false

        //start processor in other thread
        new Thread({
            result = onp.execute()
            //send finish
            finish.countDown()
        }
        ).start()

        //wait for callables ready
        callablesReady.await(10, TimeUnit.SECONDS)
        //assert running thread count
        runningcount.intValue() == 2

        //send proceed
        proceed.countDown()

        //wait for finish
        finish.await(10, TimeUnit.SECONDS)

        result
        returned.contains(node1)
        returned.contains(node2)
    }

    /**
     * two batches of nodes on multiple threads
     */
    def "multi thread multibatch"() {
        given:
        def node1 = new NodeEntryImpl("node1")
        def node2 = new NodeEntryImpl("node2")
        def node3 = new NodeEntryImpl("node3")
        def node4 = new NodeEntryImpl("node4")
        def node5 = new NodeEntryImpl("node5")
        def batch1 = [node1, node2]
        def batch2 = [node3, node4, node5]
        def returned = []
        def callablesReady = new CountDownLatch(batch1.size())
        def callablesReady2 = new CountDownLatch(batch2.size())
        def proceed = new CountDownLatch(1)
        def proceed2 = new CountDownLatch(1)
        def finish = new CountDownLatch(1)
        def batch2Ready = new CountDownLatch(1)
        def runningcount = new AtomicInteger(0)
        def runningcount2 = new AtomicInteger(0)


        def batch = batch1
        def orchestrator = new Orchestrator() {
            @Override
            INodeEntry nextNode() {
                return batch.size() > 0 ? batch.remove(0) : null
            }

            @Override
            synchronized void returnNode(final INodeEntry node, boolean success, NodeStepResult result) {
                returned << node
                if (returned.size() == 2) {
                    batch = batch2
                    //send batch2 ready
                    batch2Ready.countDown()
                }
            }

            @Override
            boolean isComplete() {
                return returned.size() == 5
            }
        }
        Map<INodeEntry, Callable<NodeStepResult>> executions = new HashMap<>()

        batch1.each { node ->
            executions.put(node, { ->
                //increment running threads
                runningcount.incrementAndGet()
                //send ready
                callablesReady.countDown()
                //wait for proceed
                proceed.await(10, TimeUnit.SECONDS)
                //finish
                new NodeStepResultImpl(node)
            })
        }

        batch2.each{node->
            //batch 2
            executions.put(node, { ->
                //increment running threads
                runningcount2.incrementAndGet()
                //send ready2
                callablesReady2.countDown()
                //wait for proceed2
                proceed2.await(10, TimeUnit.SECONDS)
                //finish
                new NodeStepResultImpl(node)
            })
        }

        def onp = new OrchestratorNodeProcessor(3, false, orchestrator, executions)

        expect:

        def result = false

        new Thread({
            result = onp.execute()
            finish.countDown()
        } ).start()


        //wait for callables ready
        callablesReady.await(10, TimeUnit.SECONDS)
        //assert running thread count 2
        runningcount.intValue() == 2

        //send proceed
        proceed.countDown()


        //wait for batch2
        batch2Ready.await(10, TimeUnit.SECONDS)

        //wait for callables batch2 ready
        callablesReady2.await(10, TimeUnit.SECONDS)
        //assert running thread count 3
        runningcount2.intValue() == 3
        proceed2.countDown()

        finish.await(60, TimeUnit.SECONDS)

        result
        returned.containsAll(batch1)
        returned.containsAll(batch2)
    }
}
