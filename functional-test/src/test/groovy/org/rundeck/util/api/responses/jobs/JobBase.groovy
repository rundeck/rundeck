package org.rundeck.util.api.responses.jobs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class JobBase {
    @JsonProperty("description")
    protected String description;

    @JsonProperty("executionEnabled")
    public
    boolean executionEnabled;

    @JsonProperty("group")
    protected String group;

    @JsonProperty("id")
    protected String id;

    @JsonProperty("loglevel")
    protected String loglevel;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("nodeFilterEditable")
    protected boolean nodeFilterEditable;

    @JsonProperty("plugins")
    protected Plugins plugins;

    @JsonProperty("scheduleEnabled")
    protected boolean scheduleEnabled;

    @JsonProperty("sequence")
    protected Sequence sequence;

    @JsonProperty("uuid")
    protected String uuid;

    @JsonProperty("schedule")
    protected Schedule schedule;

    @JsonProperty("schedules")
    protected List<String> schedules;

    public static class Plugins {
        @JsonProperty("ExecutionLifecycle")
        private Object ExecutionLifecycle;
    }

    public static class Sequence {
        @JsonProperty("commands")
        private List<Command> commands;

        @JsonProperty("keepgoing")
        private boolean keepgoing;

        @JsonProperty("strategy")
        private String strategy;
    }

    public static class Command {
        @JsonProperty("exec")
        private String exec;
    }

    public static class Schedule {
        @JsonProperty("month")
        private String month

        @JsonProperty("time")
        private Time time

        @JsonProperty("weekday")
        private Weekday weekday

        @JsonProperty("year")
        private String year
    }

    public static class Time {
        @JsonProperty("hour")
        private String hour

        @JsonProperty("minute")
        private String minute

        @JsonProperty("seconds")
        private String seconds
    }

    public static class Weekday {
        @JsonProperty("day")
        private String day
    }
}
