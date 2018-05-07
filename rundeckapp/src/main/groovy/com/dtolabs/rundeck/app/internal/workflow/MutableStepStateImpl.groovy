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

package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepStateImpl

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/16/13
 * Time: 10:46 AM
 */
class MutableStepStateImpl implements MutableStepState {
    ExecutionState executionState;
    Map metadata;
    String errorMessage;
    Date startTime
    Date updateTime
    Date endTime

    MutableStepStateImpl() {
        executionState=ExecutionState.WAITING
    }

    @Override
    public java.lang.String toString() {
        return "step{" +
                "state=" + executionState +
                (metadata?
                ", metadata=" + metadata :'' ) +
                (errorMessage?
                ", errorMessage='" + errorMessage + '\'' :'')+
                '}';
    }
}
