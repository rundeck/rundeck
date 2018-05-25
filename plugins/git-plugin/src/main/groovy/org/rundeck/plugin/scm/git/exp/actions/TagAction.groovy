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

package org.rundeck.plugin.scm.git.exp.actions

import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportResultImpl
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmPluginInvalidInput
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.BuilderUtil
import org.rundeck.plugin.scm.git.GitExportAction
import org.rundeck.plugin.scm.git.GitExportPlugin
import org.rundeck.plugin.scm.git.GitUtil


/**
 * Create a tag
 */
class TagAction extends BaseAction implements GitExportAction {

    public static final String P_MESSAGE = 'message'
    public static final String P_TAG_NAME = 'tagName'

    TagAction(final String id, final String title, final String description) {
        super(id, title, description)
    }

    BasicInputView getInputView(final ScmOperationContext context, GitExportPlugin plugin) {
        BuilderUtil.inputViewBuilder(id) {
            title "Commit Changes to Git"
            buttonTitle "Commit"
            properties([
                    BuilderUtil.property {
                        string P_MESSAGE
                        title "Message"
                        description "Enter a message for the Tag."
                        required true
                        renderingAsTextarea()
                    },

                    BuilderUtil.property {
                        string P_TAG_NAME
                        title "Tag"
                        description "Enter a tag name to include, will be pushed with the branch."
                        required true
                    },

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
        if (!input[P_TAG_NAME]) {
            throw new ScmPluginInvalidInput(
                    Validator.errorReport(P_TAG_NAME, "Tag name is required.")
            )
        }
        if (!input[P_MESSAGE]) {
            throw new ScmPluginInvalidInput(
                    Validator.errorReport(P_MESSAGE, "Message required for a new tag.")
            )
        }
        validateTagDoesNotExist( plugin, input[P_TAG_NAME])
        validateTagName( plugin, input[P_TAG_NAME])
        def commit = plugin.getHead()
        Ref tagref

        try {
            tagref = GitUtil.createTag(plugin.git, input[P_TAG_NAME], input[P_MESSAGE], commit)
        } catch (Exception e) {
            plugin.logger.debug("Failed create tag: ${e.message}", e)
            throw new ScmPluginException("Failed create tag: ${e.message}", e)
        }
        def result = new ScmExportResultImpl()
        result.success = true
        result.message = "Created tag: ${tagref.name}"
        return result
    }

    static void validateTagDoesNotExist( GitExportPlugin plugin, String tag) {
        def found = GitUtil.findTag(tag, plugin.git)
        if (found) {
            throw new ScmPluginInvalidInput(
                    Validator.errorReport(P_TAG_NAME, "Tag already exists: ${tag}")
            )
        }
    }
    static void validateTagName( GitExportPlugin plugin, String tag) {
        def valid = Repository.isValidRefName("refs/tags/" + tag)
        if (!valid) {
            throw new ScmPluginInvalidInput(
                    Validator.errorReport(P_TAG_NAME, "Tag name is not valid: ${tag}")
            )
        }
    }
}
