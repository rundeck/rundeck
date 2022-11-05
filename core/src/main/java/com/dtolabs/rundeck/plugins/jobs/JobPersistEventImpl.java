package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.jobs.JobOption;
import com.dtolabs.rundeck.core.jobs.JobPersistEvent;
import lombok.Builder;
import lombok.Data;

import java.util.SortedSet;

@Data
@Builder
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

}
