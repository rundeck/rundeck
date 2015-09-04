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
     * @return result message
     */
    public String getMessage();

    /**
     * @return Id associated with export, if any
     */
    public String getId();
}
