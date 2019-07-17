package com.dtolabs.rundeck.core.jobs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
    Map<String, String> getOptionsValues();
}
