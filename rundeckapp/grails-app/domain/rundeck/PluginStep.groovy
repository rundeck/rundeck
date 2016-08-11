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

class PluginStep extends WorkflowStep{
    boolean nodeStep
    String type
    String jsonData
    static constraints = {
        type nullable: false, blank: false
        jsonData(nullable: true, blank: true)
    }
    //ignore fake property 'configuration' and do not store it
    static transients = ['configuration']

    public Map getConfiguration() {
        //de-serialize the json
        if (null != jsonData) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(jsonData, Map.class)
        } else {
            return null
        }

    }

    public void setConfiguration(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            jsonData = mapper.writeValueAsString(obj)
        } else {
            jsonData = null
        }
    }

    static mapping = {
        jsonData(type: 'text')
    }

    public Map toMap() {
        def map=[type: type, nodeStep:nodeStep]
        if(this.configuration){
            map.put('configuration',this.configuration)
        }
        if (description) {
            map.description = description
        }
        if (errorHandler) {
            map.errorhandler = errorHandler.toMap()
        } else if (keepgoingOnSuccess) {
            map.keepgoingOnSuccess = keepgoingOnSuccess
        }
        map
    }

    static PluginStep fromMap(Map data) {
        PluginStep ce = new PluginStep()
        ce.nodeStep=data.nodeStep
        ce.type=data.type
        ce.configuration=data.configuration

        ce.keepgoingOnSuccess = !!data.keepgoingOnSuccess
        ce.description=data.description?.toString()
        return ce
    }

    public PluginStep createClone() {
        return new PluginStep(type: type, nodeStep: nodeStep, jsonData: jsonData,keepgoingOnSuccess: keepgoingOnSuccess,description: description)
    }

    @Override
    public String toString() {
        return "PluginStep{" +
               "nodeStep=" + nodeStep +
               ", type='" + type + '\'' +
               ", jsonData='" + jsonData + '\'' +
               '}';
    }

    @Override
    public String summarize() {
        return "Plugin["+ type + ', nodeStep: '+nodeStep+']';
    }
}
