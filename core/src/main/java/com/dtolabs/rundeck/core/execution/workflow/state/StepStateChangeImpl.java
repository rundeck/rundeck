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

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 4:59 PM
 */
public class StepStateChangeImpl implements StepStateChange {
    private StepState stepState;
    private String nodeName;
    private boolean nodeState;

    public StepState getStepState() {
        return stepState;
    }

    public void setStepState(StepState stepState) {
        this.stepState = stepState;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public boolean isNodeState() {
        return nodeState;
    }

    public void setNodeState(boolean nodeState) {
        this.nodeState = nodeState;
    }

    @Override
    public String toString() {
        return "StepStateChangeImpl{" +
                "stepState=" + stepState +
                ", nodeName='" + nodeName + '\'' +
                ", nodeState=" + nodeState +
                '}';
    }
}
