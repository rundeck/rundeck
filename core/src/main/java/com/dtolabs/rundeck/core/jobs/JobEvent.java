package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;

import java.util.Map;

/**
 * Describes the job life cycle event.
 * Created by rnavarro
 * Date: 5/07/19
 */

public interface JobEvent {

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

}