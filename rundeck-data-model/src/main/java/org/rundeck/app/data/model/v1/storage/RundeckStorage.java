/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.app.data.model.v1.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.rundeck.app.data.utils.Utils;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface RundeckStorage {

    String getNamespace();
    String getDir();
    String getName();
    String getJsonData();
    String getPathSha();
    byte[] getData();
    Date getLastUpdated();
    Date getDateCreated();

    static String setupSha(RundeckStorage storage) {
        return Utils.encodeAsSHA256((!StringUtils.isBlank(storage.getNamespace()) ? storage.getNamespace(): "") + ':' +
                    getPath(storage.getDir(), storage.getName()));
    }

    static String getPath(String directory, String name) {
        return (!StringUtils.isBlank(directory) ? (directory+'/') : "" )+name;
    }

    static String storageMetaAsString(Map obj) throws JsonProcessingException {
        //serialize json and store into field
        String jsonData = null;
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper();
            jsonData = mapper.writeValueAsString(obj);
        }

        return jsonData
    }

    static Map storageMetaAsMap(String jsonData) throws JsonProcessingException{
        //de-serialize the json
        if (null != jsonData) {
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonData, Map.class);
        } else {
            return null;
        }

    }


}
