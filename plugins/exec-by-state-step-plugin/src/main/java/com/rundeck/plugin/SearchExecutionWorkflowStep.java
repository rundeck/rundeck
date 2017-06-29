package com.rundeck.plugin;


import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.jobs.JobService;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.Map;

@Plugin(service = ServiceNameConstants.WorkflowStep, name = "search-executions")
@PluginDescription(title = "Search Executions", description = "Search execution by state")
public class SearchExecutionWorkflowStep implements StepPlugin {
    public static String serviceName = "search-executions";

    @SelectValues(freeSelect = true, values = {"incomplete","failed", "succeeded"})
    @PluginProperty(name = "state", title = "state", description = "state of the execution", required = true)
    private String state;

    @PluginProperty(name = "json", title = "json output", description = "Format output as json to display")
    private boolean json;

    @Override
    public void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
        String project = context.getFrameworkProject();

        JobService jobService = context.getExecutionContext().getJobService();


        SearchExecution se = new SearchExecution(state, project);
        se.checkState();
        String res = se.execute(jobService);
        if(json) {
            System.out.println(res);
        }else{
            System.out.println("RUNDECK:DATA:executions = "+res);
        }
    }
}
