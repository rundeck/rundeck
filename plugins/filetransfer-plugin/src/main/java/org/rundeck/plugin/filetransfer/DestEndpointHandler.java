package org.rundeck.plugin.filetransfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by tuto on 10-06-16.
 */
public interface DestEndpointHandler {

  public OutputStream getOutputStream() throws IOException;

  public boolean finish() throws IOException;
}
