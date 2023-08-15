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

package rundeck.services.workflow


import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateImpl
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateImplTest
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStepStateImpl
import grails.converters.JSON
import spock.lang.Specification
import spock.lang.Unroll

import static com.dtolabs.rundeck.core.execution.workflow.state.StateUtils.stepIdentifier

@Unroll
class StateMappingSpec extends Specification {

    public static final ArrayList<LinkedHashMap<String, LinkedHashMap<String, Serializable>>> STATE_CHANGES1 = [
        [
            "workflow": [
                "date" : "2014-09-05T21:28:04Z",
                "nodes": [
                    "localhost"
                ],
                "state": "RUNNING"
            ]
        ],
        [
            "step": [
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1"
            ]
        ],
        [
            "step": [
                "node"        : "localhost",
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1"
            ]
        ],
        [
            "step": [
                "node"        : "localhost",
                "meta"        : [
                    "failureReason": "NonZeroResultCode"
                ],
                "state"       : "FAILED",
                "errorMessage": "Result code was 1",
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1"
            ]
        ],
        [
            "step": [
                "meta"        : [
                    "handlerTriggered": "true"
                ],
                "state"       : "RUNNING_HANDLER",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1e"
            ]
        ],
        [
            "step": [
                "node"        : "localhost",
                "meta"        : null,
                "state"       : "RUNNING_HANDLER",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1e"
            ]
        ],
        //this state transition should apply to step "1"
        [
            "subworkflow": [
                "quell": false,
                "nodes": [
                    "alpha1",
                    "alpha2",
                    "alpha4"
                ],
                "state": "RUNNING",
                "index": 0,
                "ident": "1e@node=localhost",
                "date" : "2014-09-05T21:28:04Z"
            ]
        ],
        [
            "step": [
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],
        [
            "step": [
                "node"        : "alpha1",
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],
        [
            "step": [
                "node"        : "alpha1",
                "meta"        : null,
                "state"       : "SUCCEEDED",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:05Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],
        [
            "step": [
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:05Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],
        [
            "step": [
                "node"        : "alpha2",
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:05Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],
        [
            "step": [
                "node"        : "alpha2",
                "meta"        : null,
                "state"       : "SUCCEEDED",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:05Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],
        [
            "step": [
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:05Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],
        [
            "step": [
                "node"        : "alpha4",
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:05Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],
        [
            "step": [
                "node"        : "alpha4",
                "meta"        : null,
                "state"       : "SUCCEEDED",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:06Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],
        [
            "subworkflow": [
                "quell": false,
                "nodes": null,
                "state": "SUCCEEDED",
                "index": 0,
                "ident": "1e@node=localhost",
                "date" : "2014-09-05T21:28:06Z"
            ]
        ],
        [
            "step": [
                "node"        : "localhost",
                "meta"        : null,
                "state"       : "SUCCEEDED",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:06Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost"
            ]
        ],
        [
            "workflow": [
                "date" : "2014-09-05T21:28:06Z",
                "nodes": [
                    "localhost"
                ],
                "state": "SUCCEEDED"
            ]
        ],
    ]
    public static final ArrayList<LinkedHashMap<String, LinkedHashMap<String, Serializable>>> STATE_CHANGES2 = [
        [
            "workflow": [
                "date" : "2014-09-05T21:28:04Z",
                "nodes": [
                    "localhost"
                ],
                "state": "RUNNING"
            ]
        ],
        [
            "step": [
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1"
            ]
        ],
        [
            "step": [
                "node"        : "localhost",
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1"
            ]
        ],
        [
            "step": [
                "node"        : "localhost",
                "meta"        : [
                    "failureReason": "NonZeroResultCode"
                ],
                "state"       : "FAILED",
                "errorMessage": "Result code was 1",
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1"
            ]
        ],
        [
            "step": [
                "meta"        : [
                    "handlerTriggered": "true"
                ],
                "state"       : "RUNNING_HANDLER",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1e"
            ]
        ],
        [
            "step": [
                "node"        : "localhost",
                "meta"        : null,
                "state"       : "RUNNING_HANDLER",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1e"
            ]
        ],
        //this state transition should apply to step "1"
        [
            "subworkflow": [
                "quell": false,
                "nodes": [
                    "alpha1",
                    "alpha2",
                    "alpha4"
                ],
                "state": "RUNNING",
                "index": 0,
                "ident": "1e@node=localhost",
                "date" : "2014-09-05T21:28:04Z"
            ]
        ],
        [
            "step": [
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:04Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],

        [
            "step": [
                "node"        : "localhost",
                "meta"        : null,
                "state"       : "RUNNING",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:05Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],
        [
            "step": [
                "node"        : "localhost",
                "meta"        : null,
                "state"       : "SUCCEEDED",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:06Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost/1"
            ]
        ],
        [
            "subworkflow": [
                "quell": false,
                "nodes": null,
                "state": "SUCCEEDED",
                "index": 0,
                "ident": "1e@node=localhost",
                "date" : "2014-09-05T21:28:06Z"
            ]
        ],
        [
            "step": [
                "node"        : "localhost",
                "meta"        : null,
                "state"       : "SUCCEEDED",
                "errorMessage": null,
                "date"        : "2014-09-05T21:28:06Z",
                "index"       : 0,
                "ident"       : "1e@node=localhost"
            ]
        ],
        [
            "workflow": [
                "date" : "2014-09-05T21:28:06Z",
                "nodes": [
                    "localhost"
                ],
                "state": "SUCCEEDED"
            ]
        ],
    ]

    def "stepStatesForNode from grails json parser with missing nodeState"() {
        given:
            def sut = new StateMapping()
            def json = '''{
"nodes":{
"Anode":[
    {
        "stepctx":"1/1"
    }
],
"MissingNode":[
    {
        "stepctx":"1/1"
    }
]
},
"steps":[
    {
    "parameterStates":{},
    "workflow":{
        "steps":[
            {
                "nodeStates":{
                    "Anode":{"state":"test"}
                }
            }
        ]
    }
    }
]
}'''
            def map = JSON.parse(json)
        when:
            def result = sut.stepStatesForNode(map, node)
        then:
            result == expect
        where:
            node          | expect
            'Anode'       | [[stepctx: '1/1', state: 'test']]
            'MissingNode' | [[stepctx: '1/1']]

    }


    def "mapof"() {
        given:
            def sut = new StateMapping()
            def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
            mutableStep1.nodeStep = true
            def state = new MutableWorkflowStateImpl(['localhost'], 1, [0: mutableStep1])
            MutableWorkflowStateImplTest.processStateChanges(state, STATE_CHANGES1)
        when:
            def result = sut.mapOf(1, state)
        then:
            result.executionId == expect.executionId
            for (def k : result.keySet()) {
                assert result[k] == expect[k]
            }
        where:
            expect = [
                executionId   : 1,
                nodes         : [
                    alpha2   : [[stepctx: '1/1', executionState: 'SUCCEEDED']],
                    alpha4   : [[stepctx: '1/1', executionState: 'SUCCEEDED']],
                    alpha1   : [[stepctx: '1/1', executionState: 'SUCCEEDED']],
                    localhost: [[stepctx: "1", executionState: 'SUCCEEDED']]
                ],
                serverNode    : null,
                executionState: 'SUCCEEDED',
                completed     : true,
                targetNodes   : ['localhost'],
                allNodes      : ['localhost', 'alpha1', 'alpha2', 'alpha4'],
                stepCount     : 1,
                updateTime    : '2014-09-06T04:28:06Z',
                startTime     : '2014-09-06T04:28:04Z',
                endTime       : '2014-09-06T04:28:06Z',
                steps         : [
                    [
                        hasSubworkflow : true,
                        workflow       : [
                            executionState: 'SUCCEEDED',
                            completed     : true,
                            targetNodes   : ['alpha1', 'alpha2', 'alpha4'],
                            allNodes      : [],
                            stepCount     : 1, updateTime: '2014-09-06T04:28:06Z', startTime: '2014-09-06T04:28:04Z',
                            endTime       : '2014-09-06T04:28:06Z',
                            steps         : [
                                [
                                    nodeStates     :
                                        [
                                            alpha2: [executionState: 'SUCCEEDED', startTime: '2014-09-06T04:28:05Z',
                                                     updateTime    : '2014-09-06T04:28:05Z', duration: 0L, endTime:
                                                         '2014-09-06T04:28:05Z'],
                                            alpha4: [executionState: 'SUCCEEDED', startTime: '2014-09-06T04:28:05Z',
                                                     updateTime    : '2014-09-06T04:28:06Z', duration: 1000L, endTime:
                                                         '2014-09-06T04:28:06Z'],
                                            alpha1: [executionState: 'SUCCEEDED', startTime: '2014-09-06T04:28:04Z',
                                                     updateTime    : '2014-09-06T04:28:05Z', duration: 1000L, endTime:
                                                         '2014-09-06T04:28:05Z']
                                        ],
                                    parameterStates: [:],
                                    id             : '1',
                                    stepctx        : '1/1',
                                    nodeStep       : true,
                                    executionState : 'SUCCEEDED',
                                    startTime      : '2014-09-06T04:28:04Z',
                                    updateTime     : '2014-09-06T04:28:05Z',
                                    duration       : 2000L,
                                    endTime        : '2014-09-06T04:28:06Z'
                                ]
                            ]
                        ],
                        nodeStates     : [
                            localhost: [
                                executionState: 'SUCCEEDED',
                                startTime     : '2014-09-06T04:28:04Z',
                                updateTime    : '2014-09-06T04:28:06Z',
                                duration      : 2000L,
                                endTime       : '2014-09-06T04:28:06Z'
                            ]
                        ],
                        parameterStates: [:],
                        id             : '1',
                        stepctx        : '1',
                        nodeStep       : true,
                        executionState : 'SUCCEEDED',
                        startTime      : '2014-09-06T04:28:04Z',
                        updateTime     : '2014-09-06T04:28:04Z',
                        duration       : 2000L,
                        endTime        : '2014-09-06T04:28:06Z'
                    ]
                ]
            ]
    }

    def "summarize node should include step 1"() {
        given: 'node step has subworkflow error handler'
            def sut = new StateMapping()
            def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
            mutableStep1.nodeStep = true
            def state = new MutableWorkflowStateImpl(['localhost'], 1, [0: mutableStep1])
            MutableWorkflowStateImplTest.processStateChanges(state, STATE_CHANGES2)
        when: "summarize node state"
            def result = sut.mapOf(1, state)
            def summarized = sut.summarize(result, ['localhost'], true)
        then: "node has states for step 1 and 1/1"
            summarized.nodeSteps['localhost'].size() == 2
            summarized.nodeSteps['localhost'].find { it.stepctx == '1/1' } != null
            summarized.nodeSteps['localhost'].find { it.stepctx == '1' } != null
    }
}
