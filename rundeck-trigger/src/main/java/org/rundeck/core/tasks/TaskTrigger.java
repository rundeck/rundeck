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

public interface TaskTrigger {
    /**
     * @return condition type
     */
    String getType();

    /**
     * @return data generated for the Trigger when this condition occurs
     */
    default Map getTriggerData() {
        return null;
    }

    /**
     * @return true if the configuration is valid
     */
    default boolean isValid() {
        return true;
    }

    /**
     * @return any custom validation errors, where the map key is the config property name, and the value is the error
     *         string
     */
    default Map<String, String> getValidationErrors() {
        return null;
    }
}
