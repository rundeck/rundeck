package org.rundeck.plugin.scm.git.exp.actions
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.*
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.GitExportAction
import org.rundeck.plugin.scm.git.GitExportPlugin

import static org.rundeck.plugin.scm.git.BuilderUtil.inputView
import static org.rundeck.plugin.scm.git.BuilderUtil.property
/**
 * Created by greg on 9/8/15.
 */
class PushAction extends BaseAction implements GitExportAction {
    PushAction(final String id, final String title, final String description) {
        super(id, title, description)
    }

    @Override
    BasicInputView getInputView(GitExportPlugin plugin) {
        def status = plugin.getStatusInternal()
        inputView(id) {
            title "Push remote Git changes"
            buttonTitle "Push"
            properties([
                    property {
                        string "status"
                        title "Export Status"
                        renderingOption StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType.STATIC_TEXT
                        renderingOption StringRenderingConstants.STATIC_TEXT_CONTENT_TYPE_KEY, "text/x-markdown"
                        defaultValue status.message + """

Pushing to remote branch: `${plugin.branch}`"""
                    },

                    property {
                        booleanType "push"
                        title "Push Remotely?"
                        description "Check to push to the remote"
                        defaultValue "true"
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
