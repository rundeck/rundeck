package org.rundeck.storage.data.file;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 2/18/14 Time: 11:13 AM
 */
public interface MetadataMapper {
    void writeMetadata(Map<String, String> meta, File destination) throws IOException;
    Map<String,String> readMetadata(File metadata) throws IOException;
}
