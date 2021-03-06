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
import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils
import com.dtolabs.rundeck.core.execution.workflow.state.StepContextId
import spock.lang.Specification
import spock.lang.Unroll

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

    /**
     * A workflow step (non-node-step) with subworkflow should be success after the workflow succeeds
     */
    def "workflow step subworkflow resolve"(){
        given:
        def date = new Date()

        def nodeA='a'
        def nodeB='b'

        //step 1, a subworkflow
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep1.nodeStep = false
        def mutableStep2 = new MutableWorkflowStepStateImpl(stepIdentifier(2))
        mutableStep2.nodeStep=false


        //top workflow runs on one node
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl([nodeA], 2, [0: mutableStep1,1:mutableStep2], null, nodeA);

        //workflow start
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, [nodeA])
        //step 1 start
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0,stepStateChange(stepState(ExecutionState.RUNNING)), date)

        //step1; sub workflow start
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, false, ExecutionState.RUNNING, date, [nodeA], null)

        //step 1 /1 start
        mutableWorkflowState.updateStateForStep(stepIdentifier(1,1), 0,stepStateChange(stepState(ExecutionState.RUNNING)), date)

        //step 1/1; start on node a
        mutableWorkflowState.updateStateForStep(stepIdentifier(1,1), 0, stepStateChange(stepState(ExecutionState.RUNNING), nodeA), date)
        //step 1/1: succeed on node a
        mutableWorkflowState.updateStateForStep(stepIdentifier(1,1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED), nodeA), date)
        //step 1 /1 end
        mutableWorkflowState.updateStateForStep(stepIdentifier(1,1), 0,stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)



        when:


        //step1; sub workflow end
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, false, ExecutionState.SUCCEEDED, date, [nodeA], null)
        //node step finishes for nodeC
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED),nodeA), date)


        then:

//        new StateMapping().mapOf(1L,mutableWorkflowState)==[:]

        mutableWorkflowState.stepStates[0].subWorkflowState.stepStates[0].nodeStateMap[nodeA].executionState.toString()=='SUCCEEDED'
        mutableWorkflowState.stepStates[0].subWorkflowState.executionState.toString()=='SUCCEEDED'
        mutableWorkflowState.stepStates[0].stepState.executionState.toString()=='SUCCEEDED'

    }

    def "update sub workflow state should update nodeset"(){
        given:
        def date = new Date()

        def nodeA='a'
        def nodeB='b'
        def nodeC='c'

        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep1.nodeStep = false
        def mutableStep2 = new MutableWorkflowStepStateImpl(stepIdentifierFromString("1/2@node=anode"))
        mutableStep2.nodeStep=false
        mutableStep2.mutableSubWorkflowState = new MutableWorkflowStateImpl([nodeA], 2)

        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl([nodeA], 2, [0: mutableStep1,1:mutableStep2], null, nodeA)

        when:
        mutableWorkflowState.updateSubWorkflowState(stepIdentifierFromString("1/2@node=anode"), 1, false, ExecutionState.SUCCEEDED, date, [nodeB, nodeC], null)

        then:
        mutableWorkflowState.stepStates[1].subWorkflowState.nodeSet == [nodeB, nodeC]

    }


    def "locateStepWithContext"() {
        given:
            def wf = new MutableWorkflowStateImpl([], 5)
            wf.mutableStepStates[2].getParameterizedStepState(stepIdentifierFromString('3@node=anode'), [node: 'anode'])
        when:
            def result = wf.locateStepWithContext(stepIdentifierFromString(stepId), ndx, ignore)
        then:
            result.stepIdentifier.toString() == expect
        where:
            stepId                 | ndx | ignore | expect
            '1'                    | 0   | false  | '1'
            '1/2/3/4/5'            | 0   | false  | '1'
            '1/2/3/4/5'            | 1   | false  | '2'
            '1/2/3/4/5'            | 2   | false  | '3'
            '1/2/3@node=anode/4/5' | 2   | false  | '3@node=anode'
            '1/2/3@node=anode/4/5' | 2   | true   | '3'
            '1/2/3/4/5'            | 3   | false  | '4'
            '1/2/3/4/5'            | 4   | false  | '5'
    }

    def "locateStepWithContext should update nodeSet"() {
        given:
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep1.nodeStep = false
        def mutableStep2 = new MutableWorkflowStepStateImpl(stepIdentifier(2))
        mutableStep2.nodeStep=false
        mutableStep2.mutableSubWorkflowState = new MutableWorkflowStateImpl(['nodeA'], 2)

        def wf = new MutableWorkflowStateImpl(['nodeA'], 2, [0: mutableStep1,1:mutableStep2], null, 'nodeA')
        when:
        def result = wf.locateStepWithContext(stepIdentifierFromString(stepId), 1, false, nodeNames)
        then:
        result.mutableSubWorkflowState.nodeSet == nodeSet
        where:
        stepId           | nodeNames          | nodeSet
        '1/2@node=anode' | ["nodeA", "nodeB"] | ["nodeA", "nodeB"]
        '1/2@node=anode' | null               | ["nodeA"]

    }
}
