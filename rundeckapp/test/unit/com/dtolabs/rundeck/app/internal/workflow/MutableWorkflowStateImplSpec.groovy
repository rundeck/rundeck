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

package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepContextId
import spock.lang.Specification

import static com.dtolabs.rundeck.core.execution.workflow.state.StateUtils.*

class MutableWorkflowStateImplSpec extends Specification {
    /**
     *
     * @return
     */
    def "node B should not be ABORTED for parameterized subworkflow error handler after node A finishes"(){
        given:
        def date = new Date()

        def nodeA='a'
        def nodeB='b'

        //step 1, both a node step and a subworkflow
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep1.nodeStep = true


        //top workflow runs on two nodes
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl([nodeA,nodeB], 1, [0: mutableStep1], null, nodeA);

        //workflow start
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, [nodeA,nodeB])
        //step 1 start
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0,stepStateChange(stepState(ExecutionState.RUNNING)), date)

        StepContextId ctx1e = stepContextId(1, true)
        StepContextId ctx1eA = stepContextId(1, true,[node:nodeA])
        StepContextId ctx1eb = stepContextId(1, true, [node: nodeB])
        //step 1; start on node a
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.RUNNING), nodeA), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.RUNNING), nodeB), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.FAILED), nodeA), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.FAILED), nodeB), date)

        //handler on nodeA
        mutableWorkflowState.updateStateForStep(stepIdentifier([ctx1e]), 0, stepStateChange(stepState(ExecutionState.RUNNING_HANDLER),nodeA), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier([ctx1e]), 0, stepStateChange(stepState(ExecutionState.RUNNING_HANDLER),nodeB), date)

        //begin subworkflow
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier([ctx1e]), 0, false,ExecutionState.RUNNING, date,[nodeA],null)
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier([ctx1e]), 0, false,ExecutionState.RUNNING, date,[nodeB],null)

        StepContextId ctx1 = stepContextId(1, false, [node: nodeA])
        StepContextId ctx2 = stepContextId(1, false)

        def paramStepId_1eA1=stepIdentifier([ctx1eA, ctx2])
        def paramStepId_1eB1=stepIdentifier([ctx1eb, ctx2])

        //subworkflow errorhandler on nodeA
        mutableWorkflowState.updateStateForStep(paramStepId_1eA1, 0, stepStateChange(stepState(ExecutionState.RUNNING)), date)
        mutableWorkflowState.updateStateForStep(paramStepId_1eB1, 0, stepStateChange(stepState(ExecutionState.RUNNING)), date)
        mutableWorkflowState.updateStateForStep(paramStepId_1eA1, 0, stepStateChange(stepState(ExecutionState.RUNNING),nodeA), date)
        mutableWorkflowState.updateStateForStep(paramStepId_1eB1, 0, stepStateChange(stepState(ExecutionState.RUNNING),nodeB), date)



        when:

        //node step finishes for nodeC
        mutableWorkflowState.updateStateForStep(paramStepId_1eA1, 0, stepStateChange(stepState(ExecutionState.SUCCEEDED),nodeA), date)


        then:

//        new StateMapping().mapOf(1L,mutableWorkflowState)==[:]

        mutableWorkflowState.stepStates[0].subWorkflowState.stepStates[0].nodeStateMap[nodeA].executionState.toString()=='SUCCEEDED'
        //node step for nodeB should not be finished
        mutableWorkflowState.stepStates[0].subWorkflowState.stepStates[0].nodeStateMap[nodeB].executionState.toString()=='RUNNING'

    }
    def "captured state test of above"(){
        given:
        def date = new Date()

        def nodeA='node-0'
        def nodeB='node-1'
        def serverNode='node-X'

        //step 1, both a node step and a subworkflow
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep1.nodeStep = true

        //top workflow runs on two nodes
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl([nodeA,nodeB], 1, [0: mutableStep1], null, serverNode);

        def changes=MutableWorkflowStateImplTest.loadJson("state3.json")
        changes=changes[0..<progress]

        when:

        //node step finishes for nodeC
        MutableWorkflowStateImplTest.processStateChanges(mutableWorkflowState, changes)

        then:

        //node step for nodeB should not be finished
        mutableWorkflowState.stepStates[0].subWorkflowState.stepStates[0].nodeStateMap[nodeA].executionState.toString()==astate
        mutableWorkflowState.stepStates[0].subWorkflowState.stepStates[0].nodeStateMap[nodeB].executionState.toString()==bstate
//        new StateMapping().mapOf(1L,mutableWorkflowState)==[:]

        where:
        progress | astate      | bstate
        18       | 'SUCCEEDED' | 'RUNNING'
        19       | 'SUCCEEDED' | 'SUCCEEDED'

    }


}
