package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.jobs.JobExportReference;
import com.dtolabs.rundeck.core.jobs.JobRevReference;
import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Export plugin
 */
public interface ScmExportPlugin {
    /**
     * perform any setup/refresh needed after creation
     */
    void initialize();

    /**
     * perform any cleanup/teardown needed after disabling
     */
    void cleanup();

    /**
     * Define any UI properties for export action
     *
     * @param jobs set of jobs to list for commit
     *
     * @return list of properties
     */
    List<Property> getExportProperties(Set<JobRevReference> jobs);

    /**
     * Perform export of the jobs
     *
     * @param jobs  jobs to be exported
     * @param input input for the {@link #getExportProperties(Set)}
     *
     * @return id of commit
     */
    String export(Set<JobExportReference> jobs, Set<String> pathsToDelete, Map<String, Object> input) throws ScmPluginException;

    /**
     * @return overall status
     */
    ScmExportSynchState getStatus();

    /**
     * Return the state of the given job
     *
     * @param job job
     *
     * @return state
     */
    JobState getJobStatus(JobExportReference job);

    /**
     * Return a list of tracked files that have been deleted.
     */
    List<String> getDeletedFiles();

    /**
     * Return the state of the given job
     *
     * @param event           change event
     * @param exportReference serialize the job
     *
     * @return state
     */
    JobState jobChanged(JobChangeEvent event, JobExportReference exportReference);

    /**
     * Return the File for the given job
     *
     * @param job job
     *
     * @return state
     */
    File getLocalFileForJob(JobRevReference job);

    /**
     * Return the relative path for the job in the repo
     *
     * @param job job
     *
     * @return state
     */
    String getRelativePathForJob(JobRevReference job);

    /**
     * Get diff for the given job
     *
     * @param job job
     *
     * @return TODO: diff result type
     */
    ScmDiffResult getFileDiff(JobExportReference job);
}
