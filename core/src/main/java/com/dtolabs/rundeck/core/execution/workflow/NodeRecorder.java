/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* NodeRecorder.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Dec 15, 2010 3:13:18 PM
*
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.FailedNodesListener;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import java.util.*;

/**
 * NodeRecorder stores success/failure node list
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeRecorder implements FailedNodesListener {
    private HashSet<String> matchedNodes;
    private HashMap<String, NodeStepResult> failedNodes;
    private boolean success=false;

    public NodeRecorder() {
        matchedNodes =new HashSet<String>();
        failedNodes=new HashMap<String, NodeStepResult>();
        success=false;
    }

    public void nodesFailed(final Map<String, NodeStepResult> failures) {
        failedNodes.putAll(failures);
    }

    public void nodesSucceeded() {
        success = true;
    }

    public void matchedNodes(final Collection<String> names) {
        matchedNodes.addAll(names);
    }

    /**
     * @return the matched nodes less the failed nodes.
     */
    public HashSet<String> getSuccessfulNodes() {
        final HashSet<String> successfulNodes = new HashSet<String>(matchedNodes);
        successfulNodes.removeAll(failedNodes.keySet());
        return successfulNodes;
    }

    /**
     * @return the set of failed nodes
     */
    public HashMap<String, NodeStepResult> getFailedNodes() {
        return failedNodes;
    }

    /**
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return the set of matched nodes
     */
    public HashSet<String> getMatchedNodes() {
        return matchedNodes;
    }
}
