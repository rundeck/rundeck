package com.dtolabs.rundeck.plugins.scm;

/**
 * an item tracked by import plugin, such as a path in a repository.
 */
public interface ScmImportTrackedItem {

    /**
     * @return unique identifier, e.g. relative filepath
     */
    String getId();

    /**
     * @return text title
     */
    String getTitle();

    /**
     * @return e.g. glyphicon-file
     */
    String getIconName();

    /**
     * @return associated Job ID if known
     */
    String getJobId();


    /**
     * @return true if the item is selected
     */
    boolean isSelected();
}
