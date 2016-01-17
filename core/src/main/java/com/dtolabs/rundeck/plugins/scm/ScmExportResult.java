package com.dtolabs.rundeck.plugins.scm;

/**
 * Result of Export action
 */
public interface ScmExportResult {
    /**
     * @return true if export was successful, false otherwise
     */
    public boolean isSuccess();

    /**
     * @return true if an error occurred, false otherwise
     */
    public boolean isError();

    /**
     * @return basic result message
     */
    public String getMessage();

    /**
     * @return extended result message
     */
    public String getExtendedMessage();

    /**
     * @return Id associated with export, if any
     */
    public String getId();
    /**
     * @return info for the exported commit, to synch with import status
     */
    ScmCommitInfo getCommit();
}
