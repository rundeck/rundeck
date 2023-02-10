package org.rundeck.app.data.workflow

import org.rundeck.app.data.model.v1.job.workflow.WorkflowData

class WorkflowDataUtil {

    /**
     * Get the config for a type, wraps the value as a list if it is not a collection
     * @param type
     * @return available config data, as a List, or null
     */
    static def getPluginConfigDataList(WorkflowData workflowData, String type) {
        def map = workflowData.pluginConfigMap
        def val = map?.get(type)
        if (val && !(val instanceof Collection)) {
            val = [val]
        }
        val
    }
}
