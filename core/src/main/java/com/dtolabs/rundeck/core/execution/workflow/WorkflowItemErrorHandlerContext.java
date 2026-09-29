/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

/**
 * Marker context component, registered via {@link com.dtolabs.rundeck.core.execution.ExecutionContext#componentForType(Class)},
 * that indicates the current execution context originates from a workflow step's error handler
 * branch rather than from the step's own main execution branch.
 * <p>
 * A plain {@code stepContext}/{@code stepNumber} path (see {@link com.dtolabs.rundeck.core.execution.ExecutionContext})
 * cannot by itself distinguish a step's main execution from its error handler's execution, since
 * both are built from the same numeric step path. Consumers that need to tell the two apart
 * (e.g. to resolve per-branch configuration) can check for the presence of this component on the
 * context instead.
 */
public final class WorkflowItemErrorHandlerContext {
    public static final WorkflowItemErrorHandlerContext INSTANCE = new WorkflowItemErrorHandlerContext();

    private WorkflowItemErrorHandlerContext() {
    }
}
