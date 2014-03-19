package org.rundeck.storage.api;

import java.io.IOException;
import java.io.InputStream;

/**
 * HasInputStream provides lazy loading of an input stream that might cause an exception
 *
 * @author greg
 * @since 2014-02-19
 */
public interface HasInputStream {
    public InputStream getInputStream() throws IOException;
}
