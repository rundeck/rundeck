package rundeck.services

import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStateListener
import rundeck.Execution

class WorkflowService {

    static transactional = true
    /**
     * in-memory states of executions,
     */
    static Map<Long,Object> workflowStates=new HashMap<Long, Object>()
    /**
     * Create and return a listener for changes to the workflow state for an execution
     * @param execution
     */
    def WorkflowStateListener createWorkflowStateListenerForExecution(Execution execution) {


    }
    /**
     * Read the workflow state for an execution
     * @param execution
     */
    def readWorkflowStateForExecution(Execution execution){

    }
}
