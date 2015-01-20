/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.List;
import java.util.Map;

/**
 * Represents the execution state of a single node
 */
public interface WorkflowNodeState {
    /**
     * @return The node name
     *
     */
    public String getNodeName();

    /**
     * @return The node's current step
     *
     */
    public StepIdentifier getLastIdentifier();

    /**
     * @return The node's current state
     *
     */
    public StepState getNodeState();


    /**
     * @return a map of step ident to step states for the node
     *
     */
    public Map<StepIdentifier, ? extends StepState> getStepStateMap();

}
