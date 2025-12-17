package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.execution.ExecutionReference;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;

import java.util.Map;

/**
 * Describes the job or execution life cycle event.
 * @author rnavarro
 * @since 5/07/19
 */
public interface JobExecutionEvent extends JobEvent {

    /**
     *
     * @return String project name.
     */
    String getProjectName();
    /**
     *
     * @return StepExecutionContext of the event.
     */
    StepExecutionContext getExecutionContext();

    /**
     *
     * @return Map<String, String> options of the job.
     */
    Map<String, String> getOptions();

    /**
     *
     * @return ExecutionLogger logger of the job.
     */
    ExecutionLogger getExecutionLogger();

    /**
     *
     * @return String user name triggering the job.
     */
    String getUserName();

    /**
     *
     * @return String job execution id.
     */
    String getExecutionId();
    /**
     *
     * @return INodeSet node set where the job will run
     */
    INodeSet getNodes();

    /**
     * @return reference to the execution
     */
    ExecutionReference getExecution();

    /**
     * @return details of the workflow
     */
    WorkflowExecutionItem getWorkflow();

    /**
     *
     * @return
     */
    JobEventResult getResult();

}
