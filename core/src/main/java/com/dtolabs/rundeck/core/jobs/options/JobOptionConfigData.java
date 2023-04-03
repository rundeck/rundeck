package com.dtolabs.rundeck.core.jobs.options;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JobOptionConfigData {
    private final Map<String,JobOptionConfigEntry> jobOptionConfigEntries;

    public JobOptionConfigData() {
        this.jobOptionConfigEntries = new TreeMap<>();
    }

    public void addConfig(JobOptionConfigEntry jobOptionConfigEntry){
        this.jobOptionConfigEntries.put(jobOptionConfigEntry.configType(),jobOptionConfigEntry);
    }

    public Map<String, JobOptionConfigEntry> getJobOptionConfigEntries() {
        return jobOptionConfigEntries;
    }

    public JobOptionConfigEntry getJobOptionEntry(Class classType){
        return jobOptionConfigEntries.values().stream().filter(it->classType.isInstance(it)).findFirst().orElse(null);
    }

    public JobOptionConfigEntry getJobOptionEntry(String configType) {
        return jobOptionConfigEntries.get(configType);
    }

}
