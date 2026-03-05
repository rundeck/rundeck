package org.rundeck.app.data.workflow

import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.job.workflow.ConditionalDefinition
import org.rundeck.app.data.model.v1.job.workflow.ConditionalSet
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.PluginStep
import spock.lang.Specification

/**
 * Tests for ConditionalStep data model
 */
class ConditionalStepSpec extends Specification implements DataTest {

    void setupSpec() {
        mockDomains CommandExec, JobExec, PluginStep
    }

    def "ConditionalStep fromMap creates instance with conditionSet and subSteps"() {
        given:
        def stepMap = [
            conditionGroups: [
                [
                    [key: 'option.env', operator: '==', value: 'prod']
                ]
            ],
            subSteps: [
                [type: 'exec', exec: 'echo test'],
                [type: 'exec', exec: 'echo test2']
            ],
            nodeStep: false,
            description: 'Test conditional step'
        ]

        when:
        ConditionalStep step = ConditionalStep.fromMap(stepMap)

        then:
        step != null
        step.conditionSet != null
        step.conditionSet.conditionGroups.size() == 1
        step.conditionSet.conditionGroups[0].size() == 1
        step.conditionSet.conditionGroups[0][0].key == 'option.env'
        step.conditionSet.conditionGroups[0][0].operator == '=='
        step.conditionSet.conditionGroups[0][0].value == 'prod'
        step.subSteps != null
        step.subSteps.size() == 2
        step.nodeStep == false
        step.description == 'Test conditional step'
    }

    def "ConditionalStep fromMap with job reference subSteps"() {
        given:
        def stepMap = [
            conditionGroups: [
                [
                    [key: 'step.status', operator: '==', value: 'success']
                ]
            ],
            subSteps: [
                [jobref: [group: 'test', name: 'job1']]
            ],
            nodeStep: false
        ]

        when:
        ConditionalStep step = ConditionalStep.fromMap(stepMap)

        then:
        step != null
        step.subSteps != null
        step.subSteps.size() == 1
        step.subSteps[0] instanceof JobExec
    }

    def "ConditionalStep fromMap with error handler"() {
        given:
        def stepMap = [
            conditionGroups: [
                [
                    [key: 'option.env', operator: '==', value: 'prod']
                ]
            ],
            subSteps: [
                [type: 'exec', exec: 'echo test']
            ],
            errorhandler: [type: 'exec', exec: 'echo error'],
            nodeStep: false
        ]

        when:
        ConditionalStep step = ConditionalStep.fromMap(stepMap)

        then:
        step != null
        step.errorHandler != null
        step.errorHandler instanceof CommandExec
    }

    def "ConditionalStep toMap serializes correctly"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]
        condSet.nodeStep = false

        def subStep1 = new CommandExec(adhocRemoteString: 'echo test1')
        def subStep2 = new CommandExec(adhocRemoteString: 'echo test2')

        ConditionalStep step = new ConditionalStep()
        step.conditionSet = condSet
        step.subSteps = [subStep1, subStep2]
        step.nodeStep = false
        step.description = 'Test step'

        when:
        def map = step.toMap()

        then:
        map.type == 'conditional'
        map.nodeStep == false
        map.description == 'Test step'
        map.conditionGroups != null
        map.conditionGroups.size() == 1
        map.conditionGroups[0].size() == 1
        map.conditionGroups[0][0].key == 'option.env'
        map.subSteps != null
        map.subSteps.size() == 2
    }

    def "ConditionalStep validation fails when conditionSet is null"() {
        given:
        ConditionalStep step = new ConditionalStep()
        step.conditionSet = null
        step.subSteps = [new CommandExec(adhocRemoteString: 'echo test')]

        when:
        def valid = step.validate()

        then:
        !valid
        step.errors.hasFieldErrors('conditionSet')
    }

    def "ConditionalStep validation fails when subSteps is empty"() {
        given:
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])]]

        ConditionalStep step = new ConditionalStep()
        step.conditionSet = condSet
        step.subSteps = []

        when:
        def valid = step.validate()

        then:
        !valid
        step.errors.hasFieldErrors('subSteps')
    }

    def "ConditionalStep validation fails when conditionGroups is empty"() {
        given:
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = []

        ConditionalStep step = new ConditionalStep()
        step.conditionSet = condSet
        step.subSteps = [new CommandExec(adhocRemoteString: 'echo test')]

        when:
        def valid = step.validate()

        then:
        !valid
        step.errors.hasFieldErrors('conditionSet')
    }

    def "ConditionalStep validation fails when condition key is empty"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: '', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        ConditionalStep step = new ConditionalStep()
        step.conditionSet = condSet
        step.subSteps = [new CommandExec(adhocRemoteString: 'echo test')]

        when:
        def valid = step.validate()

        then:
        !valid
        step.errors.hasFieldErrors('conditionSet')
    }

    def "ConditionalStep validation fails when operator is invalid"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: 'invalid', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        ConditionalStep step = new ConditionalStep()
        step.conditionSet = condSet
        step.subSteps = [new CommandExec(adhocRemoteString: 'echo test')]

        when:
        def valid = step.validate()

        then:
        !valid
        step.errors.hasFieldErrors('conditionSet')
    }

    def "ConditionalStep validation fails when nested ConditionalStep in subSteps"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def nestedCondSet = new ConditionalSetImpl()
        nestedCondSet.conditionGroups = [[ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'dev'])]]

        def nestedStep = new ConditionalStep()
        nestedStep.conditionSet = nestedCondSet
        nestedStep.subSteps = [new CommandExec(adhocRemoteString: 'echo nested')]

        ConditionalStep step = new ConditionalStep()
        step.conditionSet = condSet
        step.subSteps = [nestedStep]

        when:
        def valid = step.validate()

        then:
        !valid
        step.errors.hasFieldErrors('subSteps')
    }

    def "ConditionalStep getPluginType returns 'conditional'"() {
        given:
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])]]

        ConditionalStep step = new ConditionalStep()
        step.conditionSet = condSet
        step.subSteps = [new CommandExec(adhocRemoteString: 'echo test')]

        when:
        def pluginType = step.getPluginType()

        then:
        pluginType == 'conditional'
    }

    def "ConditionalStep instanceOf returns true for ConditionalStep class"() {
        given:
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])]]

        ConditionalStep step = new ConditionalStep()
        step.conditionSet = condSet
        step.subSteps = [new CommandExec(adhocRemoteString: 'echo test')]

        when:
        def result = step.instanceOf(ConditionalStep)

        then:
        result == true
    }

    def "ConditionalStep fromMap returns null when conditionGroups missing"() {
        given:
        def stepMap = [
            subSteps: [
                [type: 'exec', exec: 'echo test']
            ]
        ]

        when:
        ConditionalStep step = ConditionalStep.fromMap(stepMap)

        then:
        step == null
    }

    def "ConditionalStep with multiple condition groups (OR logic)"() {
        given:
        def stepMap = [
            conditionGroups: [
                [
                    [key: 'option.env', operator: '==', value: 'prod']
                ],
                [
                    [key: 'option.env', operator: '==', value: 'staging']
                ]
            ],
            subSteps: [
                [type: 'exec', exec: 'echo test']
            ],
            nodeStep: false
        ]

        when:
        ConditionalStep step = ConditionalStep.fromMap(stepMap)

        then:
        step != null
        step.conditionSet.conditionGroups.size() == 2
        step.conditionSet.conditionGroups[0].size() == 1
        step.conditionSet.conditionGroups[1].size() == 1
    }

    def "ConditionalStep with AND group (multiple conditions in one group)"() {
        given:
        def stepMap = [
            conditionGroups: [
                [
                    [key: 'option.env', operator: '==', value: 'prod'],
                    [key: 'option.region', operator: '==', value: 'us-east']
                ]
            ],
            subSteps: [
                [type: 'exec', exec: 'echo test']
            ],
            nodeStep: false
        ]

        when:
        ConditionalStep step = ConditionalStep.fromMap(stepMap)

        then:
        step != null
        step.conditionSet.conditionGroups.size() == 1
        step.conditionSet.conditionGroups[0].size() == 2
    }
}

