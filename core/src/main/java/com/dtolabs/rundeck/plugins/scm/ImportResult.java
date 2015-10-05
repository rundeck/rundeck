package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.jobs.JobRevReference;

/**
 * Result of importing a job
 */
public interface ImportResult {

    /**
     * @return true if successful
     */
    boolean isSuccessful();

    /**
     * @return failure message
     */
    String getErrorMessage();

    /**
     * @return imported job
     */
    JobScmReference getJob();

    /**
     * @return true if a new job was created
     */
    boolean isCreated();

    /**
     * @return true if an existing job was modified
     */
    boolean isModified();
}
