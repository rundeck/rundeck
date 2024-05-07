package com.rundeck.plugins.killhandler

import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap

/**
 * Service for tracking process ids across multiple nodes on an execution.
 */
@CompileStatic
class KillHandlerProcessTrackingService {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, NodePidList>> executionTrackMap = new ConcurrentHashMap<>()

    /**
     * Registers a new process id for a node under the context of an execution.
     * <p>This method is thread-safe</p>
     * @param executionId ID of execution
     * @param nodename Node name
     * @param pid Process ID to register.
     */
    void registerPID(String executionId, String nodename, String pid) {

        Objects.requireNonNull(executionId, "null execution id")
        Objects.requireNonNull(nodename, "null nodename")
        Objects.requireNonNull(pid, "null pid")

        def nodeMap = executionTrackMap.computeIfAbsent(executionId, { id -> new ConcurrentHashMap<>() })
        def nodeEntry = nodeMap.computeIfAbsent(nodename, { k -> new NodePidList() })
        nodeEntry.addPid(pid)
    }


    /**
     * Gets a read-only map of data currently registered for an execution.
     * @param executionId
     * @return
     */
    Map<String, NodePidList> getExecutionTrackData(String executionId) {
        ConcurrentHashMap<String, NodePidList> execPIDsMap = executionTrackMap.get(executionId)
        return execPIDsMap ? Collections.unmodifiableMap(execPIDsMap) : null
    }

    /**
     * Removes all tracking data for an execution.
     * @param executionId Execution ID to remove.
     */
    void flushExecution(String executionId) {
        executionTrackMap.remove(executionId)
    }

    /**
     * Entry for keeping a list of pids for a node in a thread-safe manner.
     */
    class NodePidList {

        private final List<String> pids = new LinkedList<>()

        private synchronized void addPid(String pid) {
            pids.add(pid)
        }

        List<String> getPids() {
            return Collections.unmodifiableList(pids)
        }
    }

}
