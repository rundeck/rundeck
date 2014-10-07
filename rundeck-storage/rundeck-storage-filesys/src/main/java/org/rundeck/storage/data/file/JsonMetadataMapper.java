package org.rundeck.storage.data.file;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
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
        HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        //merge metadata by reading in existing metadata
        if(destination.exists()) {
            stringStringHashMap.putAll(readMetadata(destination));
        }
        Map map=meta;
        for (Object o : map.keySet()) {
            if (null != o && null != map.get(o)) {
                stringStringHashMap.put(o.toString(), map.get(o).toString());
            }
        }
        objectMapper.writeValue(destination, stringStringHashMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> readMetadata(File metadata) throws IOException {
        if(metadata.isFile()){
            return objectMapper.readValue(metadata, Map.class);
        } else {
            return Collections.<String, String>emptyMap();
        }
    }
}
