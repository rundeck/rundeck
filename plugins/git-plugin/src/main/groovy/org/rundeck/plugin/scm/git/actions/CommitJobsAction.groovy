package org.rundeck.plugin.scm.git.actions

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.core.plugins.views.BasicInputViewBuilder
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportResultImpl
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import com.dtolabs.rundeck.plugins.scm.ScmUserInfoMissing
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.eclipse.jgit.api.AddCommand
import org.eclipse.jgit.api.CommitCommand
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.revwalk.RevCommit
import org.rundeck.plugin.scm.git.BaseGitAction
import org.rundeck.plugin.scm.git.GitExportPlugin

/**
 * Created by greg on 9/8/15.
 */
class CommitJobsAction extends BaseGitAction {
    CommitJobsAction(final String id, final String title, final String description) {
        super(id, title, description)
    }

    BasicInputView getInputView(GitExportPlugin plugin) {
        BasicInputViewBuilder.forActionId(id).with {
            title "Commit Changes to Git"
            buttonTitle "Commit"
            properties([
                    PropertyBuilder.builder().with {
                        string "commitMessage"
                        title "Commit Message"
                        description "Enter a commit message. Committing to branch: `" + plugin.branch + '`'
                        required true
                        renderingAsTextarea()
                        build()
                    },

                    PropertyBuilder.builder().with {
                        string "tagName"
                        title "Tag"
                        description "Enter a tag name to include, will be pushed with the branch."
                        required false
                        build()
                    },

                    PropertyBuilder.builder().with {
                        booleanType "push"
                        title "Push Remotely?"
                        description "Check to push to the remote"
                        required false
                        build()
                    },
            ]
            )
            build()
        }
    }

    @Override
    ScmExportResult perform(
            final GitExportPlugin plugin,
            final Set<JobExportReference> jobs,
            final Set<String> pathsToDelete,
            final ScmUserInfo userInfo,
            final Map<String, Object> input
    ) throws ScmPluginException
    {
        //determine action
        def internal = plugin.getStatusInternal()
        def localGitChanges = !internal.gitStatus.isClean()

        RevCommit commit
        def result = new ScmExportResultImpl()

        if (localGitChanges) {
            if (!jobs && !pathsToDelete) {
                throw new ScmPluginException("No jobs were selected")
            }
            if (!input.commitMessage) {
                throw new ScmPluginException("A commitMessage is required")
            }
            def commitIdentName = plugin.expand(plugin.committerName, userInfo)
            if (!commitIdentName) {
                ScmUserInfoMissing.fieldMissing("committerName")
            }
            def commitIdentEmail = plugin.expand(plugin.committerEmail, userInfo)
            if (!commitIdentEmail) {
                ScmUserInfoMissing.fieldMissing("committerEmail")
            }
            plugin.serializeAll(jobs)
            String commitMessage = input.commitMessage.toString()
            Status status = plugin.git.status().call()
            //add all changes to index
            if (jobs) {
                AddCommand addCommand = plugin.git.add()
                jobs.each {
                    addCommand.addFilepattern(plugin.relativePath(it))
                }
                addCommand.call()
            }
            def rmfiles = new HashSet<String>(status.removed + status.missing)
            def todelete = pathsToDelete.intersect(rmfiles)
            if (todelete) {
                def rm = plugin.git.rm()
                todelete.each {
                    rm.addFilepattern(it)
                }
                rm.call()
            }

            CommitCommand commit1 = plugin.git.commit().
                    setMessage(commitMessage).
                    setCommitter(commitIdentName, commitIdentEmail)
            jobs.each {
                commit1.setOnly(plugin.relativePath(it))
            }
            pathsToDelete.each {
                commit1.setOnly(it)
            }
            commit = commit1.call()
            result.success = true

        } else {
            //no git changes, but some jobs were selected
            throw new ScmPluginException("No changes to local git repo need to be exported")
        }
        result.id = commit?.name


        result
    }

}
