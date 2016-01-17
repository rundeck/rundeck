package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.jobs.JobReference;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Defines the manifest of repo files
 * tracked and mapping to Job definitions.
 */
public interface ScmRepoManifest {
    /**
     * Return the mapping of UUID to relative file path
     *
     * @return the mapping of UUID to relative file path
     */
    Map<String, String> getJobToFileMapping();

    String getRelativePathFor(String uuid);

    String getRelativePathFor(JobReference job);

    /**
     * Store the mapping to disk
     */
    void store(OutputStream out) throws IOException;

    /**
     * Add or update a UUID to filepathmapping entry
     *
     * @param uuid     uuid
     * @param filepath filepath
     */
    void setMapping(String uuid, String filepath);

    /**
     * Remove the entry for the UUID
     *
     * @param uuid uuid
     */
    void removeUUID(String uuid);

    /**
     * Remove the entry for the path
     *
     * @param path relative path
     */
    void removeFile(String path);
}
