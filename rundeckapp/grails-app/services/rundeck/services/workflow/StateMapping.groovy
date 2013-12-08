/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package rundeck.services.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils
import com.dtolabs.rundeck.core.execution.workflow.state.StepAspect
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStepState

import java.text.SimpleDateFormat

/**
 * read/write a generic Map from a WorkflowState
 */
class StateMapping {

    def Map mapOf(Long id, WorkflowState workflowState) {
        def nodestates = [:]
        def allNodes = []
        def map = mapOf(workflowState, null, nodestates, allNodes)
        map.allNodes = allNodes
        return [executionId: id, nodes: nodestates, serverNode: workflowState.serverNode] + map
    }

    def Map mapOf(WorkflowState workflowState, StepIdentifier parent = null, Map nodestates, List<String> allNodes) {
        allNodes.addAll(workflowState.allNodes.findAll { !allNodes.contains(it) })
        [
                executionState: workflowState.executionState.toString(),
                completed: workflowState.executionState.isCompletedState(),
                targetNodes: workflowState.nodeSet,
                allNodes: workflowState.allNodes,
                stepCount: workflowState.stepCount,
                timestamp: encodeDate(workflowState.timestamp),
                startTime: encodeDate(workflowState.startTime),
                endTime: encodeDate(workflowState.endTime),
                steps: workflowState.stepStates.collect { mapOf(it, parent, nodestates, allNodes) },
        ]
    }

    def Map mapOf(WorkflowStepState state, StepIdentifier parent = null, Map nodestates, List<String> allNodes) {
        def map = [:]
        if (state.hasSubWorkflow()) {
            StepIdentifier ident = parent ? StateUtils.stepIdentifier(parent.context + state.stepIdentifier.context) : state.stepIdentifier
            map += [
                    hasSubworkflow: state.hasSubWorkflow(),
                    workflow: mapOf(state.subWorkflowState, ident, nodestates, allNodes)
            ]
        }
        if (state.nodeStateMap) {
            def nmap = [:]
            state.nodeStateMap.each { String node, StepState nstate ->
                nmap[node] = mapOf(nstate)
                def list = [stepctx: stepctxToString(parent, state.stepIdentifier)] + simpleMapOf(nstate)
                if (!nodestates[node]) {
                    nodestates[node] = [list]
                } else {
                    nodestates[node].add(list)
                }
            }
            map += [nodeStates: nmap]
        }
        map + [
                id: stepIdentifierToString(state.stepIdentifier),
                nodeStep: state.nodeStep
        ] + mapOf(state.stepState)
    }

    def stepctxToString(StepIdentifier parent = null, StepIdentifier id) {
        stepIdentifierToString(parent ? StateUtils.stepIdentifier(parent.context + id.context) : id)
    }


    def String stepIdentifierToString(StepIdentifier ident) {
        ident.context.collect {
            it.step + (it.aspect == StepAspect.ErrorHandler ? 'e' : '')
        }.join("/")
    }


    def Map mapOf(StepState state) {
        [
                executionState: state.executionState.toString(),
                startTime: encodeDate(state.startTime),
                updateTime: encodeDate(state.updateTime),
                endTime: encodeDate(state.endTime),
        ] + (state.errorMessage ? [errorMessage: state.errorMessage] : [:]) + (state.metadata ? [meta: state.metadata] : [:])
    }

    def Map simpleMapOf(StepState state) {
        [executionState: state.executionState.toString(),]
    }

    static String encodeDate(Date date) {
        if (!date) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf.format(date)
    }

    static Date decodeDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf.parse(date)
    }



    def WorkflowState workflowStateFromMap(Map map) {
        ExecutionState state = ExecutionState.valueOf(map.executionState)
        List<String> nodes = map.targetNodes
        int stepCount = map.stepCount
        Date timestamp = map.timestamp ? decodeDate(map.timestamp) : null
        Date startTime = map.startTime ? decodeDate(map.startTime) : null
        Date endTime = map.endTime ? decodeDate(map.endTime) : null
        List stepStates = map.steps.collect {
            workflowStepStateFromMap(it)
        }
        String serverNode = map.serverNode ?: null;
        return StateUtils.workflowState(nodes, nodes, stepCount, state, timestamp, startTime, endTime, serverNode, stepStates, true)
    }

    WorkflowStepState workflowStepStateFromMap(Map map) {
        StepState state = stepStateFromMap(map)
        HashMap<String, StepState> nodeStateMap = null
        if (map.nodeStates) {
            nodeStateMap = new HashMap<String, StepState>()
            map.nodeStates.each { node, Map data ->
                nodeStateMap[node] = stepStateFromMap(data)
            }
        }
        WorkflowState subWorkflowState = null
        if (map.hasSubworkflow) {
            subWorkflowState = workflowStateFromMap(map.workflow)
        }
        List<String> nodeStepTargets = null
        if (map.stepTargetNodes) {
            nodeStepTargets = new ArrayList<String>(map.stepTargetNodes)
        }
        boolean nodeStep = !!map.nodeStep
        StateUtils.workflowStepState(state, nodeStateMap, stepIdentifierFromString(map.id), subWorkflowState, nodeStepTargets, nodeStep)
    }

    StepState stepStateFromMap(Map map) {
        Date updateTime = map.updateTime ? decodeDate(map.updateTime) : null
        Date startTime = map.startTime ? decodeDate(map.startTime) : null
        Date endTime = map.endTime ? decodeDate(map.endTime) : null
        return StateUtils.stepState(ExecutionState.valueOf(map.executionState), map.meta, map.errorMessage, startTime, updateTime, endTime)
    }

    def StepIdentifier stepIdentifierFromString(String string) {
        StateUtils.stepIdentifier(
                string.split(/\//).collect { s ->
                    StateUtils.stepContextId(Integer.parseInt(s.replaceAll(/e$/, '')), s.endsWith('e'))
                })
    }

}
