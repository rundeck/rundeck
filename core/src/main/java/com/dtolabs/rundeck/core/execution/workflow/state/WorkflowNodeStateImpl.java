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

import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 11/13/13 Time: 12:47 PM
 */
public class WorkflowNodeStateImpl implements WorkflowNodeState {
    private String nodeName;
    private StepIdentifier lastIdentifier;
    private StepState nodeState;
    private Map<StepIdentifier, ? extends StepState> stepStateMap;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public StepIdentifier getLastIdentifier() {
        return lastIdentifier;
    }

    public void setLastIdentifier(StepIdentifier lastIdentifier) {
        this.lastIdentifier = lastIdentifier;
    }

    public StepState getNodeState() {
        return nodeState;
    }

    public void setNodeState(StepState nodeState) {
        this.nodeState = nodeState;
    }

    public Map<StepIdentifier, ? extends StepState> getStepStateMap() {
        return stepStateMap;
    }

    public void setStepStateMap(Map<StepIdentifier, ? extends StepState> stepStateMap) {
        this.stepStateMap = stepStateMap;
    }
}
