/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.jobs;

/**
 * Basic status result from lifecycle events
 */
public interface LifecycleStatus {

    /**
     * @return true if event handler was successful
     */
    default boolean isSuccessful() {
        return true;
    }

    /**
     * @return descriptive error message when result is not successful
     */
    default String getErrorMessage() {
        return null;
    }

    /**
     * @return true indicates values returned by this status result should be used (types of values depends on event
     *         context)
     */
    default boolean isUseNewValues() {
        return false;
    }
}
