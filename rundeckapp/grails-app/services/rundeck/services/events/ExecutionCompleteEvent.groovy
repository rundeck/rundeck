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

import com.dtolabs.rundeck.core.execution.workflow.StepNodeUsageWorkflowListener.StepNodeUsageEntry
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
     * Per-step breakdown of step-node usage for this execution: one entry per step, keyed by
     * its hierarchical step path (e.g. "3", or "3/1" for a step nested under step 3). Each
     * entry holds that step's elapsed time in nanoseconds (node-level dispatches already
     * summed in), a discrete node-dispatch count, and the plugin/provider type that ran. Null
     * if {@code StepNodeUsageWorkflowListener} never finalized a breakdown for this execution
     * (e.g. a crash mid-execution).
     */
    Map<String, StepNodeUsageEntry> stepNodeUsageBreakdown


    @Override
    public String toString() {
        return "rundeck.services.events.ExecutionCompleteEvent{" +
                "state='" + state + '\'' +
                ", execution=" + execution +
                ", job=" + job +
                ", nodeStatus=" + nodeStatus +
                ", context=" + context +
                ", stepNodeUsageBreakdown=" + stepNodeUsageBreakdown +
                '}';
    }
}
