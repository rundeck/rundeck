package org.rundeck.plugin.scm.git.exp.actions

import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.core.plugins.views.BasicInputViewBuilder
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportResultImpl
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.eclipse.jgit.merge.MergeStrategy
import org.rundeck.plugin.scm.git.BaseAction
import org.rundeck.plugin.scm.git.GitExportAction
import org.rundeck.plugin.scm.git.GitExportPlugin

/**
 * Created by greg on 9/8/15.
 */
class SynchAction extends BaseAction implements GitExportAction {
    SynchAction(final String id, final String title, final String description) {
        super(id, title, description)
    }

    @Override
    BasicInputView getInputView(GitExportPlugin plugin) {
        def builder = BasicInputViewBuilder.forActionId(id).with {
            title "Synch remote changes to Git"
            buttonTitle "Synch"
        }
        def status = plugin.getStatusInternal()
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
                        title "Synch Method"
                        description """Choose a method to synch the remote branch changes with local git repository.

* `merge` - merge remote changes into local changes
* `rebase` - rebase local changes on top of remote
"""
                        values "merge", "rebase"
                        defaultValue "merge"
                        required true
                        build()
                    },
                    PropertyBuilder.builder().with {
                        select "resolution"
                        title "Conflict Resolution Strategy"
                        description """Choose a strategy to resolve conflicts in the synched files.

* `ours` - apply our changes over theirs
* `theirs` - apply their changes over ours"""
                        values(MergeStrategy.get()*.name)
                        defaultValue "ours"
                        required true
                        build()
                    },
            ]
            )

        } else if (status.branchTrackingStatus?.behindCount > 0) {

            //just need to fast forward, so Pull is the right action
            builder.buttonTitle("Pull Changes")

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
        def status = plugin.getStatusInternal()


        if (status.branchTrackingStatus?.behindCount > 0 && status.branchTrackingStatus?.aheadCount > 0) {
            gitResolve(plugin, input)
        } else if (status.branchTrackingStatus?.behindCount > 0) {
            gitPull(plugin)
        } else {
            //no action
        }

    }

    ScmExportResult gitPull(final GitExportPlugin plugin) {
        def pullResult = plugin.git.pull().setRemote('origin').setRemoteBranchName(plugin.branch).call()

        def result = new ScmExportResultImpl()
        result.success = pullResult.successful
        result.message = pullResult.toString()
        result
    }

    ScmExportResult gitResolve(final GitExportPlugin plugin, final Map<String, Object> input) {


        if (input.refresh == 'rebase') {
            def pullbuilder = plugin.git.pull().setRemote('origin').setRemoteBranchName(plugin.branch)
            pullbuilder.setRebase(true)
            def pullResult = pullbuilder.call()

            def result = new ScmExportResultImpl()
            result.success = pullResult.successful
            result.message = pullResult.toString()
            return result
        } else {
            //fetch, then
            //merge

            def fetchResult = plugin.git.fetch().setRemote('origin').call()
            def update = fetchResult.getTrackingRefUpdate("refs/remotes/origin/master")


            def strategy = MergeStrategy.get(input.resolution)
            def mergebuild = plugin.git.merge().setStrategy(strategy)
            def commit = plugin.git.repository.resolve("refs/remotes/origin/master")

            mergebuild.include(commit)

            def mergeresult = mergebuild.call()

            def result = new ScmExportResultImpl()
            result.success = mergeresult.mergeStatus.successful
            result.message = mergeresult.toString()
            return result

        }
    }
}
