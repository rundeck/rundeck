package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.jobs.JobOption;
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;

import java.util.*;

public class JobPreExecutionEventImpl implements JobPreExecutionEvent {

    private String projectName;
    private String userName;
    private Map scheduledExecutionMap;
    private HashMap optionsValues;
    private String nodeFilter;
    private INodeSet nodes;
    private SortedSet<JobOption> options;


    public JobPreExecutionEventImpl(String projectName,
                                    String userName,
                                    Map scheduledExecutionMap,
                                    HashMap optionsValues,
                                    INodeSet nodes,
                                    String nodeFilter) throws ValidationException {

        this.projectName = projectName;
        this.userName = userName;
        if(scheduledExecutionMap != null){
            this.scheduledExecutionMap = (Map)((HashMap) scheduledExecutionMap).clone();
            if(this.scheduledExecutionMap.containsKey("options")){
                ArrayList<LinkedHashMap> originalOptions = (ArrayList<LinkedHashMap>) this.scheduledExecutionMap.get("options");
                setOptions(originalOptions);
            }
        }
        if(optionsValues != null){
            this.optionsValues = (HashMap) optionsValues.clone();
        }else{
            this.optionsValues = new HashMap();
        }
        this.nodes = nodes;
        this.nodeFilter = nodeFilter;

    }

    public JobPreExecutionEventImpl(JobPreExecutionEventImpl origin) throws ValidationException {
        this(origin.projectName, origin.userName, origin.scheduledExecutionMap, origin.optionsValues, origin.nodes, origin.nodeFilter);
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
    public SortedSet<JobOption> getOptions() {
        return this.options;
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

    @Override
    public INodeSet getNodes() { return this.nodes; }

    private void setOptions(ArrayList<LinkedHashMap> originalOptions) throws ValidationException {
        if(originalOptions != null && !originalOptions.isEmpty()){
            this.options = new TreeSet<JobOption>();
            for (LinkedHashMap originalOption: originalOptions) {
                JobOptionImpl option = new JobOptionImpl(originalOption);
                this.options.add(option);
            }
        }
    }

}
