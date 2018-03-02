/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.core.tasks;

import java.util.List;

/**
 * Trigger  handler
 */
public interface TaskTriggerHandler<T extends TaskContext> {
    /**
     * @return true if this handler needs registration info at system startup
     */
    default boolean onStartup() {
        return false;
    }

    /**
     * @param taskTrigger
     * @param context
     * @return true if the given taskTrigger should be handled
     */
    boolean handlesTrigger(TaskTrigger taskTrigger, T context);

    /**
     * Register taskTrigger checks for the trigger, will be called when a trigger is created or modified, so if the
     * registration has already occurred for the given trigger, the taskTrigger data should be checked for changes
     *
     * @param context
     * @param trigger
     * @param action
     * @param service
     * @return true if successful
     */
    boolean registerTriggerForAction(
        T context,
        TaskTrigger trigger,
        List<TaskCondition> conditions,
        TaskAction action,
        TaskActionInvoker<T> service
    ) throws TriggerException;

    /**
     * Remove taskTrigger check registration for the action
     *
     * @param context
     * @param trigger
     * @param action
     * @param service
     */
    void deregisterTriggerForAction(
        T context,
        TaskTrigger trigger,
        List<TaskCondition> conditions,
        TaskAction action,
        TaskActionInvoker<T> service
    ) throws TriggerException;
}
