/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
