package com.rundeck.plugins.killhandler

import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.NodeExecutionService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleStatus
import com.dtolabs.rundeck.core.jobs.JobExecutionEvent
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.jobs.ExecutionLifecyclePlugin
import groovy.transform.CompileStatic
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Plugin(name = KillHandlerExecutionLifecyclePlugin.PROVIDER_NAME, service = ServiceNameConstants.ExecutionLifecycle)
@PluginDescription(title = KillHandlerExecutionLifecyclePlugin.PLUGIN_TITLE, description = KillHandlerExecutionLifecyclePlugin.PLUGIN_DESC)
@CompileStatic
class KillHandlerExecutionLifecyclePlugin implements ExecutionLifecyclePlugin {
    static final String PROVIDER_NAME = 'killhandler'
    static final String PLUGIN_TITLE = "Kill tracked processes after execution"
    static final String PLUGIN_DESC = '''Kill all processes collected by the 'Capture Process IDs' log filter\n\n
This operation will use the 'kill' and 'pkill' for Unix and 'taskkill' for Windows commands. These commands must be available at the node.
'''
    static final String GLOBAL_NODE_NAME = "__global"
    private static final String OSFAMILY_WINDOWS = "windows"
    private static final Logger logger = LoggerFactory.getLogger(KillHandlerExecutionLifecyclePlugin)

    AuthorizedServicesProvider rundeckAuthorizedServicesProvider
    KillHandlerProcessTrackingService processTrackingService

    @PluginProperty(
            title = "Kill spawned processes",
            description = "Also kill processes whose process SID matches the tracked PIDs"
    )
    boolean killChilds = false

    @Override
    ExecutionLifecycleStatus afterJobEnds(final JobExecutionEvent event) {
        if (!event.result.result.success || event.result.aborted) {
            def execId = event.execution.id
            def executionTrackData = processTrackingService.getExecutionTrackData(execId)

            if (executionTrackData) {
                // Delete stored execution data
                processTrackingService.flushExecution(execId)

                event.executionLogger.log(Constants.WARN_LEVEL, "Kill Handler processing tracked processes...")
                def authContext = event.executionContext.getUserAndRolesAuthContext()
                def nodeExecutionService = rundeckAuthorizedServicesProvider.getServicesWith(authContext).getService(NodeExecutionService)
                def execContext = event.executionContext

                event.nodes.each { node ->
                    def nodename = node?.nodename
                    def nodePidData = nodename ? executionTrackData.get(nodename) : null

                    if (!nodePidData && executionTrackData.containsKey(GLOBAL_NODE_NAME)) {
                        event.executionLogger.log(Constants.DEBUG_LEVEL, "No PID data for '${nodename}', falling back to '${GLOBAL_NODE_NAME}'")
                        nodePidData = executionTrackData.get(GLOBAL_NODE_NAME)
                    }

                    if (nodePidData?.pids) {
                        killProcessesOnNode(node, nodePidData, nodeExecutionService, execContext, event)
                    }
                }
            } else {
                event.executionLogger.log(Constants.WARN_LEVEL, 'No process ID captured, skipping...')
            }
        }
        return null
    }

    /**
     * Kill processes on a specific node
     * @param node The target node
     * @param nodePidData The PID data containing processes to kill
     * @param nodeExecutionService The node execution service
     * @param execContext The execution context
     * @param event The job execution event for logging
     */
    private void killProcessesOnNode(INodeEntry node,
                                     KillHandlerProcessTrackingService.NodePidList nodePidData,
                                     NodeExecutionService nodeExecutionService,
                                     ExecutionContext execContext,
                                     JobExecutionEvent event) {
        List<String> pids = nodePidData.pids as List<String>
        String commaPidList = pids.join(",")
        event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing tracked processes on node '${node.nodename}': ${commaPidList}")

        try {
            // Kill main processes
            NodeExecutorResult result = killMainProcesses(node, nodePidData, nodeExecutionService, execContext)
            event.executionLogger.log(Constants.DEBUG_LEVEL, "Result from killing processes attempt: ${result}")

            // Kill child processes if enabled
            if (killChilds && !OSFAMILY_WINDOWS.equalsIgnoreCase(node.osFamily)) {
                NodeExecutorResult childResult = killChildProcesses(node, nodePidData, nodeExecutionService, execContext, event)
                event.executionLogger.log(Constants.DEBUG_LEVEL, "Result from killing child processes attempt: ${childResult}")
            }

        } catch (Exception e) {
            logger.error("Failed to kill processes on node '${node.nodename}': ${e.message}", e)
            event.executionLogger.log(Constants.ERR_LEVEL, "Failed to kill processes on node '${node.nodename}': ${e.message}")
        }
    }

    /**
     * Kill the main tracked processes
     * @param node The target node
     * @param nodePidData The PID data containing processes to kill
     * @param nodeExecutionService The node execution service
     * @param execContext The execution context
     * @return NodeExecutorResult The result of the execution
     */
    private NodeExecutorResult killMainProcesses(INodeEntry node,
                                                 KillHandlerProcessTrackingService.NodePidList nodePidData,
                                                 NodeExecutionService nodeExecutionService,
                                                 ExecutionContext execContext) {
        List<String> pids = nodePidData.pids as List<String>
        String cmdKill

        if (OSFAMILY_WINDOWS.equalsIgnoreCase(node.osFamily)) {
            // Windows: Use taskkill with /T flag to kill process tree and /F to force
            cmdKill = "taskkill /PID " + pids.join(" /PID ") + " /T /F"
        } else {
            // Unix/Linux: Use kill with SIGKILL (-9)
            cmdKill = "kill -9 " + pids.join(" ")
        }

        return nodeExecutionService.executeCommand(execContext,
                ExecArgList.fromStrings(false, false, cmdKill),
                node)
    }

    /**
     * Kill child processes by session ID
     * @param node The target node
     * @param nodePidData The PID data containing parent processes
     * @param nodeExecutionService The node execution service
     * @param execContext The execution context
     * @param event The job execution event for logging
     * @return NodeExecutorResult The result of the execution
     */
    private NodeExecutorResult killChildProcesses(INodeEntry node,
                                                  KillHandlerProcessTrackingService.NodePidList nodePidData,
                                                  NodeExecutionService nodeExecutionService,
                                                  ExecutionContext execContext,
                                                  JobExecutionEvent event) {
        List<String> pids = nodePidData.pids as List<String>
        String commaPidList = pids.join(",")
        event.executionLogger.log(Constants.DEBUG_LEVEL, "Killing processes by session ID on node '${node.nodename}': ${commaPidList}")

        try {
            // When the parent pid is killed, children processes change its ppid to 1 (init pid)
            // To circumvent this, we issue a kill by SID also.
            // Note: The 'pkill -s' (session ID) option is not supported on macOS (all known versions, including macOS 10.x through 14.x/Sonoma).
            // This command will not work on macOS nodes due to the BSD implementation of pkill lacking the '-s' flag.
            String cmdKillSid = "pkill -SIGKILL -s " + commaPidList
            return nodeExecutionService.executeCommand(execContext,
                    ExecArgList.fromStrings(false, false, cmdKillSid),
                    node)
        } catch (Exception e) {
            logger.warn("Failed to kill child processes by SID on node '${node.nodename}': ${e.message}")
            event.executionLogger.log(Constants.DEBUG_LEVEL, "Failed to kill child processes by SID on node '${node.nodename}': ${e.message}")
            throw e // Re-throw to be handled by the calling method
        }
    }
}
