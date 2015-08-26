package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.jobs.JobReference;
import com.dtolabs.rundeck.plugins.scm.JobSerializer;

/**
 * A Job reference with an internal version number
 */
public interface JobRevReference extends JobReference {
    public Long getVersion();

}
