package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class JobPreExecutionEventImpl implements JobPreExecutionEvent {

    private String projectName;
    private String userName;
    private Map scheduledExecutionMap;
    private Map optionsValues;

    public void setProjectName(String projectName){
        this.projectName = projectName;
    }
    public void setUserName(String userName){
        this.userName = userName;
    }
    public void setScheduledExecutionMap(Map scheduledExecutionMap){
        this.scheduledExecutionMap = scheduledExecutionMap;
    }
    public void setOptionsValues(Map optionsValues) {
        this.optionsValues = optionsValues;
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
    public Map<String, String> getOptionsValues() {
        return this.optionsValues;
    }
}
