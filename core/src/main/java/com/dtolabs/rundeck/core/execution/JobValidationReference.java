package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.jobs.JobReference;

import java.io.Serializable;

/**
 * Job descriptor interface for validation.
 */
public interface JobValidationReference extends JobReference {

    public Serializable getDatabaseId();

    public String getUuid();

    public Boolean getMultipleExecutions();

    public Integer getMaxMultipleExecutions();

    public boolean hasSecureOptions();

    @Override
    default String getJobAndGroup() {
        return (null != getGroupPath()) ? (getGroupPath() + '/' + getJobName()) : getJobName();
    }


}
