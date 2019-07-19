package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class JobPreExecutionEventImpl implements JobPreExecutionEvent {

    private String projectName;
    private String userName;
    private Map scheduledExecutionMap;
    private Map optionsValues;
    private INodeSet nodeSet;

    public JobPreExecutionEventImpl(String projectName, String userName, Map scheduledExecutionMap, Map optionsValues, INodeSet nodeSet) throws IOException, ClassNotFoundException {
        this.projectName = projectName;
        this.userName = userName;
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(scheduledExecutionMap);
            oos.flush();
            ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bin);
            this.scheduledExecutionMap = (Map) ois.readObject();
        }
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(optionsValues);
            oos.flush();
            ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bin);
            this.optionsValues = (Map) ois.readObject();
        }
        this.nodeSet = nodeSet;
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
    public Map<String, String> getOptionsValues() {
        return this.optionsValues;
    }

    @Override
    public INodeSet getNodes() {
        return this.nodeSet;
    }

    public Map getScheduledExecutionMap(){
        return this.scheduledExecutionMap;
    }
}
