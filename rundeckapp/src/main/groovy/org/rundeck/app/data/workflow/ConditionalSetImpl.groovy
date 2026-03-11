package org.rundeck.app.data.workflow

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.rundeck.app.data.model.v1.job.workflow.ConditionalDefinition
import org.rundeck.app.data.model.v1.job.workflow.ConditionalSet

/**
 * Implementation of ConditionalSet for deserialized conditional set data.
 * Contains condition groups organized as List<List<ConditionalDefinition>>
 * where inner lists are AND groups and outer list items are OR groups.
 */
@JsonIgnoreProperties(["errors"])
class ConditionalSetImpl implements ConditionalSet {
    List<List<ConditionalDefinition>> conditionGroups
    boolean nodeStep

    private final ConditionalSet dataModelConditionalSet;

    public ConditionalSetImpl() {
        this.dataModelConditionalSet = null;
    }

    public ConditionalSetImpl(ConditionalSet dataModelConditionalSet) {
        if (dataModelConditionalSet == null) {
            throw new IllegalArgumentException("ConditionalSet cannot be null");
        }
        this.dataModelConditionalSet = dataModelConditionalSet;
        this.conditionGroups = dataModelConditionalSet.conditionGroups;
        this.nodeStep = dataModelConditionalSet.isNodeStep();
    }

    /**
     * Create a ConditionalSetImpl instance from a Map representation
     * @param setMap Map containing conditional set data with key: conditionGroups
     * @return ConditionalSetImpl instance
     */
    ConditionalSet fromMap(Map<String, Object> setMap) {
        if (!setMap) {
            return null
        }

        def condSet = new ConditionalSetImpl()
        if (setMap.conditionGroups) {
            def groups = []
            if (setMap.conditionGroups instanceof List) {
                setMap.conditionGroups.each { group ->
                    if (group instanceof List) {
                        def andGroup = []
                        group.each { condDef ->
                            if (condDef instanceof Map) {
                                def cond = ConditionalDefinitionImpl.fromMap(condDef as Map<String, Object>)
                                if (cond) {
                                    andGroup.add(cond)
                                }
                            } else if (condDef instanceof ConditionalDefinition) {
                                andGroup.add(condDef)
                            }
                        }
                        if (andGroup) {
                            groups.add(andGroup)
                        }
                    }
                }
            }
            condSet.conditionGroups = groups
            condSet.nodeStep = setMap.nodeStep == true
        }
        return condSet
    }

    /**
     * Convert to canonical map representation for serialization
     * @return Map representation
     */
    Map toMap() {
        def map = [:]
        if (conditionGroups) {
            def groups = []
            conditionGroups.each { group ->
                def andGroup = []
                group.each { condDef ->
                    if (condDef instanceof ConditionalDefinitionImpl) {
                        andGroup.add(condDef.toMap())
                    } else if (condDef instanceof Map) {
                        andGroup.add(condDef)
                    } else {
                        // Fallback: create a map from the ConditionalDefinition interface
                        def condMap = [:]
                        if (condDef.key) condMap.key = condDef.key
                        if (condDef.operator) condMap.operator = condDef.operator
                        if (condDef.value != null) condMap.value = condDef.value
                        andGroup.add(condMap)
                    }
                }
                if (andGroup) {
                    groups.add(andGroup)
                }
            }
            map.conditionGroups = groups
            map.nodeStep = nodeStep
        }
        return map
    }

    /**
     * Factory method to create ConditionSetImpl from data model ConditionalSet
     * @param dataModelConditionalSet Data model ConditionalSet
     * @return ConditionSetImpl instance
     */
    public static ConditionalSet fromDataModel(ConditionalSet dataModelConditionalSet) {
        if (dataModelConditionalSet == null) {
            return null;
        }
        return new ConditionalSetImpl(dataModelConditionalSet);
    }
}

