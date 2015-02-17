package com.dtolabs.rundeck.core.jobs;

/**
 * Not found
 */
public class JobNotFound extends Exception {
    private String jobName;
    private String groupPath;
    private String jobId;
    private String project;

    public JobNotFound(String message, String jobId, String project) {
        super(message);
        this.jobId = jobId;
        this.project = project;
    }

    public JobNotFound(String message, String jobName, String groupPath, String project) {
        super(message);
        this.jobName = jobName;
        this.groupPath = groupPath;
        this.project = project;
    }

    public String getJobName() {
        return jobName;
    }

    public String getGroupPath() {
        return groupPath;
    }

    public String getJobId() {
        return jobId;
    }

    public String getProject() {
        return project;
    }
}
