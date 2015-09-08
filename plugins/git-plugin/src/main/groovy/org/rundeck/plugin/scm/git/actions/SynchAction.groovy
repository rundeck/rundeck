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
class SynchAction extends BaseGitAction {
    SynchAction(final String id) {
        super(id)
    }

    @Override
    BasicInputView getInputView(GitExportPlugin plugin) {
        def builder = BasicInputViewBuilder.forActionId(id).with {
            title "Synch remote changes to Git"
            buttonTitle "Synch"
        }
        def status = plugin.getStatusInternal()
        //need to fast forward
        def props = [
                PropertyBuilder.builder().with {
                    string "status"
                    title "Git Status"
                    renderingOption StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType.STATIC_TEXT
                    renderingOption StringRenderingConstants.STATIC_TEXT_CONTENT_TYPE_KEY, "text/x-markdown"
                    defaultValue status.message + """

Pulling from remote branch: `${plugin.branch}`"""
                    build()
                },
        ]
        if (status.branchTrackingStatus?.behindCount > 0 && status.branchTrackingStatus?.aheadCount > 0) {
            props.addAll([
                    PropertyBuilder.builder().with {
                        select "refresh"
                        title "Refresh Strategy"
                        description """Method to refresh local repo from remote

* `pull` - pull changes
* `rebase` - rebase local changes on top of remote
* `merge` - merge remote changes into local changes"""
                        values "pull", "rebase", "merge"
                        defaultValue "pull"
                        required true
                        build()
                    },
                    PropertyBuilder.builder().with {
                        select "resolution"
                        title "Conflict Resolution Strategy"
                        description """Method to resolve conflicts from remote

* `ours` - apply our changes over theirs
* `theirs` - apply their changes over ours"""
                        values "ours", "theirs"
                        defaultValue "ours"
                        required true
                        build()
                    },
            ]
            )

        } else if (status.branchTrackingStatus?.behindCount > 0) {

            props << PropertyBuilder.builder().with {
                booleanType "pull"
                title "Pull from Remote"
                description "Check to pull changes from the remote"
                defaultValue "true"
                required true
                build()
            }
        }

        builder.properties(props).build()
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
        //todo
        throw new ScmPluginException("todo")

    }
}
