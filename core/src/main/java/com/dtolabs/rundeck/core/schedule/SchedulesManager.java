package com.dtolabs.rundeck.core.schedule;


import org.rundeck.app.components.schedule.TriggerBuilderHelper;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Manage jobs schedules
 *
 * @author ronaveva
 * @since 12/16/19
 */
public interface SchedulesManager {

    /**
     * It returns true if schedules are enabled
     * @return boolean
     */
    boolean isSchedulesEnable();

    /**
     * It handles the cleaning of no longer associated schedule definitions and triggers the new ones
     * @param jobUUID
     * @param isUpdate it forces to recreate the job trigger
     * @return boolean it returns true if at least one job was scheduled
     */
    Map handleScheduleDefinitions(String jobUUID, boolean isUpdate);

    /**
     * It creates a job trigger
     * @param jobName
     * @param jobGroup
     * @param cronExpression
     * @param priority
     */
    TriggerBuilderHelper createTriggerBuilder(String jobName, String jobGroup, String cronExpression, int priority);

    /**
     * It creates a job trigger
     * @param jobUUID
     * @param cronExpression
     * @param triggerName
     */
    TriggerBuilderHelper createTriggerBuilder(String jobUUID, String cronExpression, String triggerName);

    /**
     * Return the next scheduled or predicted execution time for the scheduled job, and if it is not scheduled
     * return a time in the future.  If the job is not scheduled on the current server (cluster mode), returns
     * the time that the job is expected to run on its configured server.
     * @param jobUUID
     * @param require
     * @return Date
     */
    Date nextExecutionTime(String jobUUID, boolean require);

    /**
     * Returns true if the job is set to schedule
     * @param uuid
     * @return boolean
     */
    boolean isScheduled(String uuid);

    /**
     * list scheduled jobs which match the given serverUUID, or all jobs if it is null.
     * @param serverUUID
     * @param project
     * @return
     */
    List getAllScheduled(String serverUUID, String project);


    /**
     * Returns true if the job should be scheduled either by its own schedule or by an schedule definition
     * @param uuid
     * @return boolean
     */
    boolean shouldScheduleExecution(String uuid);

    /**
     * It persist the schedules assigned to the job
     * @param uuid job uuid
     * @param schedules schedules list to add
     * @param shouldSchedule indicates whether should add the schedules or not
     * @return Map
     */
    void persistSchedulesToJob(String uuid, List schedules, Boolean shouldSchedule, String project);

    /**
     * It returns all of the schedule definitions associated to the job
     * @param uuid job uuid
     * @param project project name
     * @return List
     */
    List getJobSchedules(String uuid, String project);

    /**
     * Gets a list of scheduled executions
     * @param toServerUUID
     * @param fromServerUUID
     * @param selectAll
     * @param projectFilter
     * @param jobids
     * @return List
     */
    List getSchedulesJobToClaim(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter, List<String> jobids);

    /**
     * Returns a list of dates in a time lapse between now and the to Date.
     * @param jobUuid
     * @param to Date in the future
     * @param past It will be used to calculate to current date
     * @return list of dates
     */
    List<Date> nextExecutions(String jobUuid, Date to, boolean past);
}