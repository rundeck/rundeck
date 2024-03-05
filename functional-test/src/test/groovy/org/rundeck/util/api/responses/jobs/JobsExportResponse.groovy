package org.rundeck.util.api.responses.jobs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class JobsExportResponse {
    @JsonProperty("description")
    String description
    @JsonProperty("executionEnabled")
    boolean executionEnabled
    @JsonProperty("group")
    String group
    @JsonProperty("id")
    String id
    @JsonProperty("loglevel")
    String loglevel
    @JsonProperty("name")
    String name
    @JsonProperty("nodeFilterEditable")
    boolean nodeFilterEditable
    @JsonProperty("plugins")
    Job.Plugins plugins
    @JsonProperty("schedule")
    Job.Schedule schedule
    @JsonProperty("scheduleEnabled")
    String scheduleEnabled
    @JsonProperty("sequence")
    Job.Sequence sequence
    @JsonProperty("uuid")
    String uuid
}
