package com.dtolabs.rundeck.plugins.scm;

/**
 * Created by greg on 8/25/15.
 */
public interface ScmDiffResult {
    /**
     * @return true if there are differences between the source and target
     */
    boolean getModified();

    /**
     * @return true if the source was not found
     */
    boolean getSourceNotFound();

    /**
     * @return true if the target was not found
     */
    boolean getTargetNotFound();

    /**
     * @return content type
     */
    String getContentType();

    /**
     * @return diff contents
     */
    String getContent();
}
