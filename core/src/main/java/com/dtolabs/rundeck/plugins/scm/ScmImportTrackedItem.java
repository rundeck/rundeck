package com.dtolabs.rundeck.plugins.scm;

/**
 * an item tracked by import plugin
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
}
