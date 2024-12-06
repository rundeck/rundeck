package org.rundeck.util.api.responses.execution

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.openqa.selenium.json.Json
import org.rundeck.util.api.responses.jobs.Job

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class Execution {
    String id
    String href
    String permalink
    String status
    String project
    String executionType
    String user
    RetriedExecution retriedExecution
    int retryAttempt

    @JsonProperty("date-started")
    ExecutionDate dateStarted

    @JsonProperty("date-ended")
    ExecutionDate dateEnded

    Job job
    String description
    String argstring
    String serverUUID

    static class RetriedExecution {
        String id
        String permalink
        String href
        String status
    }

    static class ExecutionDate {
        long unixtime;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        Date date;
    }

    boolean endedBeforeExecution(Execution execution){
        return dateEnded.unixtime <= execution.dateStarted.unixtime
    }

    boolean overlappedWithExecution(Execution execution){
        return (dateStarted.unixtime <= execution.dateEnded.unixtime &&
                execution.dateStarted.unixtime <= dateEnded.unixtime)
    }
}
