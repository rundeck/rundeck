package com.dtolabs.rundeck.core.jobs;


import com.dtolabs.rundeck.core.common.INodeSet;

import java.util.Map;
import java.util.SortedSet;

public interface JobPreExecutionEvent {

    /**
     *
     * @return String Job name.
     */
    String getJobName();

    /**
     *
     * @return Job UUID
     */
    String getJobUUID();

    /**
     *
     * @return String project name.
     */
    String getProjectName();

    /**
     *
     * @return String user name triggering the job.
     */
    String getUserName();

    /**
     *
     * @return List<LinkedHashMap> options of the job.
     */
    SortedSet<JobOption> getOptions();

    /**
     *
     * @return Map<String, String> values setup to job options.
     */
    Map<String, String> getOptionsValues();

    /**
     *
     * @return String node filter
     */
    String getNodeFilter();

    /**
     *
     * @return INodeSet node set where the job will run
     */
    INodeSet getNodes();
    
    /**
     * @return Execution metadata map.
     */
    Map getExecutionMetadata();
}
