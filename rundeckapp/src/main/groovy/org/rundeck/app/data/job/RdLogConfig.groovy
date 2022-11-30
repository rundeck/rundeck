package org.rundeck.app.data.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.config.LogConfig
import rundeck.ScheduledExecution

@JsonIgnoreProperties(["errors"])
class RdLogConfig implements LogConfig, Validateable {
    String loglevel="WARN";
    String logOutputThreshold;
    String logOutputThresholdAction;
    String logOutputThresholdStatus;

    static constraints = {
        importFrom(ScheduledExecution)
    }
}
