package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.jobs.JobOption;
import com.dtolabs.rundeck.core.jobs.JobPersistEvent;
import lombok.Builder;
import lombok.Data;

import java.util.*;

@Data
@Builder
public class JobPersistEventImpl implements JobPersistEvent {
    
    private String jobName;
    private String projectName;
    private SortedSet<JobOption> options;
    private INodeSet nodes;
    private String userName;
    private String nodeFilter;
    
    public JobPersistEventImpl(String jobName,
                               String projectName,
                               SortedSet<JobOption> options,
                               INodeSet nodes,
                               String userName,
                               String nodeFilter) {
        this.jobName = jobName;
        this.projectName = projectName;
        this.options = options;
        this.nodes = nodes;
        this.userName = userName;
        this.nodeFilter = nodeFilter;
    }
    
    public JobPersistEventImpl(JobPersistEvent origin) {
        this(origin.getJobName(),
            origin.getProjectName(),
            origin.getOptions(),
            origin.getNodes(),
            origin.getUserName(),
            origin.getNodeFilter()
        );
    }
    
}
