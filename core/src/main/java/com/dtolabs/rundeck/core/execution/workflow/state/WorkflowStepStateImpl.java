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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 3:00 PM
 */
public class WorkflowStepStateImpl implements WorkflowStepState {
    private StepIdentifier stepIdentifier;
    private StepState stepState;
    private Map<String, StepState> nodeStateMap;
    private Map<String, WorkflowStepState> parameterStateMap;
    private boolean subWorkflow;
    private WorkflowState subWorkflowState;
    private List<String> nodeStepTargets;
    private boolean nodeStep;


    public StepIdentifier getStepIdentifier() {
        return stepIdentifier;
    }

    public boolean hasSubWorkflow() {
        return subWorkflow;
    }

    public WorkflowState getSubWorkflowState() {
        return subWorkflowState;
    }

    public StepState getStepState() {
        return stepState;
    }

    public Map<String, ? extends StepState> getNodeStateMap() {
        return nodeStateMap;
    }

    public void setStepIdentifier(StepIdentifier stepIdentifier) {
        this.stepIdentifier = stepIdentifier;
    }

    public void setStepState(StepState stepState) {
        this.stepState = stepState;
    }

    public void setNodeStateMap(Map<String, StepState> nodeStateMap) {
        this.nodeStateMap = nodeStateMap;
    }

    public void setSubWorkflow(boolean subWorkflow) {
        this.subWorkflow = subWorkflow;
    }

    public void setSubWorkflowState(WorkflowState subWorkflowState) {
        this.subWorkflowState = subWorkflowState;
    }

    public List<String> getNodeStepTargets() {
        return nodeStepTargets;
    }

    public void setNodeStepTargets(List<String> nodeStepTargets) {
        this.nodeStepTargets = new ArrayList<String>(nodeStepTargets);
    }

    public boolean isNodeStep() {
        return nodeStep;
    }

    public void setNodeStep(boolean nodeStep) {
        this.nodeStep = nodeStep;
    }

    @Override
    public Map<String, ? extends WorkflowStepState> getParameterizedStateMap() {
        return parameterStateMap;
    }
}
