package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.jobs.JobExportReference;
import com.dtolabs.rundeck.core.jobs.JobReference;
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
    String export(
            Set<JobExportReference> jobs,
            Set<String> pathsToDelete,
            ScmUserInfo userInfo,
            Map<String, Object> input
    ) throws ScmPluginException;

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
     * Return the state of the given job, with optional original repo path
     *
     * @param job job
     * @param originalPath path of original job, e.g. if the file was renamed
     *
     * @return state
     */
    JobState getJobStatus(JobExportReference job, String originalPath);

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
    File getLocalFileForJob(JobReference job);

    /**
     * Return the relative path for the job in the repo
     *
     * @param job job
     *
     * @return state
     */
    String getRelativePathForJob(JobReference job);

    /**
     * Get diff for the given job
     *
     * @param job job
     *
     */
    ScmDiffResult getFileDiff(JobExportReference job);

    /**
     * Get diff for the given job against another path, e.g. the original
     * path before a rename
     *
     * @param job job
     * @param originalPath original path
     *
     */
    ScmDiffResult getFileDiff(JobExportReference job, String originalPath);
}
