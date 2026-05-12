/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

/**
 * Marker interface for {@link com.dtolabs.rundeck.core.execution.StepExecutionItem}s that
 * have been flattened into the engine's top-level command list from a parent workflow step
 * containing sub-steps (for example, a Conditional step expanded into one engine step per
 * sub-step).
 *
 * The rule engine and parallel/sequential strategies continue to see one entry per sub-step
 * (so flat engine step numbers are preserved). The workflow execution listeners use the
 * parent and sub indices reported here to emit a hierarchical
 * {@link com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier}
 * (e.g. {@code "2/1"}) for log lines and step state events, so the resulting
 * {@code .rdlog} and state files align with the original (un-flattened) job definition.
 */
public interface HasParentStepContext {
    /**
     * @return 1-based index of the parent step in the original job definition (the
     * conditional step that was expanded). Positive only for conditional sub-steps;
     * -1 for regular flat steps.
     */
    int getParentStepNumber();

    /**
     * @return 1-based index of this sub-step within the parent step's sub-step list.
     * Positive only for conditional sub-steps; -1 for regular flat steps.
     */
    int getSubStepNumber();

    /**
     * The 1-based logical step number of this item in the original (un-flattened) job
     * definition. For a conditional sub-step this equals {@link #getParentStepNumber()};
     * for a regular step it is the step's own logical position. Returns -1 when not set
     * (callers must fall back to the flat engine step number).
     */
    default int getLogicalStepNumber() {
        return -1;
    }
}
