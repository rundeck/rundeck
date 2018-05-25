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

/**
 * Listens to context changes, where context can contain a node or step, or overall context.
 */
public interface StepNodeContextListener<NODE, STEP> {
    /**
     * Indicates context begins
     */
    void beginContext();

    /**
     * Indicates context has finished
     */
    void finishContext();

    /**
     * Enter a step context
     * @param step step
     */
    void beginStepContext(STEP step);

    /**
     * finish a step context
     */
    void finishStepContext();

    /**
     * Enter a node context
     * @param node node
     */
    void beginNodeContext(NODE node);

    /**
     * Finish a node context
     */
    void finishNodeContext();
}
