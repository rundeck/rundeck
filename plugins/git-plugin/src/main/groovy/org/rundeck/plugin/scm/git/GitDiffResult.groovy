package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.plugins.scm.ScmCommitInfo
import com.dtolabs.rundeck.plugins.scm.ScmImportDiffResult

/**
 * Created by greg on 8/25/15.
 */
class GitDiffResult implements ScmImportDiffResult {
    boolean modified;
    boolean oldNotFound;
    boolean newNotFound
    String content
    List<Action> actions

    ScmCommitInfo incomingCommit
}
