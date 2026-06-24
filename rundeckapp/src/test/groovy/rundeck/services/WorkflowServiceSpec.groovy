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


import com.dtolabs.rundeck.core.execution.PluginNodeStepExecutionItemImpl
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import com.dtolabs.rundeck.core.execution.workflow.WorkflowImpl
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStateDataLoader
import com.dtolabs.rundeck.core.jobs.JobReferenceItem
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import rundeck.CommandExec
import rundeck.Execution
import rundeck.JobExec
import rundeck.Workflow
import rundeck.services.workflow.StateMapping
import rundeck.services.logging.WorkflowStateFileLoader
import spock.lang.Specification

class WorkflowServiceSpec extends Specification implements ServiceUnitTest<WorkflowService>, DataTest {

    def setupSpec() {
        mockDomains Workflow,Execution,CommandExec
    }

    def "requestState fulfilled with stream source"() {
        setup:
        service.workflowStateDataLoader = Mock(WorkflowStateDataLoader)
        service.configurationService = Mock(ConfigurationService) {
            getString("workflowService.stateCache.spec",_) >> "maximumSize=5,expireAfterAccess=60s"
        }
        service.initialize()
        def e = new Execution(uuid:'1234')
        e.setId(1L)
        def efl = new WorkflowStateFileLoader(
                state: ExecutionFileState.AVAILABLE,
                stream: new ByteArrayInputStream('{"state":"statehere"}'.bytes),
        )

        when:
        def actual = service.requestState(e)

        then:
        1 * service.workflowStateDataLoader.loadWorkflowStateData(_, _) >> efl
        actual.workflowState == ['state':'statehere']
        actual.state == ExecutionFileState.AVAILABLE
        !actual.file
    }

    def "scheduled execution for the exec job is retrieved from the exec job"() {
        setup:
        final jobProject = 'proj1'
        def jobExecMock = Mock(JobReferenceItem)
        service.scheduledExecutionService = Mock(ScheduledExecutionService)

        WorkflowImpl workflow = new WorkflowImpl( [jobExecMock], 0, false, "")

        when:
        service.createStateForWorkflow(workflow, jobProject, "dummyFrameworkNodeName", null, null)

        then:
        1 * service.scheduledExecutionService.findJobFromJobReference(_,jobProject)
    }

    def "createStateForWorkflow groups flattened conditional sub-steps under a single parent slot"() {
        given:
        // Simulate the output of ExecutionUtilService.consolidateWorkflowSteps for a job with
        // [step1, conditional(sub1, sub2), step3]: a flat 4-item list where sub1 and sub2 carry
        // the parent/sub-step indices that should produce a hierarchical state tree.
        def step1 = new PluginNodeStepExecutionItemImpl('plugin', [:], false, null, 'step1', null)

        def sub1 = new PluginNodeStepExecutionItemImpl('plugin', [:], false, null, 'sub1', null)
        sub1.parentStepNumber = 2
        sub1.subStepNumber = 1

        def sub2 = new PluginNodeStepExecutionItemImpl('plugin', [:], false, null, 'sub2', null)
        sub2.parentStepNumber = 2
        sub2.subStepNumber = 2

        def step3 = new PluginNodeStepExecutionItemImpl('plugin', [:], false, null, 'step3', null)

        WorkflowImpl workflow = new WorkflowImpl([step1, sub1, sub2, step3], 0, false, "")

        when:
        def state = service.createStateForWorkflow(workflow, 'proj1', 'frameworkNode', null, null)

        then:
        // Three logical slots (not four) — the conditional collapses sub1+sub2 into one slot.
        state.stepCount == 3
        state.mutableStepStates.size() == 3
        // Slot 1 (index 0): step1 - plain step, no sub-workflow
        !state.mutableStepStates[0].hasSubWorkflow()
        state.mutableStepStates[0].stepIdentifier.context*.step == [1]
        // Slot 2 (index 1): conditional group at logical step 2, has sub-workflow
        state.mutableStepStates[1].hasSubWorkflow()
        state.mutableStepStates[1].stepIdentifier.context*.step == [2]
        def innerWf = state.mutableStepStates[1].mutableSubWorkflowState
        innerWf.stepCount == 2
        // Inner sub-workflow steps use LOCAL 1-based identifiers ([1], [2]).
        // The serializer concatenates the outer parent context to produce stepctx "2/1", "2/2".
        innerWf.mutableStepStates[0].stepIdentifier.context*.step == [1]
        innerWf.mutableStepStates[1].stepIdentifier.context*.step == [2]
        // Slot 3: step3 lives at logical step 3, NOT at flat step 4
        !state.mutableStepStates[2].hasSubWorkflow()
        state.mutableStepStates[2].stepIdentifier.context*.step == [3]
    }

    def "createStateForWorkflow handles two consecutive conditional groups"() {
        given:
        def c1Sub1 = new PluginNodeStepExecutionItemImpl('plugin', [:], false, null, 'c1-a', null)
        c1Sub1.parentStepNumber = 1
        c1Sub1.subStepNumber = 1

        def c2Sub1 = new PluginNodeStepExecutionItemImpl('plugin', [:], false, null, 'c2-a', null)
        c2Sub1.parentStepNumber = 2
        c2Sub1.subStepNumber = 1

        def c2Sub2 = new PluginNodeStepExecutionItemImpl('plugin', [:], false, null, 'c2-b', null)
        c2Sub2.parentStepNumber = 2
        c2Sub2.subStepNumber = 2

        WorkflowImpl workflow = new WorkflowImpl([c1Sub1, c2Sub1, c2Sub2], 0, false, "")

        when:
        def state = service.createStateForWorkflow(workflow, 'proj1', 'frameworkNode', null, null)

        then:
        state.stepCount == 2
        state.mutableStepStates[0].hasSubWorkflow()
        state.mutableStepStates[0].mutableSubWorkflowState.stepCount == 1
        state.mutableStepStates[1].hasSubWorkflow()
        state.mutableStepStates[1].mutableSubWorkflowState.stepCount == 2
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

    def "createStateForWorkflow correctly groups mixed conditional with nested and direct substeps"() {
        given: "A conditional with both a nested conditional and a direct substep"
        // Job structure:
        // Step 1: regular command
        // Step 2: Conditional (top-level)
        //   SubStep 1: Nested Conditional
        //     Nested SubStep 1: command in nested conditional
        //   SubStep 2: Direct command (not in nested conditional)
        // Step 3: regular command

        def step1 = new PluginNodeStepExecutionItemImpl('plugin', [:], false, null, 'step1', null)

        // SubStep 1 of Step 2: Nested conditional (has parentStepPath)
        def nestedSub1 = new PluginNodeStepExecutionItemImpl('plugin', [:], false, null, 'nestedSub1', null)
        nestedSub1.parentStepNumber = 2
        nestedSub1.subStepNumber = 1
        nestedSub1.parentStepPath = [2, 1]  // Path: step 2, substep 1

        // SubStep 2 of Step 2: Direct substep (no parentStepPath)
        def directSub2 = new PluginNodeStepExecutionItemImpl('plugin', [:], false, null, 'directSub2', null)
        directSub2.parentStepNumber = 2
        directSub2.subStepNumber = 2
        directSub2.parentStepPath = null  // Direct substep of step 2

        def step3 = new PluginNodeStepExecutionItemImpl('plugin', [:], false, null, 'step3', null)

        WorkflowImpl workflow = new WorkflowImpl([step1, nestedSub1, directSub2, step3], 0, false, "")

        when:
        def state = service.createStateForWorkflow(workflow, 'proj1', 'frameworkNode', null, null)

        then: "The state tree has exactly 3 logical steps"
        state.stepCount == 3
        state.mutableStepStates.size() == 3

        and: "Step 1 is a regular step"
        !state.mutableStepStates[0].hasSubWorkflow()
        state.mutableStepStates[0].stepIdentifier.context*.step == [1]

        and: "Step 2 is a conditional containing both substeps"
        state.mutableStepStates[1].hasSubWorkflow()
        state.mutableStepStates[1].stepIdentifier.context*.step == [2]
        def innerWf = state.mutableStepStates[1].mutableSubWorkflowState
        innerWf.stepCount == 2

        and: "SubStep 1 of Step 2 is the nested conditional"
        innerWf.mutableStepStates[0].hasSubWorkflow()
        innerWf.mutableStepStates[0].stepIdentifier.context*.step == [1]
        def nestedWf = innerWf.mutableStepStates[0].mutableSubWorkflowState
        nestedWf.stepCount == 1

        and: "SubStep 2 of Step 2 is the direct command"
        !innerWf.mutableStepStates[1].hasSubWorkflow()
        innerWf.mutableStepStates[1].stepIdentifier.context*.step == [2]

        and: "Step 3 is a regular step"
        !state.mutableStepStates[2].hasSubWorkflow()
        state.mutableStepStates[2].stepIdentifier.context*.step == [3]
    }
}
