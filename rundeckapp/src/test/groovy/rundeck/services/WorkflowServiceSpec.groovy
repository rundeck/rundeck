/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package rundeck.services

import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowNodeStateImpl
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateImpl
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStepStateImpl
import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.Execution
import rundeck.services.workflow.StateMapping
import spock.lang.Specification

import static com.dtolabs.rundeck.core.execution.workflow.state.StateUtils.stepIdentifier

@TestFor(WorkflowService)
@Mock([Execution])
class WorkflowServiceSpec extends Specification{
    def setup() {
    }

    def cleanup() {
    }

    def "state mapping"(){
        given:
        StateMapping mapping = new StateMapping()

        def map = execMap()
        def nodes = ['nodea']
        when:
        def resp = mapping.summarize(map,nodes,true)

        then:
        resp
        resp.nodeSummaries
        resp.nodeSteps
        resp.nodeSteps.get('nodea')
        resp.nodeSteps.get('nodea').size()==5
    }


    private Map execMap(){
        def map = [executionId:2008,
                   nodes:[nodea:[[stepctx:'1', executionState:'SUCCEEDED'],
                                      [stepctx:'2/1', executionState:'WAITING'],
                                      [stepctx:'2/2', executionState:'WAITING'],
                                      [stepctx:'2/3', executionState:'WAITING'],
                                      [stepctx:'3', executionState:'WAITING']],
                          nodeb:[[stepctx:'2/1', executionState:'SUCCEEDED'],
                               [stepctx:'2/2', executionState:'RUNNING'],
                               [stepctx:'2/3', executionState:'WAITING'],
                               [stepctx:'2@node=nodea/1', executionState:'SUCCEEDED'],
                               [stepctx:'2@node=nodea/2', executionState:'RUNNING']]
                   ],
                   serverNode:'nodea',
                   executionState:'RUNNING',
                   completed:false,
                   targetNodes:['nodea'],
                   allNodes:['nodea', 'nodeb'],
                   stepCount:3,
                   updateTime:'2018-02-09T11:43:51Z',
                   startTime:'2018-02-09T11:43:45Z',
                   endTime:null,
                   steps:[
                           [nodeStates:
                                    [nodea:[
                                            executionState:'SUCCEEDED',
                                            startTime:'2018-02-09T11:43:46Z',
                                            updateTime:'2018-02-09T11:43:49Z',
                                            duration:3108,
                                            endTime:'2018-02-09T11:43:49Z']
                                    ],
                            parameterStates:[:],
                            id:1,
                            stepctx:1,
                            nodeStep:true,
                            executionState:'SUCCEEDED',
                            startTime:'2018-02-09T11:43:46Z',
                            updateTime:'2018-02-09T11:43:46Z',
                            duration:3356,
                            endTime:'2018-02-09T11:43:49Z'],
                           [hasSubworkflow:true,
                            workflow:[
                                    executionState:'RUNNING',
                                    completed:false,
                                    targetNodes:['nodeb', 'nodea'],
                                    allNodes:['nodeb', 'nodea'],
                                    stepCount:3,
                                    updateTime:'2018-02-09T11:43:51Z',
                                    startTime:'2018-02-09T11:43:50Z',
                                    endTime:null,
                                    steps:[
                                            [nodeStates:
                                                     [nodea:[executionState:'WAITING',
                                                                  startTime:null,
                                                                  updateTime:null,
                                                                  duration:-1,
                                                                  endTime:null],
                                                      nodeb:[executionState:'SUCCEEDED',
                                                           startTime:'2018-02-09T11:43:50Z',
                                                           updateTime:'2018-02-09T11:43:51Z',
                                                           duration:1620,
                                                           endTime:'2018-02-09T11:43:51Z']],
                                             parameterStates:[:],
                                             id:1,
                                             stepctx:'2/1',
                                             nodeStep:true,
                                             executionState:'RUNNING',
                                             startTime:'2018-02-09T11:43:50Z',
                                             updateTime:'2018-02-09T11:43:50Z',
                                             duration:0, endTime:null],
                                            [nodeStates:[
                                                    nodea:[executionState:'WAITING',
                                                                startTime:null,
                                                                updateTime:null,
                                                                duration:-1,
                                                                endTime:null],
                                                    nodeb:[executionState:'RUNNING',
                                                         startTime:'2018-02-09T11:43:51Z',
                                                         updateTime:'2018-02-09T11:43:51Z',
                                                         duration:0,
                                                         endTime:null]],
                                             parameterStates:[:],
                                             id:2,
                                             stepctx:'2/2',
                                             nodeStep:true,
                                             executionState:'RUNNING',
                                             startTime:'2018-02-09T11:43:51Z',
                                             updateTime:'2018-02-09T11:43:51Z',
                                             duration:0,
                                             endTime:null],
                                            [nodeStates:[
                                                    nodea:[executionState:'WAITING',
                                                                startTime:null,
                                                                updateTime:null,
                                                                duration:-1,
                                                                endTime:null],
                                                    nodeb:[executionState:'WAITING',
                                                         startTime:null,
                                                         updateTime:null,
                                                         duration:-1,
                                                         endTime:null]],
                                             parameterStates:[:],
                                             id:3,
                                             stepctx:'2/3',
                                             nodeStep:true,
                                             executionState:'WAITING',
                                             startTime:null,
                                             updateTime:null,
                                             duration:-1,
                                             endTime:null]]
                            ],
                            parameterStates:
                                    ['node=nodea':[
                                            hasSubworkflow:true,
                                            workflow:[
                                                    executionState:'RUNNING',
                                                    completed:false,
                                                    targetNodes:['nodeb', 'nodea'],
                                                    allNodes:['nodeb', 'nodea'],
                                                    stepCount:3,
                                                    updateTime:'2018-02-09T11:43:51Z',
                                                    startTime:'2018-02-09T11:43:50Z',
                                                    endTime:null,
                                                    steps:[
                                                            [nodeStates:
                                                                     [nodeb:
                                                                              [executionState:'SUCCEEDED',
                                                                               startTime:'2018-02-09T11:43:50Z',
                                                                               updateTime:'2018-02-09T11:43:51Z',
                                                                               duration:1620,
                                                                               endTime:'2018-02-09T11:43:51Z']
                                                                     ],
                                                             parameterStates:[:],
                                                             id:1,
                                                             stepctx:'2@node=nodea/1',
                                                             nodeStep:true,
                                                             executionState:'RUNNING',
                                                             startTime:'2018-02-09T11:43:50Z',
                                                             updateTime:'2018-02-09T11:43:50Z',
                                                             duration:0, endTime:null],
                                                            [nodeStates:[
                                                                    nodeb:[executionState:'RUNNING',
                                                                         startTime:'2018-02-09T11:43:51Z',
                                                                         updateTime:'2018-02-09T11:43:51Z',
                                                                         duration:0, endTime:null]],
                                                             parameterStates:[:],
                                                             id:2,
                                                             stepctx:'2@node=nodea/2',
                                                             nodeStep:true,
                                                             executionState:'RUNNING',
                                                             startTime:'2018-02-09T11:43:51Z',
                                                             updateTime:'2018-02-09T11:43:51Z',
                                                             duration:0,
                                                             endTime:null],
                                                            [parameterStates:[:],
                                                             id:3,
                                                             stepctx:'2@node=nodea/3',
                                                             nodeStep:false,
                                                             executionState:'WAITING',
                                                             startTime:null,
                                                             updateTime:null,
                                                             duration:-1,
                                                             endTime:null]]
                                            ],
                                            parameterStates:[:],
                                            parameters:[node:'nodea'],
                                            id:'2@node=nodea',
                                            stepctx:'2@node=nodea',
                                            nodeStep:false,
                                            executionState:'RUNNING',
                                            startTime:null,
                                            updateTime:null,
                                            duration:-1,
                                            endTime:null]
                                    ],
                            id:2,
                            stepctx:'2',
                            nodeStep:true,
                            executionState:'RUNNING',
                            startTime:'2018-02-09T11:43:49Z',
                            updateTime:'2018-02-09T11:43:49Z',
                            duration:0, endTime:null],
                           [nodeStates:
                                    [nodea:
                                             [executionState:'WAITING',
                                              startTime:null,
                                              updateTime:null,
                                              duration:-1,
                                              endTime:null]
                                    ],
                            parameterStates:[:],
                            id:3,
                            stepctx:3,
                            nodeStep:true,
                            executionState:'WAITING',
                            startTime:null,
                            updateTime:null,
                            duration:-1,
                            endTime:null]
                   ]
        ]
        return map
    }
}
