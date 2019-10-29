package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.jobs.JobOption;
import com.dtolabs.rundeck.core.jobs.JobPersistEvent;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;

import java.util.*;

public class JobPersistEventImpl implements JobPersistEvent {

    private String jobName;
    private String projectName;
    private SortedSet<JobOption> options;
    private INodeSet nodes;
    private String userName;
    private String nodeFilter;

    public JobPersistEventImpl(
            String jobName,
            String projectName,
            String userName,
            INodeSet nodes,
            String nodeFilter,
            SortedSet<JobOption> options){
        this.jobName = jobName;
        this.projectName = projectName;
        this.userName = userName;
        this.nodes = nodes;
        this.nodeFilter = nodeFilter;
        this.options = options;
    }

    public JobPersistEventImpl(JobPersistEvent origin) {
        this(
                origin.getJobName(),
                origin.getProjectName(),
                origin.getUserName(),
                origin.getNodes(),
                origin.getNodeFilter(),
                origin.getOptions()
        );
    }

    @Override
    public String getJobName() { return this.jobName; }

    @Override
    public String getProjectName() { return this.projectName; }

    @Override
    public SortedSet<JobOption> getOptions() { return this.options; }

    @Override
    public INodeSet getNodes() { return this.nodes; }

    public void setNodes(INodeSet nodeSet) { this.nodes = nodeSet; }

    @Override
    public String getUserName() { return this.userName; }

    @Override
    public String getNodeFilter() { return this.nodeFilter; }

    public void setOptions(SortedSet<JobOption> options) { this.options = options; }

}
