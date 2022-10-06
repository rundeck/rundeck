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

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import com.dtolabs.rundeck.core.execution.PreparedExecutionReference;
import com.dtolabs.rundeck.core.jobs.JobReference;

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
     * @param quartzJobName  QUARTZ job name
     * @param quartzJobGroup QUARTZ group name
     */
    void deleteJobSchedule(String quartzJobName, String quartzJobGroup);

    /**
     * Schedule a job for a certain time
     *
     * @param quartzJobName  QUARTZ job name
     * @param quartzJobGroup QUARTZ group name
     * @param data   data
     * @param atTime time to run
     * @param pending if job should be in a pending state
     *
     * @return time to run
     */
    Date scheduleJob(String quartzJobName, String quartzJobGroup, Map data, Date atTime, boolean pending) throws JobScheduleFailure;

    /**
     * Schedule a job to run now
     *
     * @param quartzJobName  QUARTZ job name
     * @param quartzJobGroup QUARTZ group name
     * @param data  data
     * @param pending if job should be scheduled in a pending state
     * @return true if successful
     */
    boolean scheduleJobNow(String quartzJobName, String quartzJobGroup, Map data, boolean pending) throws JobScheduleFailure;

    /**
     * Schedule a job that was previously scheduled as pending
     * @param quartzJobName job name
     * @param quartzJobGroup job group
     *
     * @return time to run
     */
    Date reschedulePendingJob(String quartzJobName, String quartzJobGroup) throws JobScheduleFailure;

    /**
     * In cluster mode, return true if the scheduleOWner should change to current node.
     *
     * @param data map with job information, jobid and current schedule owner.
     * @return true if the scheduleOWner should change to current node.
     */
    boolean updateScheduleOwner(JobReference data);

    /**
     * Return the uuid of the node that will execute the scheduled execution.
     *
     * @param name job name
     * @param group job group
     * @param data map with job informations.
     * @param project projectName
     * @return uuid of node for the scheduled execution
     */
    String determineExecNode(JobReference job);


    /**
     * Return list dead cluster members.
     *
     * @return list dead cluster members
     */
    List<String> getDeadMembers(String uuid);

    /**
     * Tries to acquire the history cleaner ownership
     * (only acquired if the owner of the job is dead)
     * @param uuid of the server that is trying to acquire ownership
     * @param project the project where the job runs
     * @return true if the ownership was aquired
     */
    boolean tryAcquireExecCleanerJob(String uuid, String project);

    /**
     * Schedule a job to run later
     *
     * @param data  dataRundeckproClusterGrailsPlugin
     *
     * @return true if successful
     */
    default boolean scheduleRemoteJob(Map data){
        return false;
    }

    /**
     * Defines behavior of beforeExecution
     */
    enum BeforeExecutionBehavior {
        proceed,
        skip
    }

    /**
     * Pre-run hook before a Job/adhoc is executed
     *
     * @param execution execution detail
     * @return proceed or skip
     */
    default BeforeExecutionBehavior beforeExecution(PreparedExecutionReference execution, Map<String,Object> jobDataMap, UserAndRolesAuthContext authContext) {
        return BeforeExecutionBehavior.proceed;
    }

    /**
     * Post-run hook after a job/adhoc is executed
     *
     * @param execution execution detail
     */
    default void afterExecution(PreparedExecutionReference execution, Map<String,Object> jobDataMap, UserAndRolesAuthContext authContext) {

    }
}
