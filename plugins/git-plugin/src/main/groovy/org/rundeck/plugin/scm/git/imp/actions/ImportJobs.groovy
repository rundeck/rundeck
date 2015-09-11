package org.rundeck.plugin.scm.git.imp.actions

import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.core.plugins.views.BasicInputViewBuilder
import com.dtolabs.rundeck.plugins.scm.JobImporter
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportResultImpl
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.treewalk.TreeWalk
import org.rundeck.plugin.scm.git.BaseAction
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


    BasicInputView getInputView(GitImportPlugin plugin) {
        BasicInputViewBuilder.forActionId(id).with {
            title "Import remote Changes"
            description '''Import the modifications to Rundeck'''
            buttonTitle "Import"
            properties([])
            build()
        }
    }

    @Override
    ScmExportResult performAction(
            final GitImportPlugin plugin,
            final JobImporter importer,
            final List<String> selectedPaths,
            final Map<String, Object> input
    )
    {
        //perform git
        StringBuilder sb = new StringBuilder()
        boolean success=true

        //get head commit info
        def revCommit = GitUtil.getHead(plugin.repo)
        def meta = GitUtil.metaForCommit(revCommit)

        //walk the repo files and look for possible candidates
        plugin.walkTreePaths('HEAD^{tree}') { TreeWalk walk ->
            def objectId = walk.getObjectId(0)

            def size = plugin.repo.open(objectId, Constants.OBJ_BLOB).getSize()
            plugin.log.debug("import data: ${size} = ${objectId.name}")
            def bytes = plugin.repo.open(objectId, Constants.OBJ_BLOB).getBytes(Integer.MAX_VALUE)

            plugin.log.debug("import data: "+new String(bytes))

            def importResult = importer.importFromStream(
                    walk.getNameString().endsWith(".xml") ? 'xml' : 'yaml',
                    new ByteArrayInputStream(bytes),
                    meta
            )
            if(!importResult.successful){
                success=false
                sb << ("Failed importing: ${walk.getPathString()}: " + importResult.errorMessage)
            }else{

                sb << ("Succeeded importing ${walk.getPathString()}: ${importResult}")
            }
        }
        def result = new ScmExportResultImpl()
        result.success = success
        result.message = sb.toString()
        return result

    }

}