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

package com.dtolabs.rundeck.core.authorization;

import java.util.Map;

/**
 * Extension point for resolving the ACL resource used to authorize read/view of an execution,
 * keyed by its execution type.
 * <p>
 * A job's executions are authorized against the job; an execution with no job is authorized
 * against {@code AuthConstants.RESOURCE_ADHOC} by default. A feature that produces its own kind
 * of execution can contribute a resolver so that executions of a given type are gated behind
 * their own resource kind instead, without this authorization API or the open source build having
 * to know that kind. This keeps such executions subject to their own ACL when they appear in the
 * generic execution and activity lists, like any other execution type.
 * <p>
 * Implementations are discovered as Spring beans. The first resolver to return a non-null resource
 * for an execution type wins; if none match, the default resource is used. The execution type is
 * passed rather than the execution itself so this contract stays free of any dependency on the
 * application's persistence model.
 */
public interface ExecutionAuthResourceResolver {
    /**
     * Resolve the ACL resource for an execution of the given type.
     *
     * @param executionType the value of the execution's {@code executionType}, which may be null
     * @return the ACL resource map to authorize against, or {@code null} to defer to the default
     * resource
     */
    Map<String, String> authResourceForExecutionType(String executionType);
}
