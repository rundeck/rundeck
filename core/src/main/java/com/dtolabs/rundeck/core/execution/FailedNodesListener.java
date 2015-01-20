/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* FailedNodesListener.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 4, 2010 2:02:17 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import java.util.Collection;
import java.util.Map;

/**
 * FailedNodesListener is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface FailedNodesListener {
    /**
     * Called with a map of node names to failures.  The nodes will be the failed nodes.
     *
     * @param failures failures map
     */
    public void nodesFailed(Map<String, NodeStepResult> failures);

    /**
     * Called if no nodes failed during execution.
     */
    public void nodesSucceeded();

    /**
     * @param names full list of matched nodes prior to execution.
     */
    public void matchedNodes(Collection<String> names);
}
