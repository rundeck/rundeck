package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.plugins.PluginConfiguration;

import java.util.List;

/**
 * Created by greg on 5/11/16.
 */
public abstract class BaseExecutionItem implements StepExecutionItem, HasLoggingFilterConfiguration {
    private String label;
    private List<PluginConfiguration> pluginLoggingConfigurations;

    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public List<PluginConfiguration> getFilterConfigurations() {
        return pluginLoggingConfigurations;
    }
}
