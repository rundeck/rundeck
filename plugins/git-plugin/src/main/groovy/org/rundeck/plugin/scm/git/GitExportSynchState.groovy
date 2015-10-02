package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.ScmExportSynchState
import com.dtolabs.rundeck.plugins.scm.SynchState
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.BranchTrackingStatus

/**
 *
 */
class GitExportSynchState implements ScmExportSynchState {
    SynchState state
    String message
    Status gitStatus
    BranchTrackingStatus branchTrackingStatus
}
