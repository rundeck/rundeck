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

package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describes the state of a step within a workflow, which has an identifier, a possible sub workflow, and possible
 * node-oriented states.
 */
public interface WorkflowStepState {
    /**
     * @return The identifier
     */
    public StepIdentifier getStepIdentifier();

    /**
     * @return The step's state
     *
     */
    public StepState getStepState();


    /**
     * @return  a map of node name to step states for the step
     *
     */
    public Map<String, ? extends StepState> getNodeStateMap();

    /**
     * @return  a map of parameter strings to representative WorkflowStepStates
     *
     */
    Map<String, ? extends WorkflowStepState> getParameterizedStateMap();


    /**
     * @return  true if the step contains a sub workflow
     */
    public boolean hasSubWorkflow();

    /**
     * @return  the sub workflow state
     *
     */
    public WorkflowState getSubWorkflowState();

    /**
     * @return  the set of node targets if this is a node step, or null
     */
    public List<String> getNodeStepTargets();

    /**
     * @return  true if this is a node step
     */
    public boolean isNodeStep();
}
