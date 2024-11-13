package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import lombok.Data;

@Data
public class JobReferenceItem extends JobRefCommandBase {
    private final String label;
    private final String jobIdentifier;
    private final String[] args;
    private final boolean nodeStep;
    private final StepExecutionItem failureHandler;
    private final boolean keepgoingOnSuccess;
    private final Boolean nodeKeepgoing;
    private final String nodeFilter;
    private final Integer nodeThreadcount;
    private final String nodeRankAttribute;
    private final Boolean nodeRankOrderAscending;
    private final Boolean nodeIntersect;
    private final String project;
    private final Boolean failOnDisable;
    private final Boolean importOptions;
    private final String uuid;
    private final Boolean useName;
    private final Boolean ignoreNotifications;
    private final Boolean childNodes;
    private final boolean enabled;

    @Override
    public Boolean isFailOnDisable() {
        return getFailOnDisable();
    }

    @Override
    public Boolean isImportOptions() {
        return getImportOptions();
    }

    @Override
    public Boolean isUseName() {
        return getUseName();
    }

    @Override
    public Boolean isIgnoreNotifications() {
        return getIgnoreNotifications();
    }

    @Override
    public Boolean isChildNodes() {
        return getChildNodes();
    }

    @Override
    public Boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public String toString() {
        return "JobReferenceItem{" +
                (label != null ? "label='" + label + ", " : "") +
                "jobIdentifier='" + jobIdentifier + '\'' +
                (project != null ? ", project='" + project + '\'' : "") +
                ", nodeStep=" + nodeStep +
                "}";
    }
}