package rundeck.services

import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowState
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateImpl
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateListener
import com.dtolabs.rundeck.app.internal.workflow.WorkflowStateListenerAction
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.state.*
import grails.converters.JSON
import rundeck.Execution
import rundeck.Workflow

import java.text.SimpleDateFormat

class WorkflowService {

    static transactional = false
    /**
     * in-memory states of executions,
     */
    static Map<Long,WorkflowState> workflowStates=new HashMap<Long, WorkflowState>()
    def MutableWorkflowState createStateForWorkflow(Workflow wf){
        new MutableWorkflowStateImpl(null, wf.commands.size())
    }
    /**
     * Create and return a listener for changes to the workflow state for an execution
     * @param execution
     */
    def WorkflowExecutionListener createWorkflowStateListenerForExecution(Execution execution) {
        final long id=execution.id
        MutableWorkflowState state = createStateForWorkflow(execution.workflow)
        workflowStates.put(execution.id, state)
        def mutablestate= new MutableWorkflowStateListener(state)
        new WorkflowExecutionStateListenerAdapter([mutablestate, new WorkflowStateListenerAction(onWorkflowExecutionStateChanged: {
            ExecutionState executionState, Date timestamp, Set<String> nodeSet->
            if(executionState.completedState){
                //workflow finished:
                serializeState(id, state)
            }
        })])
    }
    def File serializeState(Long id,WorkflowState state){
        File file= new File("/tmp/${id}.json")
        file.withWriter { w->
            w << mapOf(id,state).encodeAsJSON()
        }
        return file
    }
    def Map deserializeState(Long id){
        File file = new File("/tmp/${id}.json")
        if(file.canRead()){
            return JSON.parse(file.text)
        }
        return null
    }

    private WorkflowState loadState(long id) {
        def Map map = deserializeState(id)
        return map?workflowStateFromMap(map):null
    }

    def Map mapOf(Long id,WorkflowState workflowState) {
        [executionId:id] + mapOf(workflowState)
    }
    def Map mapOf(WorkflowState workflowState) {
        [
                executionState:workflowState.executionState.toString(),
                targetNodes:workflowState.nodeSet,
                stepCount:workflowState.stepCount,
                timestamp:encodeDate(workflowState.timestamp),
                steps:workflowState.stepStates.collect{mapOf(it)},
        ]
    }
    def String encodeDate(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf.format(date)
    }
    def Date decodeDate(String date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf.parse(date)
    }

    def WorkflowState workflowStateFromMap(Map map) {
        ExecutionState state = ExecutionState.valueOf(map.executionState)
        Set<String> nodes = map.targetNodes
        int stepCount = map.stepCount
        Date timestamp = decodeDate(map.timestamp)

        return StateUtils.workflowState(nodes,stepCount,state,timestamp,map.steps.collect{
            workflowStepStateFromMap(it)
        })
    }


    def Map mapOf(WorkflowStepState state){
        def map=[:]
        if(state.hasSubWorkflow()){
            map+=[
                    hasSubworkflow: !!state.hasSubWorkflow(),
                    workflow:mapOf(state.subWorkflowState)
            ]
        }
        if(state.nodeStateMap){
            def nmap=[:]
            state.nodeStateMap.each {String node,StepState nstate->
                nmap[node]=mapOf(nstate)
            }
            map += [nodeStates: nmap]
        }
        if(state.nodeStepTargets){
            map+=[
                    stepTargetNodes: state.nodeStepTargets,
            ]
        }
        map + [
                id:state.stepIdentifier.context.head(),
        ] + mapOf(state.stepState)
    }

    WorkflowStepState workflowStepStateFromMap(Map map) {
        StepState state = stepStateFromMap(map)
        HashMap<String,StepState> nodeStateMap=null
        if(map.nodeStates){
            nodeStateMap=new HashMap<String, StepState>()
            map.nodeStates.each{node,Map data->
                nodeStateMap[node]=stepStateFromMap(data)
            }
        }
        WorkflowState subWorkflowState = null
        if(map.hasSubworkflow){
            subWorkflowState=workflowStateFromMap(map.workflow)
        }
        Set<String> nodeStepTargets=null
        if(map.stepTargetNodes){
            nodeStepTargets=new HashSet<String>(map.stepTargetNodes)
        }
        StateUtils.workflowStepState(state,nodeStateMap,StateUtils.stepIdentifier(map.id), subWorkflowState,nodeStepTargets)
    }


    def Map mapOf(StepState state){
        [executionState: state.executionState.toString()] +
                (state.errorMessage?[errorMessage:state.errorMessage]:[:]) +
                (state.metadata?[meta:state.metadata]:[:])
    }

    StepState stepStateFromMap(Map map) {
        return StateUtils.stepState(ExecutionState.valueOf(map.executionState),map.meta,map.errorMessage)
    }
/**
     * Read the workflow state for an execution
     * @param execution
     */
    def WorkflowState readWorkflowStateForExecution(Execution execution){
        //TODO: read state from elsewhere (db,network)
        def state = workflowStates[execution.id]
        if(state){
            return state
        }else{
            return loadState(execution.id)
        }
    }


    def Map serializeWorkflowStateForExecution(Execution execution){
        def state = readWorkflowStateForExecution(execution)
        if(state){
            return mapOf(execution.id,state)
        }else{
            return [error:'unavailable']
        }
    }
}
