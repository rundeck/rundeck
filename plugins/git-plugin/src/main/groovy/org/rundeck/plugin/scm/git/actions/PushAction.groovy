package org.rundeck.plugin.scm.git.actions

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.core.plugins.views.BasicInputViewBuilder
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportResultImpl
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.rundeck.plugin.scm.git.BaseGitAction
import org.rundeck.plugin.scm.git.GitExportPlugin

/**
 * Created by greg on 9/8/15.
 */
class PushAction extends BaseGitAction {
    PushAction(final String id) {
        super(id)
    }

    @Override
    BasicInputView getInputView(GitExportPlugin plugin) {
        BasicInputViewBuilder.forActionId(id).with {
            title "Push remote Git changes"
            buttonTitle "Push"
            properties([
                    PropertyBuilder.builder().with {
                        string "status"
                        title "Export Status"
                        renderingOption StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType.STATIC_TEXT
                        renderingOption StringRenderingConstants.STATIC_TEXT_CONTENT_TYPE_KEY, "text/x-markdown"
                        defaultValue status.message + """

Pushing to remote branch: `${plugin.branch}`"""
                        build()
                    },

                    PropertyBuilder.builder().with {
                        booleanType "push"
                        title "Push Remotely?"
                        description "Check to push to the remote"
                        defaultValue "true"
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
        def result = new ScmExportResultImpl()

        //todo: tag
        if (input.tag) {
            System.err.println("TODO: tag ${input.tag}")
        }

        def commit = plugin.getHead()
        def pushb = plugin.git.push()
        pushb.setRemote("origin")
        pushb.add(plugin.branch)

//                pushb.add(input.tag) //todo: push tag

        def push = pushb.call()
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
            result.message = "Remote push result: OK"
        }
        result.id = commit?.name
        return result
    }

}
