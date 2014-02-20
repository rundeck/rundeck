package org.rundeck.storage.data.file;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 2/18/14 Time: 11:12 AM
 */
public class JsonMetadataMapper implements MetadataMapper {
    private ObjectMapper objectMapper;

    public JsonMetadataMapper() {
        objectMapper = new ObjectMapper();
    }

    public JsonMetadataMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    @Override
    public void writeMetadata(Map<String, String> meta, File destination) throws IOException {
        if (!destination.getParentFile().exists()) {
            destination.getParentFile().mkdirs();
        }
        objectMapper.writeValue(destination, meta);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> readMetadata(File metadata) throws IOException {
        return objectMapper.readValue(metadata, Map.class);
    }
}
