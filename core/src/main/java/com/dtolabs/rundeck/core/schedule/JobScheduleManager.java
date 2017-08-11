/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.schedule;

import java.util.Date;
import java.util.Map;

/**
 * Manage scheduling jobs
 *
 * @author greg
 * @since 7/26/17
 */
public interface JobScheduleManager {

    /**
     * Delete a name/group from the scheduler
     *
     * @param name  name
     * @param group group
     */
    void deleteJobSchedule(String name, String group);

    /**
     * Schedule a job for a certain time
     *
     * @param name   name
     * @param group  group
     * @param data   data
     * @param atTime time to run
     *
     * @return time to run
     */
    Date scheduleJob(String name, String group, Map data, Date atTime) throws JobScheduleFailure;

    /**
     * Schedule a job to run now
     *
     * @param name  name
     * @param group group
     * @param data  data
     *
     * @return true if successful
     */
    boolean scheduleJobNow(String name, String group, Map data) throws JobScheduleFailure;

}
