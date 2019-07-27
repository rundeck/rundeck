package com.dtolabs.rundeck.core.jobs;


import com.dtolabs.rundeck.core.common.INodeSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public interface JobPreExecutionEvent {

    /**
     *
     * @return String project name.
     */
    String getProjectName();

    /**
     *
     * @return ArrayList<LinkedHashMap> options of the job.
     */
    ArrayList<LinkedHashMap> getOptions();

    /**
     *
     * @return String user name triggering the job.
     */
    String getUserName();

    /**
     *
     * @return Map<String, String> values setup to job options.
     */
    HashMap getOptionsValues();

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
}
