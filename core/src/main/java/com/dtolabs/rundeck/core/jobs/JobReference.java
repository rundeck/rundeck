package com.dtolabs.rundeck.core.jobs;

/**
 * A handle for identifying a job
 */
public interface JobReference {
    public String getProject();
    public String getId();
    public String getJobName();
    public String getGroupPath();
    public String getJobAndGroup();
}
