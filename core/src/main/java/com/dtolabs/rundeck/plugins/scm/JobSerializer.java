package com.dtolabs.rundeck.plugins.scm;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Can serialize a job to an outputstream in a format
 */
public interface JobSerializer {

    /**
     * @param format       format name: 'xml' or 'yaml'
     * @param outputStream destination
     */
    void serialize(String format, OutputStream outputStream) throws IOException;
}
