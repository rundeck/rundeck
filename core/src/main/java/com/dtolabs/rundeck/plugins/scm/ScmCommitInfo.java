package com.dtolabs.rundeck.plugins.scm;

import java.util.Date;
import java.util.Map;

/**
 * SCM commit information
 */
public interface ScmCommitInfo {
    /**
     * @return Map representation of the data, if available
     */
    Map asMap();

    /**
     * @return Commit ID as a string
     */
    String getCommitId();

    /**
     * @return Commit message
     */
    String getMessage();

    /**
     * @return author
     */
    String getAuthor();

    /**
     * @return commit date
     */
    Date getDate();
}
