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

public interface TaskManager<T extends TaskContext> {
    /**
     * @param action
     * @param taskContext
     * @return handler for the action
     */
    public TaskActionHandler<T> getActionHandlerForTaskAction(TaskAction action, T taskContext);

    /**
     * @param action        action
     * @param contextInfo   context
     * @param triggerMap    trigger data map
     * @param userData      user data map
     * @param conditionData condition result data
     * @param trigger       trigger
     * @return result of performing the action
     */
    public Map performTaskAction(
        TaskAction action,
        T contextInfo,
        Map triggerMap,
        Map userData,
        Map conditionData,
        TaskTrigger trigger
    ) throws ActionFailed;
}
