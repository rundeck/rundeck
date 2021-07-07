package com.dtolabs.rundeck.plugins.scm;

/**
 * Get source and UUID form a renamed Job
 */
public interface JobRenamed {
    /**
     * get the job the UUID
     *
     * @return uuid
     */
    String getUuid();

    /**
     * get the job the SourceID
     *
     * @return sourceID
     */
    String getSourceId();

    /**
     * get the job the renamed path
     *
     * @return renamedPath
     */
    String getRenamedPath();
}
