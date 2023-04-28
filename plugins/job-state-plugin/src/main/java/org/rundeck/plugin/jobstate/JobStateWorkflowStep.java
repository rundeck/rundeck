/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package org.rundeck.plugin.jobstate;

import com.dtolabs.rundeck.core.dispatcher.ExecutionState;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.jobs.JobNotFound;
import com.dtolabs.rundeck.core.jobs.JobReference;
import com.dtolabs.rundeck.core.jobs.JobService;
import com.dtolabs.rundeck.core.jobs.JobState;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ExecutionEnvironmentConstants;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginMetadata;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;

import java.util.Map;

/**
 */
@Plugin(name = JobStateWorkflowStep.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = "Job State Conditional",
                   description =
                       "Assert that another Job is or is not running or the state of its most recent execution.\n"
                       + "\n"
                       + "Select a value for *Running* and/or *Execution State*, "
                       + "and the matched Job will be compared to the chosen values.\n"
                       + "\n "
                       + "* Either a *Job UUID*, or a *Job Name* is required to identify the job.\n"
                       + "* *Execution State* AND/OR *Running* must be entered\n"
                       + "\n"
                       + "*Condition* indicates how the comparison should be evaluated.\n"
                       + "\n"
                       + "If the condition does not evaluate to true, then the step will optionally *Halt*.\n"
                       + "\n"
                       + "If *Halt* is enabled, when the condition is not true, the workflow will be halted"
                       + " with one of these results:\n"
                       + "\n"
                       + "* *Fail When Halted* - if true, the workflow will halt with a Failure status.\n"
                       + "* *Status When Halted* - if set, the workflow will halt with this status.\n"
                       + "* Otherwise it will halt with Success status.\n")

@PluginMetadata(key = ExecutionEnvironmentConstants.ENVIRONMENT_TYPE_KEY, value = ExecutionEnvironmentConstants.LOCAL_RUNNER)

public class JobStateWorkflowStep implements StepPlugin {
    public static final String EXEC_STATE_FAILED = "Failed";
    public static final String EXEC_STATE_ABORTED = "Aborted";
    public static final String EXEC_STATE_TIMED_OUT = "TimedOut";
    public static final String EXEC_STATE_NEVER = "Never";
    public static final String CON_EQUALS = "Equals";
    public static final String CON_NOTEQUALS = "Not Equals";
    //    public static final String CON_REGEX = "Regular Expression Match";
    public static final String EXEC_STATE_SUCCEEDED = "Succeeded";
    public static final String EXEC_STATE_FAILED_WITH_RETRY = "Failed-with-retry";
    public static final String PROVIDER_NAME = "job-state-conditional";

    @PluginProperty(title = "Job Project",
            description = "The project name (needed only if is an external project).")
    String jobProject;

    @PluginProperty(title = "Job Name",
                    description = "Group and Name for the Job in the form \"group/name\".")
    String jobName;
    @PluginProperty(title = "Job UUID",
                    description = "UUID for the Job.")
    String jobUUID;
    @PluginProperty(title = "Running",
                    description = "Assert that the job is or is not running. No value indicates it will not be checked.")
    @SelectValues(freeSelect = false, values = {"true","false"})
    String running;
    @PluginProperty(title = "Execution State",
                    description =
                            "Assert the State of the Job's last execution. \"Never\" indicates the Job has never " +
                            "run. "+
                            "If a Custom state is entered, it is not case-sensitive. "
    )
    @SelectValues(freeSelect = true, values = {
            EXEC_STATE_SUCCEEDED,
            EXEC_STATE_FAILED,
            EXEC_STATE_ABORTED,
            EXEC_STATE_TIMED_OUT,
            EXEC_STATE_FAILED_WITH_RETRY,
            EXEC_STATE_NEVER
    })
    String executionState;

    @PluginProperty(title = "Condition",
                    description = "Whether the assertion should match or not.",
                    required = true,
                    defaultValue = "Equals")
    @SelectValues(values = {CON_EQUALS, CON_NOTEQUALS/*, CON_REGEX*/})
    String condition;

    //flow control properties:
    @PluginProperty(title = "Halt",
                    description =
                        "Halt if the Condition is not met. (**If Halt is not selected, the workflow execution will "
                        + "continue.**)")
    boolean halt;
    @PluginProperty(title = "Fail when Halted",
                    description = "Use a failure status when Halted, otherwise use a success status when Halted.")
    boolean fail;
    @PluginProperty(title = "Status when Halted",
                    description = "Use a custom status message when Halted (overrides Fail when Halted).")
    String status;

    static enum Failures implements FailureReason {
        ConditionNotMet
    }

    @Override
    public void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
        if (null == jobUUID && null == jobName) {
            throw new StepException(
                    "Configuration invalid: jobUUID or jobName is required",
                    StepFailureReason.ConfigurationFailure
            );
        }
        if (null == executionState && null == running) {
            throw new StepException(
                    "Configuration invalid: executionState or running is required",
                    StepFailureReason.ConfigurationFailure
            );
        }
        JobService jobService = context.getExecutionContext().getJobService();
        final JobState jobState;
        final JobReference jobReference;
        try {
            String project = context.getFrameworkProject();
            if(null != jobProject){
                project = jobProject;
            }

            if (null != jobUUID) {
                jobReference = jobService.jobForID(jobUUID, project);
            } else {
                jobReference = jobService.jobForName(jobName, project);
            }
            jobState = jobService.getJobState(jobReference);
        } catch (JobNotFound jobNotFound) {
            throw new StepException(
                    "Job was not found: " + jobNotFound.getMessage(),
                    StepFailureReason.ConfigurationFailure
            );
        }

        //evaluate job state
        boolean result=false;
        boolean equality = null == condition || !CON_NOTEQUALS.equalsIgnoreCase(condition);


        String message = renderOutcome(jobState, jobReference, equality).toString();

        if (null != running) {
            result = checkRunning(jobState);
            finishConditional(context, result, equality, message);
        }
        if (null != executionState) {
            result = checkExecutionState(jobState.getPreviousExecutionState(),jobState.getPreviousExecutionStatusString());
            finishConditional(context, result, equality, message);
        }

    }

    private void finishConditional(PluginStepContext context, boolean result, boolean equality, String message)
            throws StepException
    {
        if(result!=equality) {
            context.getLogger().log(halt && fail ? 0 : 1, message);
            haltConditionally(context);
        }else{
            context.getLogger().log(2, message);
        }
    }

    private void haltConditionally(PluginStepContext context) {
        if (halt) {
            if (null == context.getFlowControl()) {
                context.getLogger().log(
                        0,
                        "[" +
                        PROVIDER_NAME +
                        "] HALT requested, but no FlowControl available in this context"
                );
                return;
            }
            if (null != status) {
                context.getFlowControl().Halt(status);
            } else {
                context.getFlowControl().Halt(!fail);
            }
        } else if (context.getFlowControl() != null) {
            context.getFlowControl().Continue();
        }
    }

    private StringBuilder renderOutcome(JobState jobState, JobReference jobReference, boolean equality) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{{Job ")
                     .append(jobReference.getId())
                     .append("}} ")
        ;
        if(null!=running ) {
            stringBuilder.append("is ");
            if(!jobState.isRunning()) {
                stringBuilder.append("NOT ");
            }
            stringBuilder.append("RUNNING ");

        }
        if(null!=executionState && null!=running) {
            stringBuilder.append("AND ");
        }
        if(null!=executionState){
            stringBuilder.append("previously ");
            if(jobState.getPreviousExecutionState()==ExecutionState.other){

                stringBuilder
                        .append("'")
                        .append(
                                null != jobState.getPreviousExecutionStatusString()
                                ? jobState.getPreviousExecutionStatusString()
                                : "[empty]"
                        )
                        .append("'");
            } else {
                stringBuilder.append(
                        null != jobState.getPreviousExecutionState() ? jobState.getPreviousExecutionState()
                                                                               .toString().toUpperCase() : "NEVER"
                );
            }
        }
        stringBuilder.append(". Expected ");

        if(null!=running) {
            stringBuilder.append((shouldBeRunning() ^ equality) ? "NOT RUNNING " : "RUNNING ");
        }
        if(null!=executionState && null!=running) {
            stringBuilder.append("AND ");
        }
        if(null!=executionState){
            if(!equality) {
                stringBuilder.append("NOT ");
            }
            stringBuilder.append("'");
            stringBuilder.append(executionState);
            stringBuilder.append("'");
        }
        return stringBuilder;
    }

    private boolean shouldBeRunning() {
        return null!=running && "true".equalsIgnoreCase(running);
    }
    private boolean shouldNotBeRunning() {
        return null!=running && "false".equalsIgnoreCase(running);
    }

    public boolean checkExecutionState(ExecutionState previousExecutionState, String customStatusString) {
        if (executionState.equalsIgnoreCase(EXEC_STATE_NEVER)) {
            return null == previousExecutionState;
        }
        if(customStatusString!=null) {
            //compare custom string
            return executionState.equalsIgnoreCase(customStatusString);
        }else {
            try {
                ExecutionState testState = ExecutionState.valueOf(
                        executionState.trim().replace('-', '_').replaceAll(
                                "\\s",
                                ""
                        ).toLowerCase()
                );
                return previousExecutionState == testState;
            } catch (IllegalArgumentException e) {
                //TODO: custom state
            }
        }
        return false;
    }

    public boolean checkRunning(JobState jobState) {
        if (shouldBeRunning()) {
            return jobState.isRunning();
        }

        if (shouldNotBeRunning()) {
            return !jobState.isRunning();
        }
        return false;
    }

}
