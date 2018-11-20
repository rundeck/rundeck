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

package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;

/**
 * record flow control
 */
public class FlowController implements FlowControl, WorkflowStatusResult {
    private ControlBehavior controlBehavior=ControlBehavior.Continue;
    private String statusString=null;
    private boolean success=false;
    private boolean controlled=false;


    @Override
    public void Halt(final String statusString) {
        controlBehavior = ControlBehavior.Halt;
        this.statusString = statusString;
        this.controlled=true;

    }

    @Override
    public void Halt(final boolean success) {
        controlBehavior = ControlBehavior.Halt;
        this.success = success;
        this.controlled=true;
    }

    @Override
    public void Continue() {
        controlBehavior = ControlBehavior.Continue;
        this.controlled = true;
    }

    @Override
    public String getStatusString() {
        return statusString;
    }
    public boolean isCustomStatusString() {
        return null != statusString &&
               !FlowControl.STATUS_FAILED.equalsIgnoreCase(statusString) &&
               !FlowControl.STATUS_SUCCEEDED.equalsIgnoreCase(statusString);
    }

    public ControlBehavior getControlBehavior() {
        return controlBehavior;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return true if Halt or Continue has been called.
     */
    public boolean isControlled() {
        return controlled;
    }

    FailureReason getFailureReason(FailureReason defVal) {
        if (isSuccess()) {
            return null;
        }
        if (controlBehavior == ControlBehavior.Halt) {
            return FlowControlFailureReason.FlowControlHalted;
        }
        return defVal;
    }

    static enum FlowControlFailureReason
        implements FailureReason
    {
        FlowControlHalted
    }

    @Override
    public String toString() {
        return "FlowController{" +
               "controlBehavior=" + controlBehavior +
               ", statusString='" + statusString + '\'' +
               ", success=" + success +
               ", controlled=" + controlled +
               '}';
    }
}
