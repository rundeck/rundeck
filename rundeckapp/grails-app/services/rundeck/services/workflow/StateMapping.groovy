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
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStepState

import java.text.SimpleDateFormat

/**
 * read/write a generic Map from a WorkflowState
 */
class StateMapping {

    def Map summarize(Map map,List<String> nodes,boolean selectedOnly){
        def nodeSummaries=[:]
        def nodeSteps=[:]
        (selectedOnly?nodes:map.allNodes).each{node->

            def states = stepStatesForNode(map, node)
            nodeSummaries[node]=summarizeForNode(map,node, states)
            if(nodes.contains(node)){
                nodeSteps[node]=states
            }
        }
        map.nodeSummaries=nodeSummaries
        map.nodeSteps=nodeSteps
        map.steps=[]
        map.remove('nodes')
        map.remove('targetNodes')
        map
    }
    def boolean stateCompare(a,b){
        if (a == b) {
            return false
        }
        def states = ['SUCCEEDED', 'NONE','NOT_STARTED', 'WAITING', 'FAILED', 'ABORTED', 'RUNNING', 'RUNNING_HANDLER'];
        def ca = states.indexOf(a);
        def cb = states.indexOf(b);
        if (ca < 0) {
            return true;
        }
        return cb > ca
    }
    def Map summarizeForNode(Map map,String node, List steps){
        def summary=[:]
        def currentStep=[:];

        //step summary info
        def summarydata = [
            total: 0,
            SUCCEEDED: 0,
            FAILED: 0,
            WAITING: 0,
            NOT_STARTED: 0,
            RUNNING: 0,
            RUNNING_HANDLER: 0,
            other: 0,
            duration_ms_total: 0,
        ]

        Date updated=null;
        def testStates= ['SUCCEEDED', 'FAILED', 'WAITING', 'NOT_STARTED', 'RUNNING', 'RUNNING_HANDLER'];
        def duration=-1;
        Date dateStarted=null;
        steps.each{step->
            def z = step.executionState;
            if(step.stepctx.indexOf('@')<0) {
                summarydata.total++
                if (testStates.indexOf(z) >= 0 && null != summarydata[z]) {
                    summarydata[z]++;
                } else {
                    summarydata['other']++;
                }
            }
            if (!currentStep && stateCompare('NONE', step.executionState)
                    || currentStep && stateCompare(currentStep.executionState, step.executionState)) {
                currentStep.putAll(step);
            }
            def started=step.startTime?decodeDate(step.startTime):null
            if(!dateStarted || (started && started<dateStarted)){
                dateStarted=started;
            }
            def lastUpdated=lastUpdatedFor(step)
            if(!updated || lastUpdated>updated){
                updated=lastUpdated
            }
        }
        if (updated && dateStarted) {
            duration = updated.time - dateStarted.time
        }
        summary.duration=duration
        if(currentStep){
            summary.currentStep=currentStep
        }
        summary.lastUpdated=encodeDate(updated)

        //based on step states set the summary for this node
        if (summarydata.total > 0) {
            if (summarydata.RUNNING > 0) {
                summary.summaryState=("RUNNING");
            } else if (summarydata.RUNNING_HANDLER > 0) {
                summary.summaryState=("RUNNING_HANDLER");
            } else if (summarydata.total == summarydata.SUCCEEDED && summarydata.pending < 1) {
                summary.summaryState=("SUCCEEDED");
            } else if (summarydata.FAILED > 0) {
                summary.FAILED=summarydata.FAILED;
                summary.summaryState=("FAILED");
            } else if (summarydata.WAITING > 0) {
                summary.WAITING=summarydata.WAITING;
                summary.summaryState=("WAITING");
            } else if (summarydata.NOT_STARTED == summarydata.total && summarydata.pending < 1) {
                summary.summaryState=("NOT_STARTED");
            } else if (summarydata.NOT_STARTED > 0) {
                summary.PARTIAL_NOT_STARTED=summarydata.NOT_STARTED;
                summary.summaryState=("PARTIAL_NOT_STARTED");
            }else if(summarydata.pending > 0 ){
                summary.summaryState=("WAITING");
            } else if (summarydata.SUCCEEDED > 0) {
                summary.PARTIAL_SUCCEEDED=summarydata.total - summarydata.SUCCEEDED;
                summary.summaryState=("PARTIAL_SUCCEEDED");
            } else {
                summary.summaryState=("NONE_SUCCEEDED");
            }
        } else if(summarydata.pending > 0){
            summary.summaryState=("WAITING");
        } else {
            summary.summaryState=("NONE");
        }
        summary
    }
    def long durationForStep(Map step){
        if(step.duration){
            return step.duration
        }else if(step.startTime && (step.endTime || step.updateTime)){
            Date start=decodeDate(step.startTime)
            Date end=step.endTime?decodeDate(step.endTime):null
            Date update=step.updateTime?decodeDate(step.updateTime):null
            return (end!=null?end.time:update.time)-start.time
        }else{
            -1
        }
    }
    def Date lastUpdatedFor(Map step){
        if(step.endTime || step.updateTime){
            long end=step.endTime?decodeDate(step.endTime).time:0
            long update=step.updateTime?decodeDate(step.updateTime).time:0
            return new Date(Math.max(end,update))
        }else{
            null
        }
    }
    def String pluralize(int amount, String singular, String plural=null){
        return amount==1?singular: plural?:singular+'s'
    }
    def List stepStatesForNode(Map map,String node){
        def newsteps=[]
        def steps = map.nodes[node]
        steps.each{step->
            def stepStateForCtx = stepStateForCtx(map, StateUtils.stepIdentifierFromString(step.stepctx),node)
            if(stepStateForCtx) {
                def found = stepStateForCtx.nodeStates?.get(node)
                if (found) {
                    def newfound=new HashMap(found)
                    newfound.stepctx = step.stepctx
                    newsteps.push(newfound)
                }
            }
        }
        newsteps
    }

    def Map stepStateForCtx(Map model,StepIdentifier stepctx,String node=null){

        def stepid = stepctx.context[0]

        def ndx = stepid.step - 1
        def params = stepid.params?StateUtils.parameterString(stepid.params):''

        Map step = model.steps[ndx];
        if(params && step.parameterStates && step.parameterStates[params]){
            step = step.parameterStates[params];
        }else if(!params && node && step.parameterStates["node=${node}"]){
            step = step.parameterStates["node=${node}"];
        }
        if (stepctx.context.size()>1 && step.workflow) {
            return stepStateForCtx(step.workflow, StateUtils.stepIdentifierTail(stepctx))
        } else {
            return step
        }
    }
    def Map mapOf(Long id, WorkflowState workflowState) {
        def nodestates = [:]
        def allNodes = []
        def map = mapOf(workflowState, null, nodestates, allNodes)
        map.allNodes = allNodes
        return [executionId: id, nodes: nodestates, serverNode: workflowState.serverNode] + map
    }

    /**
     * Return a map containing:
     *
     * @param workflowState
     * @param parent
     * @param nodestates
     * @param allNodes
     * @return
     */
    def Map mapOf(WorkflowState workflowState, StepIdentifier parent = null, Map nodestates, List<String> allNodes) {
        allNodes.addAll(workflowState.allNodes.findAll { !allNodes.contains(it) })
        [
                executionState: workflowState.executionState.toString(),
                completed: workflowState.executionState.isCompletedState(),
                targetNodes: workflowState.nodeSet,
                allNodes: workflowState.allNodes,
                stepCount: workflowState.stepCount,
                updateTime: encodeDate(workflowState.updateTime),
                startTime: encodeDate(workflowState.startTime),
                endTime: encodeDate(workflowState.endTime),
                steps: workflowState.stepStates.collect { mapOf(it, parent, nodestates, allNodes) },
        ]
    }

    def Map mapOf(WorkflowStepState state, StepIdentifier parent = null, Map nodestates, List<String> allNodes) {
        def map = [:]
        StepIdentifier ident = parent ? StateUtils.stepIdentifier(parent.context + state.stepIdentifier.context) :
            state.stepIdentifier
        if (state.hasSubWorkflow()) {
            map += [
                    hasSubworkflow: state.hasSubWorkflow(),
                    workflow: mapOf(state.subWorkflowState, ident, nodestates, allNodes)
            ]
        } else if (state.nodeStateMap) {
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
        if (null != state.getParameterizedStateMap()) {
            def map1 = state.getParameterizedStateMap()
            def sub1=[:]
            def keys = new ArrayList(map1.keySet())
            keys.each{k->
                sub1[k]= mapOf(map1[k], parent, nodestates, allNodes)
            }
            map+=[
                    parameterStates:sub1
            ]
        }
        if(state.stepIdentifier.context[0].params){
            map+=[
                    parameters: state.stepIdentifier.context[0].params
            ]
        }
        map + [
                id: stepIdentifierToString(state.stepIdentifier),
                stepctx: stepIdentifierToString(ident),
                nodeStep: state.nodeStep
        ] + mapOf(state.stepState)
    }

    def stepctxToString(StepIdentifier parent = null, StepIdentifier id) {
        stepIdentifierToString(parent ? StateUtils.stepIdentifier(parent.context + id.context) : id)
    }


    def String stepIdentifierToString(StepIdentifier ident) {
        StateUtils.stepIdentifierToString(ident)
    }

    def long longDuration(Date start, Date update, Date end){
        if(end!=null && start!=null){
            end.time-start.time
        }else if(update!=null && start!=null){
            update.time-start.time
        }else{
            -1
        }
    }
    def Map mapOf(StepState state) {
        [
                executionState: state.executionState.toString(),
                startTime: encodeDate(state.startTime),
                updateTime: encodeDate(state.updateTime),
                duration:longDuration(state.startTime,state.updateTime,state.endTime),
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
        Date updateTime = map.updateTime ? decodeDate(map.updateTime) : null
        Date startTime = map.startTime ? decodeDate(map.startTime) : null
        Date endTime = map.endTime ? decodeDate(map.endTime) : null
        List stepStates = map.steps.collect {
            workflowStepStateFromMap(it)
        }
        String serverNode = map.serverNode ?: null;
        return StateUtils.workflowState(nodes, nodes, stepCount, state, updateTime, startTime, endTime, serverNode, stepStates, true)
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
        StateUtils.stepIdentifierFromString(string)
    }

}
