package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.workflow.StepNodeUsageWorkflowListener.StepNodeUsageEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory hand-off point for the finished "step_node_seconds" breakdown computed by
 * {@link StepNodeUsageWorkflowListener} for a top-level execution: one entry per step,
 * keyed by its hierarchical step path (e.g. "3", or "3/1" for a step nested under step 3),
 * each holding that step's duration (with any node-level dispatches already summed in) and
 * the plugin/provider type that ran. Not persisted to any database table and not exposed via
 * any external API -- a caller elsewhere in the process (e.g. a reporting listener) reads it
 * once via {@link #takeFinishedBreakdown(Long)}.
 * <p>
 * A static singleton (rather than a value owned by the listener instance) is used
 * deliberately so a caller with only an execution id, and no handle on the listener that
 * computed it, can still resolve the value.
 * <p>
 * <b>Known limitation:</b> an execution whose process is killed before
 * {@code finishWorkflowExecution} is ever called (e.g. a pod OOM-kill, not the normal abort
 * path which does reach completion) never gets an entry written for it, so there is nothing
 * to evict for that case specifically; however, nothing proactively caps or expires entries
 * that do get written, so an unbounded stream of callers that never call
 * {@link #takeFinishedBreakdown(Long)} could still accumulate entries indefinitely. This is
 * accepted for now -- in practice a caller always does take the entry shortly after
 * {@code finishWorkflowExecution} fires, since this store now backs a real runtime path (the
 * Micrometer metric, and Runbook Automation's consumption-billing subscriber both read
 * through it via {@code ExecutionCompleteEvent}), not just metric validation.
 */
public final class StepNodeUsageStore {
    private static final StepNodeUsageStore INSTANCE = new StepNodeUsageStore();

    private final ConcurrentHashMap<Long, Map<String, StepNodeUsageEntry>> finishedBreakdowns = new ConcurrentHashMap<>();

    private StepNodeUsageStore() {
    }

    public static StepNodeUsageStore getInstance() {
        return INSTANCE;
    }

    /**
     * Record the finished per-step step_node_seconds breakdown for an execution. Overwrites
     * any previously recorded value for the same execution id.
     */
    void recordFinishedBreakdown(final Long executionId, final Map<String, StepNodeUsageEntry> breakdown) {
        if (executionId != null) {
            finishedBreakdowns.put(executionId, breakdown);
        }
    }

    /**
     * Read and remove the finished step_node_seconds breakdown for an execution: one entry
     * per step, keyed by its hierarchical step path (e.g. "3", or "3/1" for a step nested
     * under step 3), each holding that step's duration in whole seconds (node-level
     * dispatches already summed in) and its plugin/provider type. Callers that only need the
     * execution's total should sum the returned map's {@code getSeconds()} values.
     *
     * @param executionId the execution id
     * @return the finished breakdown, or null if none was recorded (or it was already taken)
     */
    public Map<String, StepNodeUsageEntry> takeFinishedBreakdown(final Long executionId) {
        return executionId == null ? null : finishedBreakdowns.remove(executionId);
    }
}
