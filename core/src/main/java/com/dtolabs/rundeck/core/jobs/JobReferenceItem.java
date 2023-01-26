package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.StepExecutionItem;

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

    public JobReferenceItem(
            final String label,
            final String jobIdentifier,
            final String[] args,
            final boolean nodeStep,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final Boolean nodeKeepgoing,
            final String nodeFilter,
            final Integer nodeThreadcount,
            final String nodeRankAttribute,
            final Boolean nodeRankOrderAscending,
            final Boolean nodeIntersect,
            final String project,
            final Boolean failOnDisable,
            final Boolean importOptions,
            final String uuid,
            final Boolean useName,
            final Boolean ignoreNotifications,
            final Boolean childNodes
    )
    {
        this.label = label;
        this.jobIdentifier = jobIdentifier;
        this.args = args;
        this.nodeStep = nodeStep;
        this.handler = handler;
        this.keepgoingOnSuccess = keepgoingOnSuccess;
        this.nodeKeepgoing = nodeKeepgoing;
        this.nodeFilter = nodeFilter;
        this.nodeThreadcount = nodeThreadcount;
        this.nodeRankAttribute = nodeRankAttribute;
        this.nodeRankOrderAscending = nodeRankOrderAscending;
        this.nodeIntersect = nodeIntersect;
        this.project=project;
        this.failOnDisable = failOnDisable;
        this.importOptions = importOptions;
        this.uuid = uuid;
        this.useName = useName;
        this.ignoreNotifications = ignoreNotifications;
        this.childNodes = childNodes;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public String getJobIdentifier() {
        return jobIdentifier;
    }

    @Override
    public String[] getArgs() {
        return args;
    }

    @Override
    public boolean isNodeStep() {
        return nodeStep;
    }

    @Override
    public StepExecutionItem getFailureHandler() {
        return handler;
    }

    @Override
    public boolean isKeepgoingOnSuccess() {
        return keepgoingOnSuccess;
    }

    @Override
    public Boolean getNodeKeepgoing() {
        return nodeKeepgoing;
    }

    @Override
    public String getNodeFilter() {
        return nodeFilter;
    }

    @Override
    public Integer getNodeThreadcount() {
        return nodeThreadcount;
    }

    @Override
    public String getNodeRankAttribute() {
        return nodeRankAttribute;
    }

    @Override
    public Boolean getNodeRankOrderAscending() {
        return nodeRankOrderAscending;
    }

    @Override
    public Boolean getNodeIntersect() {
        return nodeIntersect;
    }

    @Override
    public String getProject() {
        return project;
    }

    public Boolean isFailOnDisable(){return failOnDisable;}


    public Boolean isImportOptions(){return importOptions;}

    public String getUuid(){return uuid;}

    public Boolean isUseName(){return useName;}

    public Boolean isIgnoreNotifications(){
        return ignoreNotifications;
    }

    public Boolean isChildNodes(){return childNodes;}


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