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
     * perform a total clean
     */
    default void totalClean(){}

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
     * Return the state of the given job, with optional original repo path
     *
     * @param job          job
     * @param originalPath path of original job, e.g. if the file was renamed
     * @param serialize false to avoid serialize twice a job
     *
     * @return state
     */
    default JobState getJobStatus(JobExportReference job, String originalPath, boolean serialize){
        return getJobStatus(job, originalPath);
    }

    /**
     * Set default job status
     *
     * @param jobs
     */
    default void initJobsStatus(List<JobExportReference> jobs) {

    }

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


    /**
     * Function to fix status of the jobs on cluster environment.
     * To automatically match the job status on every node.
     *
     * @param jobs rundeck jobs
     * @return map with information on the process
     */
    default Map clusterFixJobs(ScmOperationContext context, List<JobExportReference> jobs){
        return clusterFixJobs(context, jobs, null);
    }
    /**
     * Function to fix status of the jobs on cluster environment.
     * To automatically match the job status on every node.
     *
     * @param jobs rundeck jobs
     * @param originalPaths map of job ID to original path if the job has been renamed
     * @return map with information on the process
     */
    default Map clusterFixJobs(ScmOperationContext context, List<JobExportReference> jobs, Map<String,String> originalPaths){
        return null;
    }

    /**
     * Function to refresh all job status .
     * will upgrade the jobs cache status .
     *
     * @param jobs rundeck jobs
     * @return map with information on the process
     */
    default void refreshJobsStatus(List<JobExportReference> jobs){
    }

    /**
     * It gets the action id for push action
     *
     * @return action name id for push
     */
    default String getExportPushActionId(){
        return null;
    }

    /**
     * Returns true or false if the user has access to the key/password or not
     *
     * @param ctx: ScmOperationContext object from the controller.
     * @return true or false
     */
    default Boolean userHasAccessToKeyOrPassword(ScmOperationContext ctx){
        return true;
    }
}
