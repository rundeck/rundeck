package com.dtolabs.rundeck.core.jobs.options;


import lombok.Getter;

import java.util.Map;
import java.util.TreeMap;

/**
 * Defines the model for storing generic configuration data for a job option
 */
@Getter
public class JobOptionConfigData {
    private final Map<String, JobOptionConfigEntry> jobOptionConfigEntries;

    public JobOptionConfigData() {
        this.jobOptionConfigEntries = new TreeMap<>();
    }

    public JobOptionConfigData(Map<String, JobOptionConfigEntry> values) {
        this.jobOptionConfigEntries = new TreeMap<>(values);
    }

    public void addConfig(JobOptionConfigEntry jobOptionConfigEntry) {
        this.jobOptionConfigEntries.put(jobOptionConfigEntry.configType(), jobOptionConfigEntry);
    }

    public JobOptionConfigEntry getJobOptionEntry(Class<?> classType) {
        return jobOptionConfigEntries.values().stream().filter(classType::isInstance).findFirst().orElse(null);
    }

    public JobOptionConfigEntry getJobOptionEntry(String configType) {
        return jobOptionConfigEntries.get(configType);
    }

}
