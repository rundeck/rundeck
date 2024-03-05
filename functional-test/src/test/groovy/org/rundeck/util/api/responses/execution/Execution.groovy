package org.rundeck.util.api.responses.execution

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.rundeck.util.api.responses.jobs.Job

@JsonIgnoreProperties(ignoreUnknown = true)
class Execution {
    int id
    String href
    String permalink
    String status
    String project
    String executionType
    String user
    RetriedExecution retriedExecution
    int retryAttempt

    @JsonProperty("date-started")
    DateStarted dateStarted

    Job job
    String description
    String argstring
    String serverUUID

    static class DateStarted {
        long unixtime
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        Date date
    }

    static class RetriedExecution {
        long id
        String permalink
        String href
        String status
    }
}
