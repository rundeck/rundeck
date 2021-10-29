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

package com.dtolabs.rundeck.core.jobs;

/**
 * A handle for identifying a job
 */
public interface JobReference {

    /**
     * @return Job's project name
     */
    public String getProject();

    /**
     * Returns the job UUID, or the database assigned id if there is no uuid.
     */
    public String getId();

    /**
     * @return The job name
     */
    public String getJobName();

    /**
     * @return Job group path.
     */
    public String getGroupPath();

    /**
     * @return Job and group path combined.
     */
    public String getJobAndGroup();

    /**
     * @return server UUID
     */
    String getServerUUID();

    /**
     * @return original job name in case of renaming
     */
    default String getOriginalQuartzJobName(){ return null; }

    /**
     * @return original group name in case of renaming
     */
    default String getOriginalQuartzGroupName(){ return null; }
}
