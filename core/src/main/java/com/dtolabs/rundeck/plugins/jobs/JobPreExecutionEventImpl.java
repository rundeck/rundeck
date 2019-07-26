package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JobPreExecutionEventImpl implements JobPreExecutionEvent {

    private String projectName;
    private String userName;
    private Map scheduledExecutionMap;
    private HashMap optionsValues;
    private String nodeFilter;

    public JobPreExecutionEventImpl(String projectName, String userName, Map scheduledExecutionMap, HashMap optionsValues) {
        this.projectName = projectName;
        this.userName = userName;
        if(scheduledExecutionMap != null){
            this.scheduledExecutionMap = (Map)((HashMap) scheduledExecutionMap).clone();
            if(this.scheduledExecutionMap.get("filter") != null){
                this.nodeFilter = (String) this.scheduledExecutionMap.get("filter");
            }
        }
        if(optionsValues != null){
            this.optionsValues = (HashMap) optionsValues.clone();
        }else{
            this.optionsValues = new HashMap();
        }

    }

    public JobPreExecutionEventImpl(JobPreExecutionEventImpl origin){
        this(origin.projectName, origin.userName, origin.scheduledExecutionMap, origin.optionsValues);
    }

    public void setProjectName(String projectName){
        this.projectName = projectName;
    }
    public void setUserName(String userName){
        this.userName = userName;
    }

    @Override
    public String getProjectName() {
        return this.projectName;
    }

    @Override
    public ArrayList<LinkedHashMap> getOptions() {
        if(this.scheduledExecutionMap != null && this.scheduledExecutionMap.containsKey("options")){
            return (ArrayList<LinkedHashMap>) this.scheduledExecutionMap.get("options");
        }
        return null;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    @Override
    public HashMap getOptionsValues() {
        return this.optionsValues;
    }

    @Override
    public String getNodeFilter() {
        return this.nodeFilter;
    }

}
