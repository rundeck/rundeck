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

package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStepState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/16/13
 * Time: 10:39 AM
 */
public interface MutableWorkflowStepState extends WorkflowStepState{

    /**
     * The step's state
     *
     * @return
     */
    MutableStepState getMutableStepState();

    /**
     * Return a map of node name to step states for the step
     *
     * @return
     */
    Map<String, MutableStepState> getMutableNodeStateMap();

    /**
     * Return a parameterized step state
     * @param ident
     * @param params
     * @return
     */
    public MutableWorkflowStepState getParameterizedStepState(StepIdentifier ident,Map<String,String> params);

    /**
     * Return a map of node name to step states for the step
     *
     * @return
     */
    Map<String, MutableWorkflowStepState> getMutableParameterizedStateMap();

    /**
     * Return the sub workflow state
     *
     * @return
     */
    MutableWorkflowState getMutableSubWorkflowState();
    MutableWorkflowStepState getOwnerStepState();

    /**
     * Creates a mutable sub workflow state and enables it
     * @return
     */
    MutableWorkflowState createMutableSubWorkflowState(List<String> nodeSet, long count);

    /**
     * Indicates that the step is a node step with the given targets
     * @param nodeset
     */
    void setNodeStepTargets(List<String> nodeset);

}
