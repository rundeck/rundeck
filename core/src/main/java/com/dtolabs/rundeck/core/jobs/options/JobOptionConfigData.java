package com.dtolabs.rundeck.core.jobs.options;

import java.util.ArrayList;
import java.util.List;

public class JobOptionConfigData {
    private List<JobOptionConfigEntry> jobOptionConfigEntries;

    public JobOptionConfigData() {
        this.jobOptionConfigEntries = new ArrayList<>();
    }

    public void addConfig(JobOptionConfigEntry jobOptionConfigEntry){
        this.jobOptionConfigEntries.add(jobOptionConfigEntry);
    }

    public List<JobOptionConfigEntry> getJobOptionConfigEntries() {
        return jobOptionConfigEntries;
    }

    public JobOptionConfigEntry getJobOptionEntry(Class classType){
        return jobOptionConfigEntries.stream().filter(it->classType.isInstance(it)).findFirst().orElse(null);
    }

}
