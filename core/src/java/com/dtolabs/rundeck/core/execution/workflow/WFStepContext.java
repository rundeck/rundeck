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
* StepContext.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 9:08 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionItem;

import java.util.*;

/**
 * WFStepContext contains context about a workflow step, and can generate logging context from the details of the step.
 * Node and step number/item are independent.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class WFStepContext {
    private Map<String, String> loggingContext;
    private ExecutionItem stepItem;
    private int step = -1;
    private INodeEntry node;

    public int getStep() {
        return step;
    }

    public void setStep(final int step, final ExecutionItem executionItem) {
        this.step = step;
        this.stepItem = executionItem;
        clearContext();
    }

    public void clearStep() {
        setStep(-1, null);
    }

    public INodeEntry getNode() {
        return node;
    }

    public void setNode(final INodeEntry node) {
        this.node = node;
        clearContext();
    }

    public void clearNode() {
        setNode(null);
    }

    private void clearContext() {
        loggingContext = null;
    }

    public Map<String, String> getContext() {
        if (null != loggingContext) {
            return loggingContext;
        }
        loggingContext = new HashMap<String, String>();
        if (null != node) {
            loggingContext.put("node", node.getNodename());
            loggingContext.put("user", node.extractUserName());
        }
        if (null != stepItem) {
            loggingContext.put("command", stepItem.getType() + "." + step);
        }
        if (step > -1) {
            loggingContext.put("step", Integer.toString(step));
        }
        return loggingContext;
    }

    @Override
    public String toString() {
        return "StepContext{" +
               "stepItem=" + stepItem +
               ", step=" + step +
               ", node=" + node +
               '}';
    }
}
