package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.JobState
import com.dtolabs.rundeck.plugins.scm.ScmDiffResult
import com.dtolabs.rundeck.plugins.scm.ScmExportSynchState
import com.dtolabs.rundeck.plugins.scm.ScmImportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmPluginException


/**
 * Import jobs via git
 */
class GitImportPlugin implements ScmImportPlugin {
    @Override
    void scmImport(
            final String actionId,
            final Object importer,
            final List<String> selectedPaths,
            final Map<String, Object> input
    ) throws ScmPluginException
    {

    }

    @Override
    ScmExportSynchState getStatus() {
        return null
    }

    @Override
    JobState getJobStatus(final JobExportReference job) {
        return null
    }

    @Override
    JobState getJobStatus(final JobExportReference job, final String originalPath) {
        return null
    }

    @Override
    void initialize() {

    }

    @Override
    void cleanup() {

    }

    @Override
    BasicInputView getInputViewForAction(final String actionId) {
        return null
    }

    @Override
    List<Action> actionsAvailableForContext(final Map<String, String> context) {
        return null
    }

    @Override
    String getRelativePathForJob(final JobReference job) {
        return null
    }

    @Override
    ScmDiffResult getFileDiff(final JobExportReference job) {
        return null
    }

    @Override
    ScmDiffResult getFileDiff(final JobExportReference job, final String originalPath) {
        return null
    }
}
