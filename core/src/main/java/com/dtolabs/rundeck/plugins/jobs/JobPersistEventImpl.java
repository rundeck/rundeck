package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.jobs.JobOption;
import com.dtolabs.rundeck.core.jobs.JobPersistEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.*;

/**
 * Default implementation of {@link JobPersistEvent}, passed to Job Lifecycle plugins before a job is saved. Create
 * it with {@link #builder()}.
 */
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JobPersistEventImpl implements JobPersistEvent {

    private String jobName;
    private String projectName;
    private SortedSet<JobOption> options;
    private INodeSet nodes;
    private String userName;
    private String nodeFilter;
    private String timeout;
    private String retry;
    private String nodeThreadcount;
    private Boolean multipleExecutions;
    private String maxMultipleExecutions;

    /**
     * Create an event without job execution settings. Kept for backwards compatibility.
     *
     * @param jobName     job name
     * @param projectName project name
     * @param options     job options
     * @param nodes       node set where the job will run
     * @param userName    user persisting the job
     * @param nodeFilter  node filter
     */
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

    /**
     * Copy constructor, copies all values from the origin event.
     *
     * @param origin event to copy
     */
    public JobPersistEventImpl(JobPersistEvent origin) {
        this(origin.getJobName(),
            origin.getProjectName(),
            origin.getOptions(),
            origin.getNodes(),
            origin.getUserName(),
            origin.getNodeFilter(),
            origin.getTimeout(),
            origin.getRetry(),
            origin.getNodeThreadcount(),
            origin.getMultipleExecutions(),
            origin.getMaxMultipleExecutions()
        );
    }

}
