package org.rundeck.plugin.scm.git.imp.actions

import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.core.plugins.views.BasicInputViewBuilder
import com.dtolabs.rundeck.plugins.scm.*
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.GitExportAction
import org.rundeck.plugin.scm.git.GitExportPlugin
import org.rundeck.plugin.scm.git.GitImportAction
import org.rundeck.plugin.scm.git.GitImportPlugin

/**
 * Created by greg on 9/8/15.
 */
class FetchAction extends BaseAction implements GitImportAction {
    FetchAction(final String id, final String title, final String description) {
        super(id, title, description)
    }

    @Override
    BasicInputView getInputView(final GitImportPlugin plugin) {
        def builder = BasicInputViewBuilder.forActionId(id).with {
            title "Fetch remote changes"
            buttonTitle "Fetch"
        }
        //need to fast forward
        def props = [
                PropertyBuilder.builder().with {
                    string "status"
                    title "Git Status"
                    renderingOption StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType.STATIC_TEXT
                    renderingOption StringRenderingConstants.STATIC_TEXT_CONTENT_TYPE_KEY, "text/x-markdown"
                    defaultValue "Fetching from remote branch: `${plugin.branch}`"
                    build()
                },
        ]

        builder.properties(props).build()
    }

    @Override
    ScmExportResult performAction(
            final GitImportPlugin plugin,
            final JobImporter importer,
            final List<String> selectedPaths,
            final Map<String, Object> input
    )
    {
        //fetch remote changes
        def fetchResult = plugin.git.fetch().call()

        def update = fetchResult.getTrackingRefUpdate("refs/remotes/origin/master")

        def result = new ScmExportResultImpl()
        result.success = true
        result.message = update ? update.toString() : "No changes were found"
        result
    }
}
