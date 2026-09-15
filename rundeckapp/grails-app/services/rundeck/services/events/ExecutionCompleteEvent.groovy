/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package rundeck.services.events

import com.dtolabs.rundeck.core.execution.workflow.StepNodeSecondsWorkflowListener.StepNodeSecondsEntry
import rundeck.Execution
import rundeck.ScheduledExecution

/**
 * Created by greg on 11/2/15.
 */
class ExecutionCompleteEvent {
    String state
    Execution execution
    ScheduledExecution job
    Map nodeStatus
    Map context
    /**
     * Sum of step-node-second durations for this execution (Section 2 of the RBA
     * consumption-billing proposal), or null if none was recorded -- e.g. the
     * StepNodeSecondsWorkflowListener never finalized a breakdown for this execution.
     * Derived from {@link #stepNodeSecondsBreakdown}'s values -- kept as its own field since
     * most consumers (e.g. the Micrometer metric) only want the scalar.
     */
    Long stepNodeSeconds

    /**
     * Per-step breakdown backing {@link #stepNodeSeconds}: one entry per step, keyed by its
     * hierarchical step path (e.g. "3", or "3/1" for a step nested under step 3 -- the same
     * scheme state.json uses for its own step identifiers). Each entry holds that step's
     * duration in whole seconds (node-level dispatches already summed in) and the
     * plugin/provider type that ran, so a consumer can decide billability by plugin type
     * without needing to re-read the job definition or execution state. Null under the same
     * conditions {@link #stepNodeSeconds} is null. Lets a future consumer (e.g. a billing
     * subscriber excluding "non-billable" steps -- RBA_BILLING proposal Section 6.2.1)
     * exclude specific steps' contributions.
     */
    Map<String, StepNodeSecondsEntry> stepNodeSecondsBreakdown


    @Override
    public String toString() {
        return "rundeck.services.events.ExecutionCompleteEvent{" +
                "state='" + state + '\'' +
                ", execution=" + execution +
                ", job=" + job +
                ", nodeStatus=" + nodeStatus +
                ", context=" + context +
                ", stepNodeSeconds=" + stepNodeSeconds +
                ", stepNodeSecondsBreakdown=" + stepNodeSecondsBreakdown +
                '}';
    }
}
