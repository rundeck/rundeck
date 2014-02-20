package org.rundeck.storage.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 2/18/14 Time: 12:18 PM
 */
public interface ContentMeta {
    Map<String, String> getMeta();

    InputStream readContent() throws IOException;
}
