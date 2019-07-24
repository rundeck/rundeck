package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;

import java.util.SortedSet;

public interface JobPersistEvent {

    /**
     *
     * @return String project name.
     */
    String getProjectName();

    /**
     *
     * @return List<JobOptions> options for the job.
     */
    SortedSet<JobOption> getOptions();

    /**
     *
     * @return INodeSet node set where the job will run
     */
    INodeSet getNodes();

    /**
     *
     * @return String user name doing the persist.
     */
    String getUserName();

    /**
     *
     * @return String node filter.
     */
    String getNodeFilter();

}
