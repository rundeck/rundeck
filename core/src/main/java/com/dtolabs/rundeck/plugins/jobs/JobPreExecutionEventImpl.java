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
    private String jobUUID;
    private String projectName;
    private String userName;
    private SortedSet<JobOption> options;
    private Map<String, String> optionsValues;
    private String nodeFilter;
    private INodeSet nodes;
    private Map executionMetadata;
    
    
    public JobPreExecutionEventImpl(String jobName,
                                    String jobUUID,
                                    String projectName,
                                    String userName,
                                    SortedSet<JobOption> options,
                                    Map<String, String> optionsValues,
                                    String nodeFilter,
                                    INodeSet nodes,
                                    Map executionMetadata) {
        
        this.jobName = jobName;
        this.jobUUID = jobUUID;
        this.projectName = projectName;
        this.userName = userName;
        this.options = options;
        this.optionsValues = Optional.ofNullable(optionsValues)
            .map(HashMap::new)
            .orElseGet(HashMap::new);
        this.nodeFilter = nodeFilter;
        this.nodes = nodes;
        this.executionMetadata = Optional.ofNullable(executionMetadata)
            .map(map -> new HashMap(map))
            .orElseGet(HashMap::new);
    }
    
    public JobPreExecutionEventImpl(JobPreExecutionEvent origin) {
        this(
            origin.getJobName(),
            origin.getJobUUID(),
            origin.getProjectName(),
            origin.getUserName(),
            origin.getOptions(),
            origin.getOptionsValues(),
            origin.getNodeFilter(),
            origin.getNodes(),
            origin.getExecutionMetadata());
    }
    
}
