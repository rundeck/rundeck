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

import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.util.List;
import java.util.Map;

/**
 * Defines query interface
 */
public interface JobQuery {

    /**
     * Extend criteria builder input
     *
     * @param input    query input object
     * @param params   all request parameters
     * @param delegate criteria builder delegate
     * @return map with filtered values to show in jobs list page
     */
    Map extendCriteria(final JobQueryInput input, Map params, Object delegate);

    /**
     * @return list of input properties for query, added to the Job Query form
     */
    List<Property> getQueryProperties();
}
