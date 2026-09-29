/*
 * Copyright 2026 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.workflow.StepNodeUsageWorkflowListener.StepNodeUsageEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory hand-off point for the finished step-node usage breakdown computed by
 * {@link StepNodeUsageWorkflowListener} for a top-level execution: one entry per step, keyed
 * by its hierarchical step path (e.g. "3", or "3/1" for a step nested under step 3), each
 * holding that step's elapsed time and the plugin/provider type that ran. Not persisted to any
 * database table and not exposed via any external API -- a caller elsewhere in the process
 * reads it once via {@link #takeFinishedBreakdown(Long)}.
 * <p>
 * A static singleton (rather than a value owned by the listener instance) is used
 * deliberately so a caller with only an execution id, and no handle on the listener that
 * computed it, can still resolve the value.
 * <p>
 * <b>Known limitation:</b> an execution whose process is killed before
 * {@code finishWorkflowExecution} is ever called never gets an entry written for it, so there
 * is nothing to evict for that case specifically; however, nothing proactively caps or expires
 * entries that do get written, so a caller that never calls {@link #takeFinishedBreakdown(Long)}
 * could still accumulate entries indefinitely.
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
     * Record the finished per-step usage breakdown for an execution. Overwrites any
     * previously recorded value for the same execution id.
     */
    void recordFinishedBreakdown(final Long executionId, final Map<String, StepNodeUsageEntry> breakdown) {
        if (executionId != null) {
            finishedBreakdowns.put(executionId, breakdown);
        }
    }

    /**
     * Read and remove the finished usage breakdown for an execution: one entry per step,
     * keyed by its hierarchical step path (e.g. "3", or "3/1" for a step nested under step 3),
     * each holding that step's elapsed time in nanoseconds (node-level dispatches already
     * summed in) and its plugin/provider type. A caller that needs a whole-seconds total
     * across steps should sum the returned map's {@code getNanos()} values and round once, not
     * round each entry individually.
     *
     * @param executionId the execution id
     * @return the finished breakdown, or null if none was recorded (or it was already taken)
     */
    public Map<String, StepNodeUsageEntry> takeFinishedBreakdown(final Long executionId) {
        return executionId == null ? null : finishedBreakdowns.remove(executionId);
    }
}
