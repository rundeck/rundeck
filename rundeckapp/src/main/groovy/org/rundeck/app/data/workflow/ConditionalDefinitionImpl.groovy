package org.rundeck.app.data.workflow

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.rundeck.app.data.model.v1.job.workflow.ConditionalDefinition

/**
 * Implementation of ConditionalDefinition for deserialized conditional definition data.
 */
@JsonIgnoreProperties(["errors"])
class ConditionalDefinitionImpl implements ConditionalDefinition {
    String key
    String operator
    Object value

    /**
     * Create a ConditionalDefinitionImpl instance from a Map representation
     * @param defMap Map containing conditional definition data with keys: key, operator, value
     * @return ConditionalDefinitionImpl instance
     */
    static ConditionalDefinitionImpl fromMap(Map<String, Object> defMap) {
        if (!defMap) {
            return null
        }

        def condDef = new ConditionalDefinitionImpl()
        condDef.key = defMap.key
        condDef.operator = defMap.operator
        condDef.value = defMap.value
        return condDef
    }

    /**
     * Convert to canonical map representation for serialization
     * @return Map representation
     */
    Map toMap() {
        def map = [:]
        if (key) {
            map.key = key
        }
        if (operator) {
            map.operator = operator
        }
        if (value != null) {
            map.value = value
        }
        return map
    }
}

