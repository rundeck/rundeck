package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.ImportSynchState
import com.dtolabs.rundeck.plugins.scm.ScmImportSynchState
import org.eclipse.jgit.lib.BranchTrackingStatus

/**
 * Created by greg on 9/15/15.
 */
class GitImportSynchState implements ScmImportSynchState {
    int importNeeded
    int notFound
    int deleted
    BranchTrackingStatus branchTrackingStatus
    ImportSynchState state
    String message
}
