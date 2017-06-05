/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Basic data implementation of WorkflowStatusResult
 * @author greg
 * @since 5/2/17
 */
@Data
@Builder
public class WorkflowStatusResultImpl implements WorkflowStatusResult {
    String statusString;
    ControlBehavior controlBehavior;
    boolean success;

    public static WorkflowStatusResultImpl with(WorkflowStatusResult result) {
        return WorkflowStatusResultImpl.builder()
                                       .controlBehavior(result.getControlBehavior())
                                       .statusString(result.getStatusString())
                                       .success(result.isSuccess())
                                       .build();
    }

    public String toString() {
        String status = null != getStatusString() ? getStatusString() : Boolean.toString(isSuccess());
        return String.format(
                "%s requested%s",
                getControlBehavior(),
                getControlBehavior() == ControlBehavior.Halt
                ? String.format(" with result: %s", status)
                : ""
        );
    }
}
