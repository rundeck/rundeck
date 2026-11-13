package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;

import java.util.SortedSet;

/**
 * Event passed to Job Lifecycle plugins before a job is saved.
 * <p>
 * The job execution settings ({@link #getTimeout()}, {@link #getRetry()}, {@link #getNodeThreadcount()},
 * {@link #getMultipleExecutions()}, {@link #getMaxMultipleExecutions()}) are read-only: changing them on the event
 * does not change the saved job. Only the options can be changed, through {@link JobLifecycleStatus#getOptions()}.
 */
public interface JobPersistEvent extends JobEvent {

    /**
     *
     * @return String Job name.
     */
    String getJobName();

    /**
     *
     * @return String project name.
     */
    String getProjectName();

    /**
     *
     * @return List<JobOptions> options for the job.
     */
    SortedSet<JobOption> getOptions();

    /**
     *
     * @return INodeSet node set where the job will run
     */
    INodeSet getNodes();

    /**
     *
     * @return String user name doing the persist.
     */
    String getUserName();

    /**
     *
     * @return String node filter.
     */
    String getNodeFilter();

    /**
     * Raw job timeout as defined on the job, unparsed. May be a number of seconds (e.g. "3600"), a duration with
     * units (e.g. "2h30m"), or an option reference (e.g. "${option.timeout}").
     *
     * @return String job timeout, or null if not set
     */
    default String getTimeout() {
        return null;
    }

    /**
     * Raw job retry count as defined on the job, unparsed. May be an option reference.
     *
     * @return String retry count, or null if not set
     */
    default String getRetry() {
        return null;
    }

    /**
     * Raw node dispatch thread count as defined on the job, unparsed. May be a number (e.g. "4") or an option
     * reference (e.g. "${option.threads}").
     *
     * @return String thread count, or null if not set or if the job does not dispatch to nodes
     */
    default String getNodeThreadcount() {
        return null;
    }

    /**
     * @return Boolean true if the job allows multiple concurrent executions, or null if not set
     */
    default Boolean getMultipleExecutions() {
        return null;
    }

    /**
     * Raw limit of concurrent executions, unparsed. May be an option reference. Only applies when
     * {@link #getMultipleExecutions()} is true.
     *
     * @return String max multiple executions, or null if not set
     */
    default String getMaxMultipleExecutions() {
        return null;
    }

}
