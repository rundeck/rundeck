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

/*
* NodeStepResultImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/8/12 4:13 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl;


/**
 * NodeStepResultImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeStepResultImpl extends StepExecutionResultImpl implements NodeStepResult {
    private INodeEntry node;

    /**
     * Create a success result
     * @param node  node
     */
    public NodeStepResultImpl(INodeEntry node) {
        super();
        this.node = node;
    }

    /**
     * Create a failure result
     * @param exception exception
     * @param failureReason reason
     * @param failureMessage message
     * @param node node
     */
    public NodeStepResultImpl(
                              Throwable exception,
                              FailureReason failureReason,
                              String failureMessage,
                              INodeEntry node) {
        super(exception, failureReason, failureMessage);
        this.node = node;
    }

    public INodeEntry getNode() {
        return node;
    }
}
