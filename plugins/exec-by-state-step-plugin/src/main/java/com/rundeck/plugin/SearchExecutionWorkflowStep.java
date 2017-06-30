package com.rundeck.plugin;


import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.jobs.JobService;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;

import java.util.Date;
import java.util.Map;

@Plugin(service = ServiceNameConstants.WorkflowStep, name = "search-executions")
@PluginDescription(title = "Search Executions", description = "Search execution by state")
public class SearchExecutionWorkflowStep implements StepPlugin {
    public static String serviceName = "search-executions";

    @SelectValues(freeSelect = true, values = {"incomplete","failed", "succeeded"})
    @PluginProperty(name = "state", title = "State", description = "state of the execution", required = true)
    private String state;

    @PluginProperty(name = "job", title = "Job id", description = "search only for this job (optional)")
    private String jobUuid;

    @PluginProperty(name = "exclude", title = "Exclude job id", description = "exclude executions from this job (optional)")
    private String excludeJobUuid;


    @PluginProperty(name = "since", title = "Executions since", description = "Time ago to search, in seconds," +
            " +  or specify time units: \"120m\", \"2h\", \"3d\".(optional)")
    private String since;

    @PluginProperty(name = "json", title = "Json output", description = "Format output as json to display")
    private boolean json;

    @Override
    public void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
        String project = context.getFrameworkProject();

        JobService jobService = context.getExecutionContext().getJobService();


        SearchExecution se = new SearchExecution(state, project, jobUuid, excludeJobUuid,since);
        se.checkState();
        String res = se.execute(jobService,json);

        context.getExecutionContext().getExecutionListener().log(2,res);
    }
}
