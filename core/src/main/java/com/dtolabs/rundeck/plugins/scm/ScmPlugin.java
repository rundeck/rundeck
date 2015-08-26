package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by greg on 4/27/15.
 */
public interface ScmPlugin {

    /**
     * SCM status of a job
     */
    enum ScmFileStatus {
        NOT_MODIFIED,
        MODIFIED,
        /**
         * Created in Rundeck, not yet committed
         */
        CREATED,
        /**
         * Deleted in Rundeck, not yet deleted in scm
         */
        DELETED,
        /**
         * Not present in the repo (untracked)
         */
        NOT_PRESENT,
        /**
         *
         */
        CONFLICT
    }

    /**
     * Return the relative path for the given file within the working dir.
     * @param reference file
     * @return path
     */
    String relativePath(File reference);


    /**
     * Return false if {@link #setup(Map)} needs to be executed
     */
    boolean isSetup();

    /**
     * First init, determine if already setup
     */
    void init();

    /**
     * Define UI properties for setup action
     *
     * @return list of properties
     */
    List<Property> getSetupProperties();

    /**
     * Perform setup with the input
     *
     * @param input result of GUI input
     */
    void setup(Map<String, Object> input) throws ScmPluginException;


    /**
     * Get the status of a job
     *
     * @param reference job reference
     *
     * @return status
     */
    ScmFileStatus fileStatus(File reference) throws ScmPluginException;


}
