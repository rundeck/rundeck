package org.rundeck.app.api.model

import io.swagger.v3.oas.annotations.media.Schema

import java.lang.management.ThreadInfo;

@Schema(description="System Information")
class SystemInfoModel {
    @Schema(name="system", description="System Information")
    SystemInfoSystem system
    static class SystemInfoSystem{
        TimestampInfoModel timestamp
        RundeckInfoModel rundeck
        ExecutionsInfoModel executions
        OsInfoModel os
        JvmInfoModel jvm
        StatInfoModel stats
        MetricsInfoModel metrics
        ThreadDumpInfoModel threadDump
        HealthCheckInfoModel healthcheck
        PingInfoModel ping
        Map extended

        @Schema(name="timestamp", description="Timestamp Information")
        static class TimestampInfoModel {
            long epoch
            String unit
            String datetime
        }
        @Schema(name="executions", description="Executions Information")
        static class ExecutionsInfoModel {
            String active
            String executionMode
        }
        @Schema(name="os", description="Operating System Information")
        static class OsInfoModel {
            String arch
            String name
            String version
        }
        @Schema(name="jvm", description="Jvm System Information")
        static class JvmInfoModel {
            String name
            String vendor
            String version
            String implementationVersion
        }

        @Schema(name="rundeck", description="Rundeck Information")
        static class RundeckInfoModel {
            String version
            String build
            String buildGit
            String node
            String base
            String apiversion
            String serverUUID
        }
        @Schema(name="stats", description="Stat Information")
        static class StatInfoModel {

            UptimeInfoModel uptime
            CpuInfoModel cpu
            MemoryInfoModel memory
            SchedulerInfoModel scheduler
            ThreadInfoModel threads
        }
        @Schema(name="uptime", description="Uptime Information")
        static class UptimeInfoModel {
            int duration
            String unit
            SinceInfoModel since
        }
        @Schema(name="since", description="Since Uptime Information")
        static class SinceInfoModel {
            long epoch
            String unit
            String datetime
        }
        @Schema(name="cpu", description="Cpu Information")
        static class CpuInfoModel {
            LoadAverageInfoModel loadAverage
            int processors
        }
        @Schema(name="loadAverage", description="Cpu Load Average Information")
        static class LoadAverageInfoModel {
            String unit
            int average
        }
        @Schema(name="memory", description="Memory Information")
        static class MemoryInfoModel {
            String unit
            int max
            int free
            int total
        }
        @Schema(name="scheduler", description="Scheduler Information")
        static class SchedulerInfoModel {
            int running
            int threadPoolSize
        }
        @Schema(name="threads", description="Thread Information")
        static class ThreadInfoModel {
            int active
        }

        @Schema(name="metrics", description="Metrics Information")
        static class MetricsInfoModel {
            String href
            String contentType
        }
        @Schema(name="threadDump", description="Thread Dump Information")
        static class ThreadDumpInfoModel {
            String href
            String contentType
        }

        @Schema(name="healthcheck", description="Health Check Information")
        static class HealthCheckInfoModel {
            String href
            String contentType
        }
        @Schema(name="ping", description="Ping Information")
        static class PingInfoModel {
            String href
            String contentType
        }
    }
}


