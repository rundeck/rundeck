package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.jobs.JobOption;
import com.dtolabs.rundeck.core.jobs.JobPersistEvent;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;

import java.util.*;

public class JobPersistEventImpl implements JobPersistEvent {

    String projectName;
    SortedSet<JobOption> options;
    INodeSet nodes;
    String userName;
    String nodeFilter;
    Map scheduledExecutionMap;

    public JobPersistEventImpl(Map scheduledExecutionMap, String userName, INodeSet nodes) throws ValidationException {
        this.scheduledExecutionMap = scheduledExecutionMap;
        if(this.scheduledExecutionMap != null){
            if(this.scheduledExecutionMap.containsKey("nodefilters")){
                this.nodeFilter = (String)((LinkedHashMap) this.scheduledExecutionMap.get("nodefilters")).get("filters");
            }
            if(this.scheduledExecutionMap.containsKey("project")){
                this.projectName = (String) this.scheduledExecutionMap.get("project");
            }
            if(this.scheduledExecutionMap.containsKey("options")){
                ArrayList<LinkedHashMap> originalOptions = (ArrayList<LinkedHashMap>) this.scheduledExecutionMap.get("options");
                setOptions(originalOptions);
            }
        }
        this.userName = userName;
        this.nodes = nodes;
    }

    public JobPersistEventImpl(JobPersistEventImpl origin) throws ValidationException {
        this(origin.scheduledExecutionMap, origin.userName, origin.nodes);
    }

    @Override
    public String getProjectName() { return this.projectName; }

    @Override
    public SortedSet<JobOption> getOptions() { return this.options; }

    @Override
    public INodeSet getNodes() { return this.nodes; }

    public void setNodes(INodeSet nodeSet) { this.nodes = nodeSet; }

    @Override
    public String getUserName() { return this.userName; }

    @Override
    public String getNodeFilter() { return this.nodeFilter; }

    private void setOptions(ArrayList<LinkedHashMap> originalOptions) throws ValidationException {
        if(originalOptions != null && !originalOptions.isEmpty()){
            this.options = new TreeSet<JobOption>();
            for (LinkedHashMap originalOption: originalOptions) {
                JobOptionImpl option = new JobOptionImpl(originalOption);
                this.options.add(option);
            }
        }
    }

    public void setNewOptions(SortedSet<JobOption> options) {
        this.options = options;
    }
}
