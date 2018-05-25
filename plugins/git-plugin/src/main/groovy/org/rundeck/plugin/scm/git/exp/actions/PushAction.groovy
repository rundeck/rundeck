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

import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.*
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.BaseGitPlugin
import org.rundeck.plugin.scm.git.BuilderUtil
import org.rundeck.plugin.scm.git.GitExportAction
import org.rundeck.plugin.scm.git.GitExportPlugin
import org.rundeck.plugin.scm.git.GitScmCommit
import org.rundeck.plugin.scm.git.GitUtil


/**
 * Created by greg on 9/8/15.
 */
class PushAction extends BaseAction implements GitExportAction {

    PushAction(final String id, final String title, final String description) {
        super(id, title, description)
    }

    @Override
    BasicInputView getInputView(final ScmOperationContext context, GitExportPlugin plugin) {
        def status = plugin.getStatusInternal(context, false)
        BuilderUtil.inputViewBuilder(id) {
            title "Push remote Git changes"
            buttonTitle "Push"
            properties([
                    BuilderUtil.property {
                        string "status"
                        title "Export Status"
                        renderingOption StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType.STATIC_TEXT
                        renderingOption StringRenderingConstants.STATIC_TEXT_CONTENT_TYPE_KEY, "text/x-markdown"
                        defaultValue status.message + """

Pushing to remote branch: `${plugin.branch}`"""
                    },
                    BuilderUtil.property {
                        string TagAction.P_TAG_NAME
                        title "Tag"
                        description "Enter a tag name to include, will be pushed with the branch. The tag will be created if it does not exist."
                        required false
                    },
                    BuilderUtil.property {
                        string TagAction.P_MESSAGE
                        title "Tag Message"
                        description "Enter a message for the annotated Tag."
                        required false
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
        def result = new ScmExportResultImpl()

        Ref tagref
        if (input[TagAction.P_TAG_NAME]) {
            TagAction.validateTagName(plugin, input[TagAction.P_TAG_NAME])
            tagref = GitUtil.findTag(input[TagAction.P_TAG_NAME], plugin.git)
            if (!tagref) {
                def tagResult = plugin.export(
                        context,
                        GitExportPlugin.PROJECT_TAG_ACTION_ID,
                        jobs,
                        pathsToDelete,
                        input
                )
                if (!tagResult.success) {
                    return tagResult
                }
                tagref = GitUtil.findTag(input[TagAction.P_TAG_NAME], plugin.git)
            }
        }

        def commit = plugin.getHead()

        def pushb = plugin.git.push()
        pushb.setRemote(BaseGitPlugin.REMOTE_NAME)
        pushb.add(plugin.branch)
        plugin.setupTransportAuthentication(plugin.sshConfig, context, pushb)

        if (tagref) {
            pushb.add(tagref)
        }

        def push
        try {
            push = pushb.call()
        } catch (Exception e) {
            plugin.logger.debug("Failed push to remote: ${e.message}", e)
            throw new ScmPluginException("Failed push to remote: ${e.message}", e)
        }
        def sb = new StringBuilder()
        def updates = (push*.remoteUpdates).flatten()
        updates.each {
            sb.append it.toString()
        }
        def failedUpdates = updates.findAll { it.status != RemoteRefUpdate.Status.OK }
        result.success = !failedUpdates
        if (failedUpdates) {
            result.message = "Some updates failed: " + failedUpdates
        } else {
            result.message = "Remote push result: OK. (Commit: ${commit.name})"
        }
        result.id = commit?.name
        result.commit=new GitScmCommit(GitUtil.metaForCommit(commit))
        return result
    }

}
