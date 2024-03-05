package org.rundeck.util.api.responses.system

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class SystemInfo {
    System system

    static class System {
        Executions executions
        Extended extended
        Healthcheck healthcheck
        Jvm jvm
        Metrics metrics
        Os os
        Ping ping
        Rundeck rundeck
        Stats stats
        ThreadDump threadDump
        Timestamp timestamp
    }

    static class Executions {
        String active
        String executionMode
    }

    static class Extended {
        @JsonProperty("RundeckPro")
        RundeckPro rundeckPro
    }

    static class RundeckPro {
        String version
        String buildGit
        String edition
    }

    static class Healthcheck {
        String contentType
        String href
    }

    static class Jvm {
        String implementationVersion
        String name
        String vendor
        String version
    }

    static class Metrics {
        String contentType
        String href
    }

    static class Os {
        String arch
        String name
        String version
    }

    static class Ping {
        String contentType
        String href
    }

    static class Rundeck {
        String apiversion
        String base
        String build
        @JsonProperty("buildGit")
        String buildGitVersion
        String node
        String serverUUID
        String version
    }

    static class Stats {
        Cpu cpu
        Memory memory
        Scheduler scheduler
        Threads threads
        Uptime uptime
    }

    static class Cpu {
        LoadAverage loadAverage
        int processors
    }

    static class LoadAverage {
        int average
        String unit
    }

    static class Memory {
        long free
        long max
        long total
        String unit
    }

    static class Scheduler {
        int running
        int threadPoolSize
    }

    static class Threads {
        int active
    }

    static class Uptime {
        long duration
        Duration since
        String unit
    }

    static class Duration {
        String datetime
        long epoch
        String unit
    }

    static class ThreadDump {
        String contentType
        String href
    }

    static class Timestamp {
        String datetime
        long epoch
        String unit
    }

}
