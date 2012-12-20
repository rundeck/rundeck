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
* NodeExecutorResultImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 6:54 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl;


/**
 * NodeExecutorResultImpl simple implementation of {@link NodeExecutorResult}
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeExecutorResultImpl extends NodeStepResultImpl implements NodeExecutorResult {
    public static final String FAILURE_DATA_RESULT_CODE = "resultCode";
    private int resultCode;

    public static NodeExecutorResultImpl createSuccess(INodeEntry node) {
        return new NodeExecutorResultImpl(node, 0);
    }

    private NodeExecutorResultImpl(INodeEntry node, int resultCode) {
        super(node);
        this.resultCode = resultCode;
        getFailureData().put(FAILURE_DATA_RESULT_CODE, resultCode);
    }

    public static NodeExecutorResultImpl createFailure(FailureReason reason, String message,
                                                       Exception exception, INodeEntry node, int resultCode) {

        return new NodeExecutorResultImpl(exception, node, resultCode, reason, message);
    }

    public static NodeExecutorResultImpl createFailure(FailureReason reason,
                                                       String message,
                                                       INodeEntry node,
                                                       int resultCode) {

        return new NodeExecutorResultImpl(null, node, resultCode, reason, message);
    }

    public static NodeExecutorResultImpl createFailure(FailureReason reason, String message, INodeEntry node) {

        return new NodeExecutorResultImpl(null, node, -1, reason, message);
    }

    private NodeExecutorResultImpl(Exception exception,
                                   INodeEntry node,
                                   int resultCode, final FailureReason reason, final String failureMessage) {
        super(exception, reason, failureMessage, node);
        this.resultCode = resultCode;
        getFailureData().put(FAILURE_DATA_RESULT_CODE, resultCode);
    }

    public int getResultCode() {
        return resultCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof NodeExecutorResultImpl)) { return false; }
        if (!super.equals(o)) { return false; }

        NodeExecutorResultImpl result = (NodeExecutorResultImpl) o;

        if (resultCode != result.resultCode) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + resultCode;
        return result;
    }

    @Override
    public String toString() {
        return isSuccess() ? "Succeeded" : getFailureReason() + ": " + getFailureMessage();
    }

}
