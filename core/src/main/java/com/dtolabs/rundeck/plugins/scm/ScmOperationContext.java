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

package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.jobs.JobService;
import com.dtolabs.rundeck.core.storage.StorageTree;

/**
 * Context for scm import/export actions, including authorization info and storage tree access
 */
public interface ScmOperationContext {

    /**
     * Get the framework project name
     *
     * @return project name
     */
    public String getFrameworkProject();

    /**
     * ID of a job
     *
     * @return Job ID, or null
     */
    public String getJobId();

    /**
     * @return the authorization context
     */
    public UserAndRolesAuthContext getAuthContext();

    /**
     * @return the storage service
     */
    public StorageTree getStorageTree();

    /**
     * @return username
     */
    public ScmUserInfo getUserInfo();
}
