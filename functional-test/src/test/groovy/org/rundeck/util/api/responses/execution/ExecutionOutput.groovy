package org.rundeck.util.api.responses.execution

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class LogEntry {
    String time
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    String absolute_time
    String log
    String level
    String stepctx
    String node
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ExecutionOutput {
    String id
    String offset
    boolean completed
    boolean execCompleted
    boolean hasFailedNodes
    String execState
    String lastModified
    int execDuration
    float percentLoaded
    int totalSize
    int retryBackoff
    boolean clusterExec
    String serverNodeUUID
    boolean compacted
    List<LogEntry> entries
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ExecutionInfo {
    String id;
    String href;
    String permalink;
    String status;
    String project;
    String executionType;
    String user;

    @JsonProperty("date-started")
    DateInfo dateStarted;
    @JsonProperty("date-ended")
    DateInfo dateEnded;

    JobInfo job;
    String description;
    String argstring;
    String serverUUID;
    List<String> successfulNodes;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class DateInfo {
    long unixtime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    String date;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class JobInfo {
    String id;
    int averageDuration;
    String name;
    String group;
    String project;
    String description;
    Map<String, String> options;
    String href;
    String permalink;
}