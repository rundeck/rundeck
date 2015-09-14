package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.jobs.JobReference;
import com.dtolabs.rundeck.core.plugins.views.Action;
import com.dtolabs.rundeck.core.plugins.views.BasicInputView;

import java.util.List;
import java.util.Map;

/**
 * Plugin for importing job definitions from a Source control repository
 */
public interface ScmImportPlugin {

    /**
     * Perform import with the input
     *
     * @param input    result of GUI input
     * @param importer TODO can import files as jobs
     */
    ScmExportResult scmImport(
            String actionId,
            JobImporter importer,
            List<String> selectedPaths,
            Map<String, Object> input
    ) throws ScmPluginException;

    /**
     * @return overall status
     */
    ScmImportSynchState getStatus();

    /**
     * Return the state of the given job
     *
     * @param job job
     *
     * @return state
     */
    JobImportState getJobStatus(JobImportReference job);

    /**
     * Return the state of the given job, with optional original repo path
     *
     * @param job          job
     * @param originalPath path of original job, e.g. if the file was renamed
     *
     * @return state
     */
    JobImportState getJobStatus(JobImportReference job, String originalPath);


    /**
     * perform any cleanup/teardown needed after disabling
     */
    void cleanup();


    /**
     * @param actionId action ID
     *
     * @return input view for the specified action
     */
    BasicInputView getInputViewForAction(String actionId);

    /**
     * @param context context map
     *
     * @return list of actions available for the context
     */
    List<Action> actionsAvailableForContext(final Map<String, String> context);



    /**
     * list any known items that can be tracked, such as: all files found in the
     * selected source repository branch, can be an empty list (no items found
     * to track) or null (item tracking is not used)
     */
    List<ScmImportTrackedItem> getTrackedItemsForAction(String actionId);

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
    ScmDiffResult getFileDiff(JobScmReference job);

    /**
     * Get diff for the given job against another path, e.g. the original
     * path before a rename
     *
     * @param job          job
     * @param originalPath original path
     */
    ScmDiffResult getFileDiff(JobScmReference job, String originalPath);
}
