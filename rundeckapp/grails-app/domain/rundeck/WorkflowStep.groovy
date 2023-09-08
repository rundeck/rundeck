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

import com.dtolabs.rundeck.app.support.DomainIndexHelper
import com.fasterxml.jackson.databind.ObjectMapper
import grails.gorm.dirty.checking.DirtyCheck
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import rundeck.data.validation.shared.SharedWorkflowStepConstraints

@DirtyCheck
abstract class WorkflowStep {
    Boolean enabled = true
    WorkflowStep errorHandler
    Boolean keepgoingOnSuccess
    String description
    String pluginConfigData
    static belongsTo = [Workflow, WorkflowStep]
    static constraints = {
        importFrom SharedWorkflowStepConstraints
        errorHandler(nullable: true)
        pluginConfigData(nullable: true, blank: true)
        enabled(defaultValue: true)
    }
    //ignore fake property 'configuration' and do not store it
    static transients = ['pluginConfig']
    static mapping = {
        pluginConfigData(type: 'text')
        errorHandler lazy: false

        DomainIndexHelper.generate(delegate) {
            index 'IDX_ERROR_HANDLER', ['errorHandler']
        }
    }

    public String summarize() {
        return this.toString()
    }


    public Map getPluginConfig() {
        //de-serialize the json
        if (null != pluginConfigData) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(pluginConfigData, Map.class)
        } else {
            return null
        }
    }

    public Object getPluginConfigForType(String type) {
        return getPluginConfig()?.get(type)
    }

    /**
     *
     * @param type
     * @return
     */
    public List getPluginConfigListForType(String type) {
        def val = getPluginConfig()?.get(type)
        val && !(val instanceof Collection) ? [val] : val
    }
    /**
     * store plugin configuration for a type
     */
    public void storePluginConfigForType(String key, Object obj) {
        def config = getPluginConfig() ?: [:]
        config.put(key, obj)
        setPluginConfig(config)
    }


    public void setPluginConfig(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            pluginConfigData = mapper.writeValueAsString(obj)
        } else {
            pluginConfigData = null
        }
    }

    abstract WorkflowStep createClone()

    abstract Map toMap()
}
