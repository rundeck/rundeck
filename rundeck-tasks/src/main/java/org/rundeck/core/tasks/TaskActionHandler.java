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

import java.util.Map;

/**
 * Handles a TaskAction
 */
public interface TaskActionHandler<T extends TaskContext> {
    /**
     * @return true if this handler needs registration info at system startup
     */
    default boolean onStartup(T contextInfo) {
        return false;
    }

    /**
     * The action for a task should be performed
     *
     * @param taskId    ID
     * @param triggerMap data from the taskTrigger
     * @param taskTrigger  taskTrigger
     * @param action       action
     */
    Map performTaskAction(T contextInfo, Map triggerMap, TaskTrigger taskTrigger, TaskAction action)
            throws ActionFailed;

    /**
     * @param action
     * @param contextInfo
     * @return true if the given action will be handled internally, meaning the registration of the condition check for
     *         a trigger will also handle invoking the action
     */
    boolean handlesAction(TaskAction action, T contextInfo);
}
