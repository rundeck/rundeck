package org.rundeck.plugin.filetransfer;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by tuto on 10-06-16.
 */
public interface SourceEndpointHandler {

  public InputStream getInputStream() throws IOException;

  public boolean finish() throws IOException;
}
