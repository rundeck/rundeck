package org.rundeck.app.data.workflow

import org.rundeck.app.data.model.v1.job.workflow.ConditionalDefinition
import org.rundeck.app.data.model.v1.job.workflow.ConditionalSet
import spock.lang.Specification

/**
 * Tests for ConditionalSet and ConditionalDefinition implementations
 */
class ConditionalSetSpec extends Specification {

    def "ConditionalDefinitionImpl fromMap creates instance correctly"() {
        given:
        def defMap = [
            key: 'option.env',
            operator: '==',
            value: 'prod'
        ]

        when:
        ConditionalDefinition condDef = ConditionalDefinitionImpl.fromMap(defMap)

        then:
        condDef != null
        condDef.key == 'option.env'
        condDef.operator == '=='
        condDef.value == 'prod'
    }

    def "ConditionalDefinitionImpl fromMap returns null for null input"() {
        when:
        ConditionalDefinition condDef = ConditionalDefinitionImpl.fromMap(null)

        then:
        condDef == null
    }

    def "ConditionalDefinitionImpl toMap serializes correctly"() {
        given:
        ConditionalDefinitionImpl condDef = new ConditionalDefinitionImpl()
        condDef.key = 'option.env'
        condDef.operator = '!='
        condDef.value = 'dev'

        when:
        def map = condDef.toMap()

        then:
        map.key == 'option.env'
        map.operator == '!='
        map.value == 'dev'
    }

    def "ConditionalDefinitionImpl toMap handles null value"() {
        given:
        ConditionalDefinitionImpl condDef = new ConditionalDefinitionImpl()
        condDef.key = 'option.env'
        condDef.operator = 'exists'
        condDef.value = null

        when:
        def map = condDef.toMap()

        then:
        map.key == 'option.env'
        map.operator == 'exists'
        map.value == null
    }

    def "ConditionalSetImpl fromMap creates instance with single condition group"() {
        given:
        def setMap = [
            conditionGroups: [
                [
                    [key: 'option.env', operator: '==', value: 'prod']
                ]
            ],
            nodeStep: false
        ]

        when:
        ConditionalSet condSet = new ConditionalSetImpl().fromMap(setMap)

        then:
        condSet != null
        condSet.conditionGroups.size() == 1
        condSet.conditionGroups[0].size() == 1
        condSet.conditionGroups[0][0].key == 'option.env'
        condSet.nodeStep == false
    }

    def "ConditionalSetImpl fromMap creates instance with multiple OR groups"() {
        given:
        def setMap = [
            conditionGroups: [
                [
                    [key: 'option.env', operator: '==', value: 'prod']
                ],
                [
                    [key: 'option.env', operator: '==', value: 'staging']
                ]
            ],
            nodeStep: false
        ]

        when:
        ConditionalSet condSet = new ConditionalSetImpl().fromMap(setMap)

        then:
        condSet != null
        condSet.conditionGroups.size() == 2
        condSet.conditionGroups[0].size() == 1
        condSet.conditionGroups[1].size() == 1
    }

    def "ConditionalSetImpl fromMap creates instance with AND group"() {
        given:
        def setMap = [
            conditionGroups: [
                [
                    [key: 'option.env', operator: '==', value: 'prod'],
                    [key: 'option.region', operator: '==', value: 'us-east']
                ]
            ],
            nodeStep: true
        ]

        when:
        ConditionalSet condSet = new ConditionalSetImpl().fromMap(setMap)

        then:
        condSet != null
        condSet.conditionGroups.size() == 1
        condSet.conditionGroups[0].size() == 2
        condSet.nodeStep == true
    }

    def "ConditionalSetImpl fromMap returns null for null input"() {
        when:
        ConditionalSet condSet = new ConditionalSetImpl().fromMap(null)

        then:
        condSet == null
    }

    def "ConditionalSetImpl fromMap handles empty conditionGroups"() {
        given:
        def setMap = [
            conditionGroups: [],
            nodeStep: false
        ]

        when:
        ConditionalSet condSet = new ConditionalSetImpl().fromMap(setMap)

        then:
        condSet != null
        condSet.conditionGroups == null || condSet.conditionGroups.isEmpty()
    }

    def "ConditionalSetImpl toMap serializes correctly"() {
        given:
        def condDef1 = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condDef2 = ConditionalDefinitionImpl.fromMap([key: 'option.region', operator: '==', value: 'us-east'])

        ConditionalSetImpl condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef1, condDef2]]
        condSet.nodeStep = true

        when:
        def map = condSet.toMap()

        then:
        map.conditionGroups != null
        map.conditionGroups.size() == 1
        map.conditionGroups[0].size() == 2
        map.conditionGroups[0][0].key == 'option.env'
        map.conditionGroups[0][1].key == 'option.region'
        map.nodeStep == true
    }

    def "ConditionalSetImpl toMap handles multiple OR groups"() {
        given:
        def condDef1 = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condDef2 = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'staging'])

        ConditionalSetImpl condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef1], [condDef2]]
        condSet.nodeStep = false

        when:
        def map = condSet.toMap()

        then:
        map.conditionGroups != null
        map.conditionGroups.size() == 2
        map.conditionGroups[0].size() == 1
        map.conditionGroups[1].size() == 1
    }

    def "ConditionalSetImpl fromDataModel creates instance from data model"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def dataModelSet = new ConditionalSetImpl()
        dataModelSet.conditionGroups = [[condDef]]
        dataModelSet.nodeStep = false

        when:
        ConditionalSet result = ConditionalSetImpl.fromDataModel(dataModelSet)

        then:
        result != null
        result instanceof ConditionalSetImpl
        result.conditionGroups.size() == 1
    }

    def "ConditionalSetImpl fromDataModel returns null for null input"() {
        when:
        ConditionalSet result = ConditionalSetImpl.fromDataModel(null)

        then:
        result == null
    }

    def "ConditionalSetImpl fromDataModel throws exception for null in constructor"() {
        when:
        new ConditionalSetImpl(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "ConditionalSetImpl handles all valid operators"() {
        given:
        def operators = ['==', '!=', '>', '<', '>=', '<=', 'contains', 'matches', 'exists', 'not exists']

        when:
        def condDefs = operators.collect { op ->
            ConditionalDefinitionImpl.fromMap([key: 'option.test', operator: op, value: 'value'])
        }

        then:
        condDefs.size() == operators.size()
        condDefs.every { it != null }
        condDefs.eachWithIndex { cond, idx ->
            assert cond.operator == operators[idx]
        }
    }

    def "ConditionalSetImpl fromMap handles ConditionalDefinition objects in groups"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def setMap = [
            conditionGroups: [
                [condDef]
            ],
            nodeStep: false
        ]

        when:
        ConditionalSet condSet = new ConditionalSetImpl().fromMap(setMap)

        then:
        condSet != null
        condSet.conditionGroups.size() == 1
        condSet.conditionGroups[0].size() == 1
        condSet.conditionGroups[0][0] == condDef
    }
}

