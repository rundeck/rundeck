package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.jobs.JobOption;
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

@Data
@Builder
public class JobPreExecutionEventImpl implements JobPreExecutionEvent {

    private String jobName;
    private String projectName;
    private String userName;
    private Map optionsValues;
    private String nodeFilter;
    private INodeSet nodes;
    private SortedSet<JobOption> options;
    
    private Map executionMetadata;

    
    public JobPreExecutionEventImpl(String jobName,
                                    String projectName,
                                    String userName,
                                    Map optionsValues,
                                    INodeSet nodes,
                                    String nodeFilter,
                                    SortedSet<JobOption> options,
                                    Map executionMetadata) {

        this.jobName = jobName;
        this.projectName = projectName;
        this.userName = userName;
        this.nodes = nodes;
        this.nodeFilter = nodeFilter;
        this.options = options;
        this.optionsValues = Optional.ofNullable(optionsValues)
            .map(map -> new HashMap(map))
            .orElseGet(HashMap::new);
        this.executionMetadata = Optional.ofNullable(executionMetadata)
            .map(map -> new HashMap(map))
            .orElseGet(HashMap::new);
    }

    public JobPreExecutionEventImpl(JobPreExecutionEvent origin) {
        this(
            origin.getJobName(), 
            origin.getProjectName(),
            origin.getUserName(),
            origin.getOptionsValues(),
            origin.getNodes(),
            origin.getNodeFilter(),
            origin.getOptions(),
            origin.getExecutionMetadata());
    }

}
