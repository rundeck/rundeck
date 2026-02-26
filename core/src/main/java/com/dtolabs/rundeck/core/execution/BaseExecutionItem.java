package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.plugins.PluginConfiguration;
import org.rundeck.app.data.model.v1.job.workflow.ConditionalSet;

import java.util.List;

/**
 * Created by greg on 5/11/16.
 */
public abstract class BaseExecutionItem implements StepExecutionItem, HasLoggingFilterConfiguration {
    private String label;
    private List<PluginConfiguration> pluginLoggingConfigurations;
    private ConditionalSet conditions;

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

    @Override
    public ConditionalSet getConditions() {
        return conditions;
    }

    public void setConditions(ConditionalSet conditions) {
        this.conditions = conditions;
    }
}
