package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.plugins.views.Action;

import java.util.List;

/**
 * Result of a Diff between old job data and new
 */
public interface ScmDiffResult {
    /**
     * @return true if there are differences between the source and target
     */
    boolean getModified();

    /**
     * @return true if the old file was not found (does not exist)
     */
    boolean getOldNotFound();

    /**
     * @return true if the new file was not found (deleted)
     */
    boolean getNewNotFound();

    /**
     * @return diff contents
     */
    String getContent();

    /**
     * @return list of actions that can be taken
     */
    List<Action> getActions();
}
