package org.rundeck.plugin.scm.git.imp.actions

import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.JobImporter
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportResultImpl
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.treewalk.TreeWalk
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.GitImportAction
import org.rundeck.plugin.scm.git.GitImportPlugin
import org.rundeck.plugin.scm.git.GitUtil

import static org.rundeck.plugin.scm.git.BuilderUtil.inputView

/**
 * Action to import selected jobs from git HEAD commit
 */
class ImportJobs extends BaseAction implements GitImportAction {
    ImportJobs(final String id, final String title, final String description, final String iconName) {
        super(id, title, description, iconName)
    }


    BasicInputView getInputView(final ScmOperationContext context, GitImportPlugin plugin) {
        inputView(id) {
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

            def importResult = importer.importFromStream(
                    plugin.config.format,
                    new ByteArrayInputStream(bytes),
                    meta
            )
            if (!importResult.successful) {
                success = false
                sb << ("Failed importing: ${walk.getPathString()}: " + importResult.errorMessage)
            } else {
                plugin.importTracker.trackJobAtPath(importResult.job,walk.getPathString())
                sb << ("Succeeded importing ${walk.getPathString()}: ${importResult}")
            }
        }
        def result = new ScmExportResultImpl()
        result.success = success
        result.message = "Git Import " + (success ? "successful" : "failed")
        result.extendedMessage = sb.toString()
        return result

    }

}