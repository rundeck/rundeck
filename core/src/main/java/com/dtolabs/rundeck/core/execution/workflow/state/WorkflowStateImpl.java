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

import java.util.*;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 3:35 PM
 */
public class WorkflowStateImpl implements WorkflowState {
    private List<String> nodeSet;
    private List<String> allNodes;
    private long stepCount;
    private ExecutionState executionState;
    private Date updateTime;
    private Date startTime;
    private Date endTime;
    private ArrayList<WorkflowStepState> stepStates;
    private Map<String, WorkflowNodeState> nodeStates;
    private String serverNode;

    public WorkflowStateImpl(
            List<String> nodeSet,
            List<String> allNodes,
            long stepCount, ExecutionState executionState,
            Date updateTime,
            Date startTime,
            Date endTime,
            String serverNode,
            List<WorkflowStepState> stepStates,
            Map<String, WorkflowNodeState> nodeStates
    ) {
        this.setNodeSet(nodeSet);
        this.setAllNodes(allNodes);
        this.setStepCount(stepCount);
        this.setExecutionState(executionState);
        this.setUpdateTime(updateTime);
        this.setStartTime(startTime);
        this.setEndTime(endTime);
        this.setServerNode(serverNode);
        if(null!=stepStates){
            this.setStepStates(new ArrayList<>(stepStates));
        }
        this.setNodeStates(nodeStates);
    }

    public List<String> getNodeSet() {
        return nodeSet;
    }

    public long getStepCount() {
        return stepCount;
    }

    public ExecutionState getExecutionState() {
        return executionState;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public List<WorkflowStepState> getStepStates() {
        return stepStates;
    }

    public void setNodeSet(List<String> nodeSet) {
        this.nodeSet = new ArrayList<>(nodeSet);
    }

    public void setStepCount(long stepCount) {
        this.stepCount = stepCount;
    }

    public void setExecutionState(ExecutionState executionState) {
        this.executionState = executionState;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public void setStepStates(ArrayList<WorkflowStepState> stepStates) {
        this.stepStates = stepStates;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public List<String> getAllNodes() {
        return allNodes;
    }

    public void setAllNodes(List<String> allNodes) {
        this.allNodes = allNodes;
    }

    public Map<String, WorkflowNodeState> getNodeStates() {
        return nodeStates;
    }

    public void setNodeStates(Map<String, WorkflowNodeState> nodeStates) {
        this.nodeStates = nodeStates;
    }

    public String getServerNode() {
        return serverNode;
    }

    public void setServerNode(String serverNode) {
        this.serverNode = serverNode;
    }
}
