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

import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.JobImporter
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportResultImpl
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmPluginInvalidInput
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.BuilderUtil
import org.rundeck.plugin.scm.git.GitImportAction
import org.rundeck.plugin.scm.git.GitImportPlugin

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException


/**
 * Created by greg on 9/10/15.
 */
class SetupTracking extends BaseAction implements GitImportAction {

    public static final String USE_FILE_PATTERN = "useFilePattern"
    public static final String FILE_PATTERN = "filePattern"

    SetupTracking(final String id, final String title, final String description, final String iconName) {
        super(id, title, description, iconName)
    }


    BasicInputView getInputView(final ScmOperationContext context, GitImportPlugin plugin) {
        BuilderUtil.inputViewBuilder(id) {
            title "Setup Tracking"
            description '''Enter a Regular expression to match potential new repo files that are added.

Or, you can also choose to select a static list of Files found in the Repository to be tracked for Job Import.

Note: If you select Files and do not choose to match via regular expression,
then new files added to the repo *will not* be available for Job Import, and only those selected
files will be watched for changes.'''
            buttonTitle "Setup"
            properties([
                    BuilderUtil.property {
                        booleanType USE_FILE_PATTERN
                        title "Match a Regular Expression?"
                        description "Check to match all paths that match the regular expression."
                        required false
                        defaultValue 'true'
                        build()
                    },
                    BuilderUtil.property {
                        freeSelect FILE_PATTERN
                        title "Regular Expression"
                        description "Enter a regular expression. New paths in the repo matching this expression will also be imported."
                        required false
                        values '.*\\.xml', '.*\\.yaml'
                        defaultValue '.*\\.xml'
                        validator({ String pat ->
                            try {
                                Pattern.compile(pat)
                                return true
                            } catch (PatternSyntaxException e) {
                                throw new ValidationException("Invalid regular expression: " + e.message)
                            }
                                  } as PropertyValidator
                        )
                    },
            ]
            )
        }
    }

    /**
     * Add config values for inputs
     * @param plugin
     * @param selectedPaths
     * @param input
     */
    static void setupWithInput(
            final GitImportPlugin plugin,
            final List<String> selectedPaths,
            final Map<String, String> input
    )
    {
        if (input[USE_FILE_PATTERN] != null && selectedPaths != null) {
            GitImportPlugin.log.debug("SetupTracking: ${selectedPaths}, ${input} (true)")
            plugin.trackedItems = selectedPaths
            plugin.useTrackingRegex = 'true' == input[USE_FILE_PATTERN]
            plugin.trackingRegex = plugin.useTrackingRegex ? input[FILE_PATTERN] : null
            plugin.trackedItemsSelected = true
        } else {
            GitImportPlugin.log.debug("SetupTracking: ${selectedPaths}, ${input} (false)")
            plugin.trackedItemsSelected = false
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
        if (input[USE_FILE_PATTERN] != 'true' && !selectedPaths) {
            throw new ScmPluginInvalidInput(
                    Validator.errorReport(
                            USE_FILE_PATTERN,
                            "If no static paths are selected, then you must enter a regular expression."
                    )
            )
        }
        if (input[USE_FILE_PATTERN] == 'true' && !input[FILE_PATTERN]) {
            throw new ScmPluginInvalidInput(
                    Validator.errorReport(
                            FILE_PATTERN,
                            "If no static paths are selected, then you must enter a regular expression."
                    )
            )
        }
        setupWithInput(plugin, selectedPaths, input)

        def result = new ScmExportResultImpl()
        result.success = true
        result.message = "Setup successful"

        result
    }

}
