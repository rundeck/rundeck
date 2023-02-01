package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import lombok.Data;

@Data
public class JobReferenceItem extends JobRefCommandBase {
    private final String label;
    private final String jobIdentifier;
    private final String[] args;
    private final boolean nodeStep;
    private final StepExecutionItem handler;
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