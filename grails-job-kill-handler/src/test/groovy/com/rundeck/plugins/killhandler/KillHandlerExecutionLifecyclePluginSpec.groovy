package com.rundeck.plugins.killhandler

import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.NodeExecutionService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.execution.ExecutionLogger
import com.dtolabs.rundeck.core.jobs.JobExecutionEvent
import com.dtolabs.rundeck.core.jobs.JobEventResult
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.plugins.jobs.JobExecutionEventImpl
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.rundeck.app.spi.Services
import spock.lang.Specification
import spock.lang.Unroll

class KillHandlerExecutionLifecyclePluginSpec extends Specification {

    KillHandlerExecutionLifecyclePlugin plugin
    KillHandlerProcessTrackingService trackingService
    AuthorizedServicesProvider servicesProvider
    NodeExecutionService nodeExecutionService
    JobExecutionEvent event

    def setup() {
        plugin = new KillHandlerExecutionLifecyclePlugin()
        trackingService = Mock(KillHandlerProcessTrackingService)
        servicesProvider = Mock(AuthorizedServicesProvider)
        nodeExecutionService = Mock(NodeExecutionService)
        event = Mock(JobExecutionEvent)

        plugin.processTrackingService = trackingService
        plugin.rundeckAuthorizedServicesProvider = servicesProvider

        def services = Mock(Services)
        servicesProvider.getServicesWith(_ as UserAndRolesAuthContext) >> services
        services.getService(NodeExecutionService) >> nodeExecutionService
    }

    def "should kill processes on specific nodes when PIDs are tracked"() {
        given:
        def executionId = 123L
        def executionIdString = executionId.toString()
        def nodeName = "test-node"
        def pids = ["1234", "5678"]

        def node = Mock(INodeEntry)
        node.nodename >> nodeName
        node.osFamily >> "unix"

        def nodePidList = Mock(KillHandlerProcessTrackingService.NodePidList)
        nodePidList.pids >> pids

        def executionData = [(nodeName): nodePidList]

        setupEventMocks(executionId, executionData, [node], false, false)
        trackingService.getExecutionTrackData(executionIdString) >> executionData

        and: "mock the execution result"
        def mockResult = Mock(NodeExecutorResult) {
            toString() >> "Success"
        }

        when:
        plugin.afterJobEnds(event)

        then:
        1 * trackingService.flushExecution(executionIdString)
        1 * event.executionLogger.log(Constants.WARN_LEVEL, "Kill Handler processing tracked processes...")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing tracked processes on node 'test-node': 1234,5678")
        1 * nodeExecutionService.executeCommand(_, _, node) >> mockResult
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Result from killing processes attempt: Success")
    }

    def "should fallback to global node when no node-specific PIDs found"() {
        given:
        def executionId = 123L
        def executionIdString = executionId.toString()
        def nodeName = "test-node"
        def globalPids = ["9999", "8888"]

        def node = Mock(INodeEntry)
        node.nodename >> nodeName
        node.osFamily >> "unix"

        def globalPidList = Mock(KillHandlerProcessTrackingService.NodePidList)
        globalPidList.pids >> globalPids

        def executionData = [
                "__global": globalPidList
        ]

        setupEventMocks(executionId, executionData, [node], false, false)

        // Set up the tracking service to return the execution data
        trackingService.getExecutionTrackData(executionIdString) >> executionData

        // Create the mock result with proper toString behavior
        def mockResult = Mock(NodeExecutorResult)
        mockResult.toString() >> "Success"  // Define toString behavior separately

        // Set up the executeCommand to return the mock result
        nodeExecutionService.executeCommand(_ as ExecutionContext, _ as ExecArgList, node) >> mockResult

        when:
        plugin.afterJobEnds(event)

        then:
        1 * trackingService.flushExecution(executionIdString)
        1 * event.executionLogger.log(Constants.WARN_LEVEL, "Kill Handler processing tracked processes...")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "No PID data for 'test-node', falling back to '__global'")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing tracked processes on node 'test-node': 9999,8888")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Result from killing processes attempt: Success")
    }

    @Unroll
    def "should use correct kill command for #osFamily nodes"() {
        given:
        def executionId = 123L
        def executionIdString = executionId.toString()
        def nodeName = "test-node"
        def pids = ["1234", "5678"]

        def node = Mock(INodeEntry)
        node.nodename >> nodeName
        node.osFamily >> osFamily

        def nodePidList = Mock(KillHandlerProcessTrackingService.NodePidList)
        nodePidList.pids >> pids

        def executionData = [
                (nodeName): nodePidList
        ]

        setupEventMocks(executionId, executionData, [node], false, false)
        trackingService.getExecutionTrackData(executionIdString) >> executionData

        // Create mock result
        def mockResult = Mock(NodeExecutorResult)
        nodeExecutionService.executeCommand(_ as ExecutionContext, _ as ExecArgList, node) >> mockResult

        when:
        plugin.afterJobEnds(event)

        then:
        1 * trackingService.flushExecution(executionIdString)
        1 * event.executionLogger.log(Constants.WARN_LEVEL, "Kill Handler processing tracked processes...")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing tracked processes on node 'test-node': 1234,5678")
        // Use flexible matching for the result log message
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, { String message ->
            message.contains("Result from killing processes attempt:")
        })

        where:
        osFamily  | expectedCommand
        "unix"    | "kill -9 1234 5678"
        "linux"   | "kill -9 1234 5678"
        "windows" | "taskkill /PID 1234 /PID 5678 /T /F"
        "Windows" | "taskkill /PID 1234 /PID 5678 /T /F"
    }

    def "should kill child processes when killChilds is enabled for Unix nodes"() {
        given:
        plugin.killChilds = true
        def executionId = 123L
        def executionIdString = executionId.toString()
        def nodeName = "unix-node"
        def pids = ["1234", "5678"]

        def node = Mock(INodeEntry)
        node.nodename >> nodeName
        node.osFamily >> "unix"

        def nodePidList = Mock(KillHandlerProcessTrackingService.NodePidList)
        nodePidList.pids >> pids

        def executionData = [
                (nodeName): nodePidList
        ]

        setupEventMocks(executionId, executionData, [node], false, false)
        trackingService.getExecutionTrackData(executionIdString) >> executionData

        // Create mock result
        def mockResult = Mock(NodeExecutorResult)

        when:
        plugin.afterJobEnds(event)

        then:
        1 * trackingService.flushExecution(executionIdString)
        1 * event.executionLogger.log(Constants.WARN_LEVEL, "Kill Handler processing tracked processes...")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing tracked processes on node 'unix-node': 1234,5678")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing processes by session ID on node 'unix-node': 1234,5678")

        // First call - main kill command
        1 * nodeExecutionService.executeCommand(_, { ExecArgList args ->
            def argList = args.asFlatStringList()
            argList.any { it.contains("kill -9") && it.contains("1234") && it.contains("5678") }
        }, node) >> mockResult

        // Second call - child kill command
        1 * nodeExecutionService.executeCommand(_, { ExecArgList args ->
            def argList = args.asFlatStringList()
            argList.any { it.contains("pkill -SIGKILL -s") && it.contains("1234,5678") }
        }, node) >> mockResult

        // Two result log messages
        2 * event.executionLogger.log(Constants.DEBUG_LEVEL, { String message ->
            message.contains("Result from killing") && message.contains("processes attempt:")
        })
    }

    def "should not kill child processes for Windows nodes even when killChilds is enabled"() {
        given:
        plugin.killChilds = true
        def executionId = 123L
        def executionIdString = executionId.toString()
        def nodeName = "windows-node"
        def pids = ["1234", "5678"]

        def node = Mock(INodeEntry)
        node.nodename >> nodeName
        node.osFamily >> "windows"

        def nodePidList = Mock(KillHandlerProcessTrackingService.NodePidList)
        nodePidList.pids >> pids

        def executionData = [
                (nodeName): nodePidList
        ]

        setupEventMocks(executionId, executionData, [node], false, false)
        trackingService.getExecutionTrackData(executionIdString) >> executionData

        def mockResult = Mock(NodeExecutorResult) {
            toString() >> "Success"
        }

        when:
        plugin.afterJobEnds(event)

        then:
        1 * trackingService.flushExecution(executionIdString)
        1 * event.executionLogger.log(Constants.WARN_LEVEL, "Kill Handler processing tracked processes...")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing tracked processes on node 'windows-node': 1234,5678")
        // Only one call - main kill command
        1 * nodeExecutionService.executeCommand(_, { ExecArgList args ->
            def argList = args.asFlatStringList()
            argList.any { it.contains("taskkill") && it.contains("/PID 1234 /PID 5678 /T /F") }
        }, node) >> mockResult

        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Result from killing processes attempt: Success")

        // Should not execute pkill command for Windows
        0 * nodeExecutionService.executeCommand(_, { ExecArgList args ->
            def argList = args.asFlatStringList()
            argList.any { it.contains("pkill") }
        }, node)
    }

    def "should handle execution errors gracefully"() {
        given:
        def executionId = 123L
        def executionIdString = executionId.toString()
        def nodeName = "test-node"
        def pids = ["1234"]

        def node = Mock(INodeEntry)
        node.nodename >> nodeName
        node.osFamily >> "unix"

        def nodePidList = Mock(KillHandlerProcessTrackingService.NodePidList)
        nodePidList.pids >> pids

        def executionData = [
                (nodeName): nodePidList
        ]

        setupEventMocks(executionId, executionData, [node], false, false)

        // Set up the tracking service to return the execution data
        trackingService.getExecutionTrackData(executionIdString) >> executionData

        nodeExecutionService.executeCommand(_, _, node) >> { throw new RuntimeException("Command failed") }

        when:
        plugin.afterJobEnds(event)

        then:
        1 * trackingService.flushExecution(executionIdString)
        1 * event.executionLogger.log(Constants.ERR_LEVEL, { it.contains("Failed to kill processes on node 'test-node'") })
        noExceptionThrown()
    }

    def "should handle child process kill errors gracefully"() {
        given:
        plugin.killChilds = true
        def executionId = 123L
        def executionIdString = executionId.toString()
        def nodeName = "unix-node"
        def pids = ["1234"]

        def node = Mock(INodeEntry)
        node.nodename >> nodeName
        node.osFamily >> "unix"

        def nodePidList = Mock(KillHandlerProcessTrackingService.NodePidList)
        nodePidList.pids >> pids

        def executionData = [
                (nodeName): nodePidList
        ]

        setupEventMocks(executionId, executionData, [node], false, false)
        trackingService.getExecutionTrackData(executionIdString) >> executionData

        def mockResult = Mock(NodeExecutorResult) {
            toString() >> "Success"
        }

        // Set up the main kill command to succeed, but child kill to fail
        nodeExecutionService.executeCommand(_ as ExecutionContext, { ExecArgList args ->
            def argList = args.asFlatStringList()
            argList.any { it.contains("kill -9") }
        } as ExecArgList, node) >> mockResult

        nodeExecutionService.executeCommand(_ as ExecutionContext, { ExecArgList args ->
            def argList = args.asFlatStringList()
            argList.any { it.contains("pkill") }
        } as ExecArgList, node) >> { throw new RuntimeException("pkill failed") }

        when:
        plugin.afterJobEnds(event)

        then:
        1 * trackingService.flushExecution(executionIdString)
        1 * event.executionLogger.log(Constants.WARN_LEVEL, "Kill Handler processing tracked processes...")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing tracked processes on node 'unix-node': 1234")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Result from killing processes attempt: Success")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing processes by session ID on node 'unix-node': 1234")
        1 * event.executionLogger.log(Constants.ERR_LEVEL, { String message ->
            message.contains("Failed to kill processes on node 'unix-node'")
        })
        noExceptionThrown()
    }


    def "should skip processing when no execution tracking data exists"() {
        given:
        def executionId = 123L
        def executionIdString = executionId.toString()
        setupEventMocks(executionId, null, [], false, false)

        // Explicitly set up the tracking service to return null
        trackingService.getExecutionTrackData(executionIdString) >> null

        when:
        plugin.afterJobEnds(event)

        then:
        1 * event.executionLogger.log(Constants.WARN_LEVEL, 'No process ID captured, skipping...')
        0 * trackingService.flushExecution(_)
    }

    def "should skip node when no PIDs are available"() {
        given:
        def executionId = 123L
        def executionIdString = executionId.toString()
        def nodeName = "test-node"

        def node = Mock(INodeEntry)
        node.nodename >> nodeName
        node.osFamily >> "unix"

        def nodePidList = Mock(KillHandlerProcessTrackingService.NodePidList)
        nodePidList.pids >> null // No PIDs

        def executionData = [
                (nodeName): nodePidList
        ]

        setupEventMocks(executionId, executionData, [node], false, false)

        // Set up the tracking service to return the execution data
        trackingService.getExecutionTrackData(executionIdString) >> executionData

        when:
        plugin.afterJobEnds(event)

        then:
        1 * trackingService.flushExecution(executionIdString)
        1 * event.executionLogger.log(Constants.WARN_LEVEL, "Kill Handler processing tracked processes...")
        // Should not attempt to kill processes
        0 * nodeExecutionService.executeCommand(_, _, node)
    }

    def "should process multiple nodes with different PID data"() {
        given:
        def executionId = 123L
        def executionIdString = executionId.toString()

        def node1 = Mock(INodeEntry)
        node1.nodename >> "node1"
        node1.osFamily >> "unix"

        def node2 = Mock(INodeEntry)
        node2.nodename >> "node2"
        node2.osFamily >> "windows"

        def pidList1 = Mock(KillHandlerProcessTrackingService.NodePidList)
        pidList1.pids >> ["1111", "2222"]

        def pidList2 = Mock(KillHandlerProcessTrackingService.NodePidList)
        pidList2.pids >> ["3333", "4444"]

        def executionData = [
                "node1": pidList1,
                "node2": pidList2
        ]

        setupEventMocks(executionId, executionData, [node1, node2], false, false)
        trackingService.getExecutionTrackData(executionIdString) >> executionData

        //mock result with toString behavior
        def mockResult = Mock(NodeExecutorResult) {
            toString() >> "Success"
        }

        when:
        plugin.afterJobEnds(event)

        then:
        1 * trackingService.flushExecution(executionIdString)
        1 * event.executionLogger.log(Constants.WARN_LEVEL, "Kill Handler processing tracked processes...")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing tracked processes on node 'node1': 1111,2222")
        1 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing tracked processes on node 'node2': 3333,4444")

        // Expect the correct commands to be executed for each node
        1 * nodeExecutionService.executeCommand(_, { ExecArgList args ->
            def argList = args.asFlatStringList()
            argList.any { it.contains("kill -9 1111 2222") }
        }, node1) >> mockResult

        1 * nodeExecutionService.executeCommand(_, { ExecArgList args ->
            def argList = args.asFlatStringList()
            argList.any { it.contains("taskkill /PID 3333 /PID 4444 /T /F") }
        }, node2) >> mockResult

        2 * event.executionLogger.log(Constants.DEBUG_LEVEL, "Result from killing processes attempt: Success")
    }



    private void setupEventMocks(Long executionId, Map executionData, List nodes, boolean success, boolean aborted) {
        def executionLogger = Mock(ExecutionLogger)
        def executionContext = Mock(StepExecutionContext)
        def execution = Mock(ExecutionReference)
        def result = Mock(JobEventResult)
        def workflowResult = Mock(WorkflowExecutionResult)
        def nodeSet = Mock(INodeSet)

        // Configure the existing event mock
        event.execution >> execution
        event.result >> result
        event.nodes >> nodeSet
        event.executionLogger >> executionLogger
        event.executionContext >> executionContext

        // Configure execution reference - Return the execution ID as String
        execution.getId() >> executionId.toString()

        // Configure job event result
        result.getResult() >> workflowResult
        result.isAborted() >> aborted

        // Configure workflow execution result
        workflowResult.isSuccess() >> success

        // Configure execution context
        executionContext.getUserAndRolesAuthContext() >> Mock(UserAndRolesAuthContext)
        executionContext.getNodes() >> nodeSet

        // Configure the node set to return the nodes
        nodeSet.getNodes() >> nodes
        nodeSet.iterator() >> nodes.iterator()
    }
}