package com.dtolabs.rundeck.plugins.scm;

/**
 * Created by greg on 9/29/15.
 */
public interface ScmImportDiffResult extends ScmDiffResult {

    /**
     * @return the last incoming commit info if available
     */
    ScmCommitInfo getIncomingCommit();
}
