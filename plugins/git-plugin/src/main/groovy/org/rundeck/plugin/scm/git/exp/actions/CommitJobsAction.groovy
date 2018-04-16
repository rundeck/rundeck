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

import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.*
import org.eclipse.jgit.api.AddCommand
import org.eclipse.jgit.api.CommitCommand
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.revwalk.RevCommit
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.BuilderUtil
import org.rundeck.plugin.scm.git.GitExportAction
import org.rundeck.plugin.scm.git.GitExportPlugin
import org.rundeck.plugin.scm.git.GitScmCommit
import org.rundeck.plugin.scm.git.GitUtil


/**
 * Created by greg on 9/8/15.
 */
class CommitJobsAction extends BaseAction implements GitExportAction {

    public static final String P_MESSAGE = 'message'
    public static final String P_PUSH = 'push'

    CommitJobsAction(final String id, final String title, final String description) {
        super(id, title, description)
    }

    BasicInputView getInputView(final ScmOperationContext context, GitExportPlugin plugin) {
        BuilderUtil.inputViewBuilder(id) {
            title getTitle()
            description getDescription()
            buttonTitle "Commit"
            properties([
                    BuilderUtil.property {
                        string P_MESSAGE
                        title "Commit Message"
                        description "Enter a commit message. Committing to branch: `" + plugin.branch + '`'
                        required true
                        renderingAsTextarea()
                    },

                    BuilderUtil.property {
                        string TagAction.P_TAG_NAME
                        title "Tag"
                        description "Enter a tag name to include, will be pushed with the branch."
                        required false
                    },

                    BuilderUtil.property {
                        booleanType P_PUSH
                        title "Push Remotely?"
                        description "Check to push to the remote"
                        required false
                        defaultValue'true'
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
        //determine action
        def internal = plugin.getStatusInternal(context, false)
        def localGitChanges = !internal.gitStatus.isClean()

        RevCommit commit
        def result = new ScmExportResultImpl()

        if (!localGitChanges) {
            //no git changes, but some jobs were selected
            throw new ScmPluginException("No changes to local git repo need to be exported")
        }
        if (input[TagAction.P_TAG_NAME]) {
            TagAction.validateTagDoesNotExist(plugin, input[TagAction.P_TAG_NAME])
            TagAction.validateTagName(plugin, input[TagAction.P_TAG_NAME])
        }

        if (!jobs && !pathsToDelete) {
            throw new ScmPluginException("No jobs were selected")
        }
        if (!input[P_MESSAGE]) {
            throw new ScmPluginException("A ${P_MESSAGE} is required")
        }
        def commitIdentName = plugin.expand(plugin.committerName, context.userInfo)
        if (!commitIdentName) {
            ScmUserInfoMissing.fieldMissing("committerName")
        }
        def commitIdentEmail = plugin.expand(plugin.committerEmail, context.userInfo)
        if (!commitIdentEmail) {
            ScmUserInfoMissing.fieldMissing("committerEmail")
        }

        plugin.serializeAll(jobs, plugin.format, plugin.config.exportPreserve, plugin.config.exportOriginal)
        String commitMessage = input[P_MESSAGE].toString()
        Status status = plugin.git.status().call()
        int pathcount=0
        //add all changes to index
        if (jobs) {
            AddCommand addCommand = plugin.git.add()
            jobs.each {
                addCommand.addFilepattern(plugin.relativePath(it))
            }
            addCommand.call()
            pathcount+=jobs.size()
        }
        def rmfiles = new HashSet<String>(status.removed + status.missing)
        def todelete = pathsToDelete.intersect(rmfiles)
        if (todelete) {
            def rm = plugin.git.rm()
            todelete.each {
                rm.addFilepattern(it)
            }
            rm.call()
            pathcount+=todelete.size()
        }

        if (!pathcount) {
            result.success = true
            result.message = 'No git changes needed'
            return result
        }
        CommitCommand commit1 = plugin.git.commit().
                setMessage(commitMessage).
                setCommitter(commitIdentName, commitIdentEmail)
        jobs.each {
            commit1.setOnly(plugin.relativePath(it))
        }
        todelete.each {
            commit1.setOnly(it)
        }
        commit = commit1.call()
        result.success = true
        result.commit=new GitScmCommit(GitUtil.metaForCommit(commit))

        if (result.success && input[TagAction.P_TAG_NAME]) {
            def tagResult = plugin.export(context, GitExportPlugin.PROJECT_TAG_ACTION_ID, jobs, pathsToDelete, input)
            if (!tagResult.success) {
                return tagResult
            }
        }
        if (result.success && input[P_PUSH] == 'true') {
            return plugin.export(context, GitExportPlugin.PROJECT_PUSH_ACTION_ID, jobs, pathsToDelete, input)
        }
        result.id = commit?.name


        result
    }

}
