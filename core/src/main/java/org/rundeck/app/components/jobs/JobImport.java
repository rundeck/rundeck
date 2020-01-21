/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.components.jobs;

import java.util.Map;

/**
 * extension for importing job definitions
 */
public interface JobImport {
    /**
     * @param job        the defined job item
     * @param jobDataMap final canonical job data map
     */
    void importCanonicalMap(Object job, Map jobDataMap);

    /**
     * Import
     *
     * @param jobXMap    the input Xmap data
     * @param partialMap basic canonical map already created from Xmap
     * @return new Xmap data (or null)
     */
    Map convertXmap(Map jobXMap, Map partialMap);
}
