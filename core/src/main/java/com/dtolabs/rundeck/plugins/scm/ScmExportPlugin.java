package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.jobs.JobReference;
import com.dtolabs.rundeck.core.plugins.views.Action;
import com.dtolabs.rundeck.core.plugins.views.BasicInputView;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Export plugin
 */
public interface ScmExportPlugin {

    /**
     * perform any cleanup/teardown needed after disabling
     */
    void cleanup();


    /**
     * @param actionId action ID
     *
     * @return input view for the specified action
     */
    BasicInputView getInputViewForAction(final ScmOperationContext context,String actionId);

    /**
     * @param context context map
     *
     * @return list of actions available for the context
     */
    List<Action> actionsAvailableForContext(ScmOperationContext context);

    /**
     * Perform export of the jobs
     *
     * @param jobs  jobs to be exported
     * @param input input for the action properties
     *
     * @return result of export
     */
    ScmExportResult export(
            ScmOperationContext context,
            String actionId,
            Set<JobExportReference> jobs,
            Set<String> pathsToDelete,
            Map<String, String> input
    ) throws ScmPluginException;

    /**
     * @return overall status
     */
    ScmExportSynchState getStatus(ScmOperationContext context) throws ScmPluginException;

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
     * @param job          job
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
     */
    ScmDiffResult getFileDiff(JobExportReference job);

    /**
     * Get diff for the given job against another path, e.g. the original
     * path before a rename
     *
     * @param job          job
     * @param originalPath original path
     */
    ScmDiffResult getFileDiff(JobExportReference job, String originalPath);
}
