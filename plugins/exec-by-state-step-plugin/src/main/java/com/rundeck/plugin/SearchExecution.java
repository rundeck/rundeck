package com.rundeck.plugin;


import com.dtolabs.rundeck.core.execution.ExecutionReference;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.jobs.JobNotFound;
import com.dtolabs.rundeck.core.jobs.JobService;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchExecution {
    static Logger logger = Logger.getLogger(SearchExecution.class);

    private String state;
    private String project;

    public SearchExecution(String state, String project){
        this.state = state;
        this.project = project;
    }


    public void checkState()throws StepException {
        String baseProperty= "project.plugin."+ ServiceNameConstants.WorkflowStep+
                "."+ SearchExecutionWorkflowStep.serviceName;
        if(isNotSet(this.state)){
            throw new StepException("status is required", PluginFailureReason.MissingProperty);
        }
    }


    public String execute(JobService jobService) throws StepException {
        ArrayList<HashMap<String,String>> jobs = new ArrayList<>();
        Gson gson = new GsonBuilder().serializeNulls().create();

        try {
            List<ExecutionReference> list = jobService.executionForState(state,project);
            for (ExecutionReference exec:list) {
                HashMap<String, String> job = new HashMap<>();

                job.put("job",exec.getJob().getJobName());
                job.put("group",exec.getJob().getGroupPath());
                job.put("job_id",exec.getJob().getId());
                job.put("execution",exec.getId());
                job.put("filter",exec.getFilter());
                job.put("options",exec.getOptions());
                jobs.add(job);
            }
        }catch (JobNotFound e){
            e.printStackTrace();
        }


        return gson.toJson(jobs);
    }

    private boolean isNotSet(String value){
        return null == value || value.trim().length()==0;
    }
}
