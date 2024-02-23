package org.rundeck.util.api.responses.jobs

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class Job {
    @JsonProperty("description")
    private String description;

    @JsonProperty("executionEnabled")
    public
    boolean executionEnabled;

    @JsonProperty("group")
    private String group;

    @JsonProperty("id")
    private String id;

    @JsonProperty("loglevel")
    private String loglevel;

    @JsonProperty("name")
    private String name;

    @JsonProperty("nodeFilterEditable")
    private boolean nodeFilterEditable;

    @JsonProperty("plugins")
    private Plugins plugins;

    @JsonProperty("scheduleEnabled")
    private boolean scheduleEnabled;

    @JsonProperty("sequence")
    private Sequence sequence;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("schedule")
    private Schedule schedule;

    @JsonProperty("schedules")
    private List<String> schedules;

    @JsonProperty("options")
    private Map<String, Object> options = new HashMap<>()

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

    @JsonAnySetter
    void setOptions(String key, Object value) {
        options.put(key, value);
    }

    @JsonIgnore
    Object getOptions(String key) {
        return options.get(key);
    }
}
