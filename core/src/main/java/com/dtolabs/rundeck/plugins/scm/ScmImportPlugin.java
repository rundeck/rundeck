/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     * @param importer can import files as jobs
     */
    ScmExportResult scmImport(
            ScmOperationContext context,
            String actionId,
            JobImporter importer,
            List<String> selectedPaths,
            Map<String, String> input
    ) throws ScmPluginException;

    /**
     * @return overall status
     */
    ScmImportSynchState getStatus(ScmOperationContext context) throws ScmPluginException;

    /**
     * Return the state of the given job
     *
     * @param job job
     *
     * @return state
     */
    JobImportState getJobStatus(JobScmReference job);


    /**
     * Return the state of the given job, with optional original repo path
     *
     * @param job          job
     * @param originalPath path of original job, e.g. if the file was renamed
     *
     * @return state
     */
    JobImportState getJobStatus(JobScmReference job, String originalPath);

    /**
     * Return the state of the given job
     *
     * @param event     change event
     * @param reference job
     *
     * @return state
     */
    JobImportState jobChanged(JobChangeEvent event, JobScmReference reference);


    /**
     * perform any cleanup/teardown needed after disabling
     */
    void cleanup();


    /**
     * Provide the input view for an action.
     *
     * @param actionId action ID
     *
     * @return input view for the specified action
     */
    BasicInputView getInputViewForAction(final ScmOperationContext context, String actionId);

    /**
     * Return any action that is needed for post-create setup.  If not null,
     * then the user will be forwarded to this action after plugin is configured, this should
     * always return the setup action even if setup has already been performed, as
     * the user may disable/reconfigure the plugin.
     *
     * @param context context map
     *
     * @return action used for post-create setup, if necessary
     */
    Action getSetupAction(ScmOperationContext context);

    /**
     * @param context context map
     *
     * @return list of actions available for the context
     */
    List<Action> actionsAvailableForContext(ScmOperationContext context);


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
    ScmImportDiffResult getFileDiff(JobScmReference job);

    /**
     * Get diff for the given job against another path, e.g. the original
     * path before a rename
     *
     * @param job          job
     * @param originalPath original path
     */
    ScmImportDiffResult getFileDiff(JobScmReference job, String originalPath);
}
