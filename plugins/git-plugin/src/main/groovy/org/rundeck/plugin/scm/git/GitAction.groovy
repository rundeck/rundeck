package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo

/**
 * Created by greg on 9/8/15.
 */
interface GitAction extends Action {

    BasicInputView getInputView(final GitExportPlugin plugin)

    ScmExportResult perform(
            final GitExportPlugin plugin,
            final Set<JobExportReference> jobs,
            final Set<String> pathsToDelete,
            final ScmUserInfo userInfo,
            final Map<String, Object> input
    ) throws ScmPluginException
}