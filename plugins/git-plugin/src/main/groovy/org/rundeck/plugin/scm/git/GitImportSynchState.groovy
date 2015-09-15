package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.ImportSynchState
import com.dtolabs.rundeck.plugins.scm.ScmImportSynchState

/**
 * Created by greg on 9/15/15.
 */
class GitImportSynchState implements ScmImportSynchState{
    ImportSynchState state
    String message
}
