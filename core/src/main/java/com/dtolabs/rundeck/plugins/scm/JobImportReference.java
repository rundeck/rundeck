package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.jobs.JobRevReference;

import java.util.Map;

/**
 * Job reference which has SCM import metadata
 */
public interface JobImportReference extends JobRevReference {
    /**
     * @return metadata about tracked job
     */
    Map getScmImportMetadata();
    /**
     * @return the version of the job associated with the import metadata
     */
    public Long getImportVersion();
}
