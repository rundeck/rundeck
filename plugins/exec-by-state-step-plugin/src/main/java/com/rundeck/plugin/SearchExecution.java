package com.rundeck.plugin;


import com.dtolabs.rundeck.core.execution.ExecutionReference;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.jobs.JobService;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;


import java.util.*;

public class SearchExecution {
    static Logger logger = Logger.getLogger(SearchExecution.class);

    private String state;
    private String project;
    private String jobUuid;
    private String excludeJobUuid;
    private String since;


    public SearchExecution(String state, String project, String jobUuid, String excludeJobUuid, String since){
        this.state = state;
        this.project = project;
        this.jobUuid = jobUuid;
        this.excludeJobUuid = excludeJobUuid;
        this.since = since;
    }


    public void checkState()throws StepException {
        String baseProperty= "project.plugin."+ ServiceNameConstants.WorkflowStep+
                "."+ SearchExecutionWorkflowStep.serviceName;
        if(isNotSet(this.state)){
            throw new StepException("status is required", PluginFailureReason.MissingProperty);
        }
        if(isNotSet(this.project)){
            throw new StepException("project is required", PluginFailureReason.MissingProperty);
        }
    }


    public String execute(JobService jobService, boolean toJson) throws StepException {
        List<ExecutionReference> list = jobService.searchExecutions(state,project, jobUuid,excludeJobUuid,since);
        if(toJson){
            Gson gson = new GsonBuilder().serializeNulls().create();
            return gson.toJson(list);
        }
        Iterator<ExecutionReference> iterator = list.iterator();
        StringBuilder execList = new StringBuilder("RUNDECK:DATA:executions = ");
        while (iterator.hasNext()) {
            ExecutionReference exec = iterator.next();
            execList.append(exec.getId());
            if(iterator.hasNext()) {
                execList.append(',');
            }
        }
        return execList.toString();


    }

    private boolean isNotSet(String value){
        return null == value || value.trim().length()==0;
    }
}
