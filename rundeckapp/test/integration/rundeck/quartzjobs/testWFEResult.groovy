package rundeck.quartzjobs

import com.dtolabs.rundeck.core.execution.workflow.ControlBehavior
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult

/**
 * Created by greg on 2/9/15.
 */
class TestWFEResult implements WorkflowExecutionResult {

    List<StepExecutionResult> resultSet
    Map<String, Collection<StepExecutionResult>> nodeFailures
    Map<Integer, StepExecutionResult> stepFailures
    Exception exception
    boolean success
    String statusString
    ControlBehavior controlBehavior


    @Override
    public List<StepExecutionResult> getResultSet() {
        return resultSet;
    }

    @Override
    public Map<String, Collection<StepExecutionResult>> getNodeFailures() {
        return nodeFailures;
    }

    @Override
    public Map<Integer, StepExecutionResult> getStepFailures() {
        return stepFailures;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }


}
