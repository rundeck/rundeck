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
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportResultImpl
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.treewalk.TreeWalk
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.BuilderUtil
import org.rundeck.plugin.scm.git.GitImportAction
import org.rundeck.plugin.scm.git.GitImportPlugin
import org.rundeck.plugin.scm.git.GitUtil


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
    )
    {
        performAction(context, plugin, importer, selectedPaths,null,input)
    }

    ScmExportResult performAction(
            final ScmOperationContext context,
            final GitImportPlugin plugin,
            final JobImporter importer,
            final List<String> selectedPaths,
            final List<String> deletedJobs,
            final Map<String, String> input
    )
    {
        //perform git
        StringBuilder sb = new StringBuilder()
        boolean success = true

        //walk the repo files and look for possible candidates
        plugin.walkTreePaths('HEAD^{tree}', true) { TreeWalk walk ->
            def path = walk.getPathString()
            if (!(path in selectedPaths)) {
                plugin.log.debug("not selected, skipping path ${path}")
                return
            }
            def objectId = walk.getObjectId(0)
            def size = plugin.repo.open(objectId, Constants.OBJ_BLOB).getSize()
            plugin.log.debug("import data: ${size} = ${path}")
            def bytes = plugin.repo.open(objectId, Constants.OBJ_BLOB).getBytes(Integer.MAX_VALUE)

            def commit = GitUtil.lastCommitForPath plugin.repo, plugin.git, path
            def meta = GitUtil.metaForCommit(commit)
            meta.url = plugin.config.url

            def importResult = importer.importFromStream(
                    plugin.config.format,
                    new ByteArrayInputStream(bytes),
                    meta,
                    plugin.config.importPreserve
            )
            if (!importResult.successful) {
                success = false
                sb << ("Failed importing: ${walk.getPathString()}: " + importResult.errorMessage)
            } else {
                plugin.importTracker.trackJobAtPath(importResult.job,walk.getPathString())
                sb << ("Succeeded importing ${walk.getPathString()}: ${importResult}")
            }
        }

        deletedJobs?.each { jobId ->
            def importResult = importer.deleteJob(
                context.frameworkProject,
                jobId
            )
            if (!importResult.successful) {
                success = false
                sb << ("Failed deleting job with id: ${jobId}: " + importResult.errorMessage)
            } else {
                sb << ("Succeeded deleting job with id ${jobId} ")
            }

        }
        def result = new ScmExportResultImpl()
        result.success = success
        result.message = "Git Import " + (success ? "successful" : "failed")
        result.extendedMessage = sb.toString()
        return result

    }

}