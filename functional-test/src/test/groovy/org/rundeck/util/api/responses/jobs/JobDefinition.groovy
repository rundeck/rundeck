package org.rundeck.util.api.responses.jobs

import groovy.transform.CompileStatic
import groovy.transform.builder.Builder

@CompileStatic
@Builder
class JobDefinition {
    String defaultTab
    String description
    boolean executionEnabled
    String group
    String id
    String loglevel
    String name
    boolean nodeFilterEditable
    Plugins plugins
    boolean scheduleEnabled
    Sequence sequence
    String uuid
    Schedule schedule
    List<String> schedules
    List<Option> options

    @CompileStatic
    static class Plugins {
        Object executionLifecycle
    }

    @CompileStatic
    static class Sequence {
        List<Command> commands
        boolean keepgoing
        String strategy
    }

    @CompileStatic
    static class Command {
        String exec
    }

    @CompileStatic
    static class Option {
        String name
    }

    @CompileStatic
    static class Schedule {
        String month
        Time time
        Weekday weekday
        String year
    }

    @CompileStatic
    static class Time {
        String hour
        String minute
        String seconds
    }

    @CompileStatic
    static class Weekday {
        String day
    }
}
