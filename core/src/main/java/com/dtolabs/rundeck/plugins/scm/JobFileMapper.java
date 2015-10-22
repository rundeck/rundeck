package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.jobs.JobReference;

import java.io.File;

/**
 * Created by greg on 5/1/15.
 */
public interface JobFileMapper {
    public File fileForJob(JobReference jobReference);
}
