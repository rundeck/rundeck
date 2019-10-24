package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.jobs.JobOption;
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;

import java.util.*;

public class JobPreExecutionEventImpl implements JobPreExecutionEvent {

    private String jobName;
    private String projectName;
    private String userName;
    private HashMap optionsValues;
    private String nodeFilter;
    private INodeSet nodes;
    private SortedSet<JobOption> options;


    public JobPreExecutionEventImpl(String jobName,
                                    String projectName,
                                    String userName,
                                    HashMap optionsValues,
                                    INodeSet nodes,
                                    String nodeFilter,
                                    SortedSet<JobOption> options) {

        this.jobName = jobName;
        this.projectName = projectName;
        this.userName = userName;
        if(optionsValues != null){
            this.optionsValues = (HashMap) optionsValues.clone();
        }else{
            this.optionsValues = new HashMap();
        }
        this.nodes = nodes;
        this.nodeFilter = nodeFilter;
        this.options = options;
    }

    public JobPreExecutionEventImpl(JobPreExecutionEventImpl origin) {
        this(origin.jobName, origin.projectName, origin.userName, origin.optionsValues, origin.nodes, origin.nodeFilter, origin.options);
    }

    @Override
    public String getJobName() {
        return this.jobName;
    }

    @Override
    public String getProjectName() {
        return this.projectName;
    }

    @Override
    public SortedSet<JobOption> getOptions() {
        return this.options;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    @Override
    public HashMap getOptionsValues() {
        return this.optionsValues;
    }

    @Override
    public String getNodeFilter() {
        return this.nodeFilter;
    }

    @Override
    public INodeSet getNodes() { return this.nodes; }

}
