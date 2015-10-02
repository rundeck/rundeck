package org.rundeck.plugin.scm.git.exp.actions

import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.*
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.GitExportAction
import org.rundeck.plugin.scm.git.GitExportPlugin

import static org.rundeck.plugin.scm.git.BuilderUtil.inputView
import static org.rundeck.plugin.scm.git.BuilderUtil.property

/**
 * Created by greg on 9/8/15.
 */
class FetchAction extends BaseAction implements GitExportAction {
    FetchAction(final String id, final String title, final String description) {
        super(id, title, description)
    }

    @Override
    BasicInputView getInputView(final ScmOperationContext context, final GitExportPlugin plugin) {
        inputView(id) {
            title "Fetch remote changes"
            buttonTitle "Fetch"
            properties([
                    property {
                        string "status"
                        title "Git Status"
                        renderingOption StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType.STATIC_TEXT
                        renderingOption StringRenderingConstants.STATIC_TEXT_CONTENT_TYPE_KEY, "text/x-markdown"
                        defaultValue "Fetching from remote branch: `${plugin.branch}`"
                    }
            ]
            )
        }
    }

    @Override
    ScmExportResult perform(
            final GitExportPlugin plugin,
            final Set<JobExportReference> jobs,
            final Set<String> pathsToDelete,
            final ScmOperationContext context,
            final Map<String, String> input
    ) throws ScmPluginException
    {

        //fetch remote changes
        def update = plugin.fetchFromRemote(context)

        def result = new ScmExportResultImpl()
        result.success = true
        result.message = update ? update.toString() : "No changes were found"
        result
    }
}
