package com.dtolabs.rundeck.core.resourcetree;

import java.io.IOException;
import java.io.InputStream;

/**
 * $INTERFACE is ... User: greg Date: 2/19/14 Time: 12:32 PM
 */
public interface HasResourceStream {
    public InputStream getInputStream() throws IOException;
}
