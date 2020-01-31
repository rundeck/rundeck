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

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.util.List;
import java.util.Map;

/**
 * extension for importing job definitions
 */
public interface JobImport {
    /**
     * @return a unique name to associated imported objects temporarily
     */
    String getName();

    /**
     * Import job map data, if necessary return a temporary object associated with the Job
     *
     * @param job        the defined job item
     * @param jobDataMap final canonical job data map
     * @return associated object
     */
    Object importCanonicalMap(Object job, Map jobDataMap);

    /**
     * Validate the object
     *
     * @param job       job
     * @param associate associated object
     * @return true if valid
     */
    boolean validateImported(Object job, Object associate);

    /**
     * Import request parameters, if necessary return a temporary object associated with the Job
     *
     * @param job    the defined job item
     * @param params parameter map
     * @return associated object
     */
    Object importJobParams(Object job, Map params);

    /**
     * Update a job
     * @param job job to update
     * @param imported imported job definition to apply to update job
     * @param associate associated object created via {@link #importCanonicalMap(Object, Map)} or {@link #importJobParams(Object, Map)}
     * @param params web params
     * @return
     */
    Object updateJob(Object job, Object imported, Object associate, Map params);

    /**
     * Persist the changes for the associated object for the job
     *
     * @param job         the job
     * @param associate   associated object
     * @param authContext auth context
     */
    void persist(Object job, Object associate, UserAndRolesAuthContext authContext);

    /**
     * Callback after persist for the job completes
     *
     * @param job         the job
     * @param associate   associated object
     * @param authContext auth context
     */
    void wasPersisted(Object job, Object associate, UserAndRolesAuthContext authContext);

    /**
     * The indicated job will be deleted
     *
     * @param job
     * @param authContext
     */
    void willDeleteJob(Object job, AuthContext authContext);

    /**
     * The indicated job was deleted
     *
     * @param job
     * @param authContext
     */
    void didDeleteJob(Object job, AuthContext authContext);

    /**
     * Import
     *
     * @param jobXMap    the input Xmap data
     * @param partialMap basic canonical map already created from Xmap
     * @return new map data (or null)
     */
    Map convertXmap(Map jobXMap, Map partialMap);
}
