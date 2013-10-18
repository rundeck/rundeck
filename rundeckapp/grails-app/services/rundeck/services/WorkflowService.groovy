package rundeck.services

import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowState
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateImpl
import com.dtolabs.rundeck.app.internal.workflow.MutableWorkflowStateListener
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.state.EchoWFStateListener
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowExecutionStateListenerAdapter
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStateListener
import rundeck.Execution
import rundeck.Workflow

class WorkflowService {

    static transactional = false
    /**
     * in-memory states of executions,
     */
    static Map<Long,WorkflowState> workflowStates=new HashMap<Long, WorkflowState>()
    def MutableWorkflowState createStateForWorkflow(Workflow wf){
        MutableWorkflowState state = new MutableWorkflowStateImpl(null, wf.commands.size())
    }
    /**
     * Create and return a listener for changes to the workflow state for an execution
     * @param execution
     */
    def WorkflowExecutionListener createWorkflowStateListenerForExecution(Execution execution) {
        MutableWorkflowState state = createStateForWorkflow(execution.workflow)
        workflowStates.put(execution.id, state)
        def mutablestate= new MutableWorkflowStateListener(state)
        new WorkflowExecutionStateListenerAdapter([mutablestate,new EchoWFStateListener()])
    }
    /**
     * Read the workflow state for an execution
     * @param execution
     */
    def WorkflowState readWorkflowStateForExecution(Execution execution){
        //TODO: read state from elsewhere (db,network)
        return workflowStates[execution.id]
    }
}
