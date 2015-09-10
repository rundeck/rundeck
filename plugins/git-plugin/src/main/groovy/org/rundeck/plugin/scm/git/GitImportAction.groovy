package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.JobImporter
import com.dtolabs.rundeck.plugins.scm.ScmExportResult

/**
 * Created by greg on 9/10/15.
 */
interface GitImportAction extends Action{
    BasicInputView getInputView(final GitImportPlugin plugin)

    ScmExportResult performAction(
            final GitImportPlugin plugin,
            final JobImporter importer,
            final List<String> selectedPaths,
            final Map<String, Object> input
    )

}
