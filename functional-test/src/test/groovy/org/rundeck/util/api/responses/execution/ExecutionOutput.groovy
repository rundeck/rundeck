package org.rundeck.util.api.responses.execution

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

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
