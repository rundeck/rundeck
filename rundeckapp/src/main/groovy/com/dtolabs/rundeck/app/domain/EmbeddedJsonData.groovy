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

package com.dtolabs.rundeck.app.domain

import com.fasterxml.jackson.databind.ObjectMapper

trait EmbeddedJsonData {

    Map asJsonMap(String data) {
        //de-serialize the json
        if (null != data) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(data, Map.class)
        } else {
            return null
        }
    }

    List asJsonList(String data) {
        //de-serialize the json
        if (null != data) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(data, List.class)
        } else {
            return null
        }
    }


    String serializeJsonMap(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.writeValueAsString(obj)
        } else {
            return null
        }
    }

    String serializeJsonList(List obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.writeValueAsString(obj)
        } else {
            return null
        }
    }
}
