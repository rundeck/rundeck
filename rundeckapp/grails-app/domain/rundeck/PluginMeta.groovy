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

package rundeck

import com.fasterxml.jackson.databind.ObjectMapper

class PluginMeta {

    static constraints = {
        jsonData(nullable: true, blank: true)
    }
    static mapping = {
        key column: 'data_key'
        jsonData(type: 'text')
    }
    Long id
    String key
    String project
    String jsonData
    Date dateCreated
    Date lastUpdated

    //ignore fake property 'configuration' and do not store it
    static transients = ['pluginData']

    public Map getPluginData() {
        //de-serialize the json
        if (null != jsonData) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(jsonData, Map.class)
        } else {
            return null
        }

    }

    /**
     * store data under a key
     */
    public void storePluginData(String key, Object obj) {
        setPluginData(getPluginData().put(key, obj))
    }

    public void setPluginData(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            jsonData = mapper.writeValueAsString(obj)
        } else {
            jsonData = null
        }
    }
}
