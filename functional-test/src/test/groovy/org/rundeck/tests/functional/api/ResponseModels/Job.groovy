package org.rundeck.tests.functional.api.ResponseModels

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

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
    private Schedule;

    @JsonProperty("schedules")
    private List<String> schedules;

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
        private Weekday year
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
