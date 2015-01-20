package org.rundeck.storage.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * HasInputStream provides lazy loading of an input stream that might cause an exception
 *
 * @author greg
 * @since 2014-02-19
 */
public interface HasInputStream {
    public InputStream getInputStream() throws IOException;

    /**
     * Write the content stream to the output stream
     *
     * @param outputStream output stream
     * @return the content stream
     *
     * @throws IOException on io error
     */
    long writeContent(OutputStream outputStream) throws IOException;
}
