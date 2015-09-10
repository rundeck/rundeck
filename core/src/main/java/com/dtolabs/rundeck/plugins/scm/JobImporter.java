package com.dtolabs.rundeck.plugins.scm;

import java.io.InputStream;
import java.util.Map;

/**
 * Can import a job
 */
public interface JobImporter {
    /**
     * Import a serialized job
     *
     * @param format         format, 'xml' or 'yaml'
     * @param input          input stream
     * @param importMetadata metadata to attach to the job
     *
     * @return result
     */
    ImportResult importFromStream(String format, InputStream input, Map importMetadata);

    /**
     * Import a Map-representation of a Job
     *
     * @param input          input map data
     * @param importMetadata metadata to attach to the job
     *
     * @return result
     */
    ImportResult importFromMap(Map input, Map importMetadata);
}
