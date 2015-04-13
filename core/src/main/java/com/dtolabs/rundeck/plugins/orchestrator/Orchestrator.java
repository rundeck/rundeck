/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */
package com.dtolabs.rundeck.plugins.orchestrator;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

/**
 * Orchestrator is created for each execution this deals with the actual selection of nodes
 *
 * @author Ashley Taylor
 */
public interface Orchestrator {

    /**
     * @return the next available node, or null if no node is available
     */
    public INodeEntry nextNode();

    /**
     * Indicates that the node processor has completed work on the given node
     *
     * @param node the node
     * @param success true if the execution was successful
     * @param result the result if available
     */
    public void returnNode(INodeEntry node, boolean success, NodeStepResult result);

    /**
     * @return true if no more nodes will ever be available, false to indicate that new nodes might become available
     */
    public boolean isComplete();
}
