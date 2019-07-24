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

package com.dtolabs.rundeck.core.schedule;

import java.util.Date;
import java.util.List;
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

    /**
     * In cluster mode, return true if the scheduleOWner should change to current node.
     *
     * @param name job name
     * @param group job group
     * @param data map with job information, jobid and current schedule owner.
     * @return true if the scheduleOWner should change to current node.
     */
    boolean updateScheduleOwner(String name, String group, Map data);

    /**
     * Return the uuid of the node that will execute the scheduled execution.
     *
     * @param name job name
     * @param group job group
     * @param data map with job informations.
     * @param project projectName
     * @return uuid of node for the scheduled execution
     */
    String determineExecNode(String name, String group, Map data, String project);


    /**
     * Return list dead cluster members.
     *
     * @return list dead cluster members
     */
    List<String> getDeadMembers(String uuid);
}
