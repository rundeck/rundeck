package com.dtolabs.rundeck.core.jobs;

/**
 * Service for interacting with Jobs
 */
public interface JobService {

    /**
     * @param uuid    job ID
     * @param project the project
     *
     * @return a reference to the job by the ID
     *
     * @throws com.dtolabs.rundeck.core.jobs.JobNotFound if the job was not found
     */
    JobReference jobForID(String uuid, String project) throws JobNotFound;

    /**
     * @param name    the job group path/name string
     * @param project the project
     *
     * @return a job reference for the group/name
     *
     * @throws com.dtolabs.rundeck.core.jobs.JobNotFound if the job was not found
     */
    JobReference jobForName(String name, String project) throws JobNotFound;

    /**
     * @param group   group path or null
     * @param name    job name
     * @param project the project
     *
     * @return a job reference for the group and name
     *
     * @throws com.dtolabs.rundeck.core.jobs.JobNotFound if the job was not found
     */
    JobReference jobForName(String group, String name, String project) throws JobNotFound;


    /**
     * @param jobReference reference to a job
     * @return state of the job
     */
    JobState getJobState(JobReference jobReference) throws JobNotFound;
}
