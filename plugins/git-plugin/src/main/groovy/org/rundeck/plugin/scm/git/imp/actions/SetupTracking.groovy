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

    SetupTracking(final String id, final String title, final String description, final String iconName) {
        super(id, title, description, iconName)
    }


    BasicInputView getInputView(final ScmOperationContext context, GitImportPlugin plugin) {
        BuilderUtil.inputViewBuilder(id) {
            title "Setup Tracking"
            description '''Choose to select a static list of Files found in the Repository to be tracked for Job Import.

Note: New files added to the repo *will not* be available for Job Import, and only those selected
files will be watched for changes.'''
            buttonTitle "Setup"
            properties([
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
        if (selectedPaths != null) {
            GitImportPlugin.log.debug("SetupTracking: ${selectedPaths}, ${input} (true)")
            plugin.trackedItems = selectedPaths
        } else {
            GitImportPlugin.log.debug("SetupTracking: ${selectedPaths}, ${input} (false)")
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

        setupWithInput(plugin, selectedPaths, input)

        def result = new ScmExportResultImpl()
        result.success = true
        result.message = "Setup successful"

        result
    }

}
