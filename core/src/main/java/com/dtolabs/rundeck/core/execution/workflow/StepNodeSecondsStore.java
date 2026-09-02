package com.dtolabs.rundeck.core.execution.workflow;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory hand-off point for the finished "step_node_seconds" total computed by
 * {@link StepNodeSecondsWorkflowListener} for a top-level execution. Not persisted to any
 * database table and not exposed via any external API -- a caller elsewhere in the process
 * (e.g. a reporting listener) reads it once via {@link #takeFinishedTotal(Long)}.
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
 * {@link #takeFinishedTotal(Long)} could still accumulate entries indefinitely. This is
 * accepted for now, since this store exists purely to validate the metric's computation.
 */
public final class StepNodeSecondsStore {
    private static final StepNodeSecondsStore INSTANCE = new StepNodeSecondsStore();

    private final ConcurrentHashMap<Long, Long> finishedTotals = new ConcurrentHashMap<>();

    private StepNodeSecondsStore() {
    }

    public static StepNodeSecondsStore getInstance() {
        return INSTANCE;
    }

    /**
     * Record the finished step_node_seconds total for an execution. Overwrites any
     * previously recorded value for the same execution id.
     */
    void recordFinishedTotal(final Long executionId, final long stepNodeSeconds) {
        if (executionId != null) {
            finishedTotals.put(executionId, stepNodeSeconds);
        }
    }

    /**
     * Read and remove the finished step_node_seconds total for an execution.
     *
     * @param executionId the execution id
     * @return the finished total, or null if none was recorded (or it was already taken)
     */
    public Long takeFinishedTotal(final Long executionId) {
        return executionId == null ? null : finishedTotals.remove(executionId);
    }
}
