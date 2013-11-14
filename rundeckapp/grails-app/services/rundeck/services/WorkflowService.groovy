package rundeck.services

import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowState
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateImpl
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateListener
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStepStateImpl
import com.dtolabs.rundeck.app.internal.workflow.WorkflowStateListenerAction
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.state.*
import grails.converters.JSON
import grails.util.Environment
import rundeck.Execution
import rundeck.JobExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep

import java.text.SimpleDateFormat

class WorkflowService {

    static transactional = false
    /**
     * in-memory states of executions,
     */
    static Map<Long,WorkflowState> workflowStates=new HashMap<Long, WorkflowState>()
    def MutableWorkflowState createStateForWorkflow( Workflow wf, String project) {
        Map<Integer, MutableWorkflowStepStateImpl> substeps = [:]
        wf.commands.eachWithIndex { WorkflowStep step, int ndx ->
            if (step instanceof JobExec) {
                JobExec jexec = (JobExec) step
                def schedlist = ScheduledExecution.findAllScheduledExecutions(jexec.jobGroup, jexec.jobName, project)
                if (!schedlist || 1 != schedlist.size()) {
                    //skip
                    return
                }
                def id = schedlist[0].id

                ScheduledExecution se = ScheduledExecution.get(id)
                substeps[ndx] = new MutableWorkflowStepStateImpl(StateUtils.stepIdentifier(ndx+1),
                        createStateForWorkflow(se.workflow,project))
            }else{
                substeps[ndx]=new MutableWorkflowStepStateImpl(StateUtils.stepIdentifier(ndx + 1))
            }
            substeps[ndx].nodeStep = !!step.nodeStep
        }
        return new MutableWorkflowStateImpl(null, wf.commands.size(), substeps)
    }
    /**
     * Create and return a listener for changes to the workflow state for an execution
     * @param execution
     */
    def WorkflowExecutionListener createWorkflowStateListenerForExecution(Execution execution) {
        final long id=execution.id
        MutableWorkflowState state = createStateForWorkflow(execution.workflow,execution.project)
        workflowStates.put(execution.id, state)
        def mutablestate= new MutableWorkflowStateListener(state)
        def chain = [mutablestate]
        
        chain << new WorkflowStateListenerAction(onWorkflowExecutionStateChanged: {
                    ExecutionState executionState, Date timestamp, List<String> nodeSet ->
                        if (executionState.completedState) {
                            //workflow finished:
                            serializeState(id, state)
                        }
                })
        new WorkflowExecutionStateListenerAdapter(chain)
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
        def nodestates=[:]
        def map=mapOf(workflowState,null,nodestates)
        return [executionId: id, nodes: nodestates] + map
    }
    def Map mapOf(WorkflowState workflowState, StepIdentifier parent=null, Map nodestates) {
        [
                executionState:workflowState.executionState.toString(),
                completed: workflowState.executionState.isCompletedState(),
                targetNodes:workflowState.nodeSet,
                allNodes:workflowState.allNodes,
                stepCount:workflowState.stepCount,
                timestamp:encodeDate(workflowState.timestamp),
                startTime:encodeDate(workflowState.startTime),
                endTime:encodeDate(workflowState.endTime),
                steps:workflowState.stepStates.collect{mapOf(it,parent, nodestates)},
        ]
    }
    def String encodeDate(Date date){
        if(!date){
            return null;
        }
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
        List<String> nodes = map.targetNodes
        int stepCount = map.stepCount
        Date timestamp = map.timestamp?decodeDate(map.timestamp):null
        Date startTime = map.startTime?decodeDate(map.startTime):null
        Date endTime = map.endTime?decodeDate(map.endTime):null
        List stepStates = map.steps.collect {
            workflowStepStateFromMap(it)
        }
        return StateUtils.workflowState(nodes,nodes,stepCount,state,timestamp,startTime,endTime, stepStates,true)
    }

    def String stepIdentifierToString(StepIdentifier ident){
        ident.context.collect{
            it.step+(it.aspect==StepAspect.ErrorHandler?'e':'')
        }.join("/")
    }
    def StepIdentifier stepIdentifierFromString(String string){
        StateUtils.stepIdentifier(
        string.split(/\//).collect{s->
            StateUtils.stepContextId(Integer.parseInt(s.replaceAll(/e$/,'')), s.endsWith('e'))
        })
    }

    def stepctxToString(StepIdentifier parent = null, StepIdentifier id) {
        stepIdentifierToString(parent ? StateUtils.stepIdentifier(parent.context + id.context) : id)
    }

    def Map mapOf(WorkflowStepState state, StepIdentifier parent = null, Map nodestates){
        def map=[:]
        if(state.hasSubWorkflow()){
            StepIdentifier ident = parent?StateUtils.stepIdentifier(parent.context+state.stepIdentifier.context):state.stepIdentifier
            map+=[
                    hasSubworkflow: state.hasSubWorkflow(),
                    workflow:mapOf(state.subWorkflowState,ident, nodestates)
            ]
        }
        if(state.nodeStateMap){
            def nmap=[:]
            state.nodeStateMap.each {String node,StepState nstate->
                nmap[node]=mapOf(nstate)
                def list = [stepctx: stepctxToString(parent, state.stepIdentifier)] + simpleMapOf(nstate)
                if (!nodestates[node]) {
                    nodestates[node] = [list]
                } else {
                    nodestates[node].add(list)
                }
            }
            map += [nodeStates: nmap]
        }
        if(state.nodeStepTargets){
            map+=[
                    stepTargetNodes: state.nodeStepTargets,
            ]
        }
        map + [
                id: stepIdentifierToString(state.stepIdentifier),
                nodeStep:state.nodeStep
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
        List<String> nodeStepTargets=null
        if(map.stepTargetNodes){
            nodeStepTargets=new ArrayList<String>(map.stepTargetNodes)
        }
        boolean nodeStep = !!map.nodeStep
        StateUtils.workflowStepState(state,nodeStateMap,stepIdentifierFromString(map.id), subWorkflowState,nodeStepTargets,nodeStep)
    }


    def Map mapOf(StepState state){
        [
                executionState: state.executionState.toString(),
                startTime: encodeDate(state.startTime),
                updateTime: encodeDate(state.updateTime),
                endTime: encodeDate(state.endTime),
        ] + (state.errorMessage?[errorMessage:state.errorMessage]:[:]) + (state.metadata?[meta:state.metadata]:[:])
    }
    def Map simpleMapOf(StepState state){
        [executionState: state.executionState.toString(),]
    }

    StepState stepStateFromMap(Map map) {
        Date updateTime = map.updateTime ? decodeDate(map.updateTime) : null
        Date startTime = map.startTime ? decodeDate(map.startTime) : null
        Date endTime = map.endTime ? decodeDate(map.endTime) : null
        return StateUtils.stepState(ExecutionState.valueOf(map.executionState),map.meta,map.errorMessage,startTime,updateTime,endTime)
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
    def WorkflowState previewWorkflowStateForExecution(Execution execution){
        createStateForWorkflow(execution.workflow, execution.project)
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
