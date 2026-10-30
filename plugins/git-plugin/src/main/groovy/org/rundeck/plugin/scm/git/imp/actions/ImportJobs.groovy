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

package org.rundeck.plugin.scm.git.imp.actions

import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.JobImporter
import com.dtolabs.rundeck.plugins.scm.JobRenamed
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportResultImpl
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import org.eclipse.jgit.treewalk.TreeWalk
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.BuilderUtil
import org.rundeck.plugin.scm.git.GitImportAction
import org.rundeck.plugin.scm.git.GitImportPlugin
import org.rundeck.plugin.scm.git.GitUtil
import org.rundeck.plugin.scm.git.JobRenamedImp

/**
 * Action to import selected jobs from git HEAD commit
 */
class ImportJobs extends BaseAction implements GitImportAction {
    ImportJobs(final String id, final String title, final String description, final String iconName) {
        super(id, title, description, iconName)
    }

    BasicInputView getInputView(final ScmOperationContext context, GitImportPlugin plugin) {
        BuilderUtil.inputViewBuilder(id) {
            title "Import remote Changes"
            description '''Import the modifications to Rundeck'''
            buttonTitle "Import"
            properties([])
        }
    }

    @Override
    ScmExportResult performAction(
        final ScmOperationContext context,
        final GitImportPlugin plugin,
        final JobImporter importer,
        final List<String> selectedPaths,
        final Map<String, String> input
    ) {
        performAction(context, plugin, importer, selectedPaths, null, input)
    }

    ScmExportResult performAction(
            final ScmOperationContext context,
            final GitImportPlugin plugin,
            final JobImporter importer,
            final List<String> selectedPaths,
            final List<String> deletedJobs,
            final Map<String, String> input
    ) {
        //perform git
        StringBuilder sb = new StringBuilder()
        boolean success = true

        deletedJobs?.each { jobId ->
            def importResult = importer.deleteJob(
                    context.frameworkProject,
                    jobId
            )
            if (!importResult.successful) {
                success = false
                sb << ("Failed deleting job with id: ${jobId}: " + importResult.errorMessage)
            } else {
                //stop tracking every path mapped to this job so getStatusInternal doesn't keep
                //reporting DELETE_NEEDED for a job that has already been deleted
                plugin.importTracker.untrackJob(jobId)
                sb << ("Succeeded deleting job with id ${jobId} ")
            }
        }

        def jobsChanged = []

        //walk the repo files and look for possible candidates
        plugin.walkTreePaths('HEAD^{tree}', true) { TreeWalk walk ->
            def path = walk.getPathString()
            if (!(path in selectedPaths)) {
                plugin.log.debug("not selected, skipping path ${path}")
                return
            }
            def objectId = walk.getObjectId(0)
            plugin.log.debug("import data: ${path}")
            byte[] bytes
            try {
                bytes = GitUtil.getBytes(plugin.repo, objectId)
            } catch (ScmPluginException e) {
                //record and skip, rather than let it propagate: an exception here would abort the
                //whole walk, leaving jobsChanged incomplete and skipping refreshJobsStatus() below
                //for every job already imported earlier in this same batch
                success = false
                sb << ("Failed importing: ${path}: ${e.message}")
                return
            }

            def commit = plugin.lastCommitForPath(path)
            def meta = GitUtil.metaForCommit(commit)
            meta.url = plugin.config.url

            JobRenamedImp renamedJob = null
            if (plugin.importTracker.originalValue(path)) {
                //originalValue(path) only identifies this as a rename - the job's forward mapping
                //at its old path is already gone by now (jobRenamed's untrackJobPaths cleared it
                //when the rename was first detected), so the UUID must be resolved through the
                //current path, not the stale original one, or renamedJob.uuid comes back null and
                //ScmJobImporter.importJob's `jobData.job.uuid = renamedJob.uuid` creates a new job
                //instead of updating the existing one
                def jobUUID = plugin.importTracker.trackedJob(path)
                def jobCache = plugin.jobStateMap[jobUUID]
                renamedJob = new JobRenamedImp(uuid: jobUUID)
                if (jobCache?.sourceId) {
                    renamedJob.sourceId = jobCache.sourceId
                }
            }

            def importResult = importer.importFromStream(
                    plugin.config.format,
                    new ByteArrayInputStream(bytes),
                    meta,
                    plugin.config.importPreserve,
                    renamedJob
            )

            if (!importResult.successful) {
                success = false
                sb << ("Failed importing: ${walk.getPathString()}: " + importResult.errorMessage)
            } else {
                if (importResult.getJob()) {
                    jobsChanged.add(importResult.getJob())
                    plugin.importTracker.trackJobAtPath(importResult.job, walk.getPathString())
                }
                sb << ("Succeeded importing ${walk.getPathString()}: ${importResult}")
            }
        }

        if (jobsChanged) {
            plugin.refreshJobsStatus(jobsChanged)
        }

        def result = new ScmExportResultImpl()
        result.success = success
        result.message = "Git Import " + (success ? "successful" : "failed")
        result.extendedMessage = sb.toString()
        return result
    }
}