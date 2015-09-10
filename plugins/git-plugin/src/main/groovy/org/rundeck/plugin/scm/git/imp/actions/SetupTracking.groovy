package org.rundeck.plugin.scm.git.imp.actions

import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.core.plugins.views.BasicInputViewBuilder
import com.dtolabs.rundeck.plugins.scm.JobImporter
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportResultImpl
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.GitImportAction
import org.rundeck.plugin.scm.git.GitImportPlugin

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Created by greg on 9/10/15.
 */
class SetupTracking extends BaseAction implements GitImportAction {
    SetupTracking(final String id, final String title, final String description, final String iconName) {
        super(id, title, description, iconName)
    }


    BasicInputView getInputView(GitImportPlugin plugin) {
        BasicInputViewBuilder.forActionId(id).with {
            title "Setup Tracking"
            description '''Select the Files found in the Repository to be used for Job Import.

You can also choose to enter a Regular expression to match new repo files that are added.'''
            buttonTitle "Setup"
            properties([
                    PropertyBuilder.builder().with {
                        booleanType "useFilePattern"
                        title "Match a Regular Expression?"
                        description "Check to match all paths that match the regular expression."
                        required false
                        build()
                    },
                    PropertyBuilder.builder().with {
                        string "filePattern"
                        title "Regular Expression"
                        description "Enter a regular expression. New paths in the repo matching this expression will also be imported."
                        required false
                        validator({ String pat ->
                            try {
                                Pattern.compile(pat)
                                return true
                            } catch (PatternSyntaxException e) {
                                throw new ValidationException("Invalid regular expression: " + e.message)
                            }
                                  } as PropertyValidator
                        )
                        build()
                    },
            ]
            )
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
        GitImportPlugin.log.debug("SetupTracking: ${selectedPaths}, ${input}")
        plugin.trackedItems = selectedPaths
        plugin.useTrackingRegex = 'true' == input.useFilePattern
        plugin.trackingRegex = plugin.useTrackingRegex ? input.filePattern : null
        plugin.trackedItemsSelected = true

        def result = new ScmExportResultImpl()
        result.success = true
        result.message = "Setup successful"

        result
    }

}
