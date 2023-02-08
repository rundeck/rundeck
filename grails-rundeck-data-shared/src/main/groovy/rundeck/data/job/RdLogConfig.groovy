package rundeck.data.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.config.LogConfig
import rundeck.data.validation.shared.SharedLogConfigConstraints

@JsonIgnoreProperties(["errors"])
class RdLogConfig implements LogConfig, Validateable {
    String loglevel="INFO"
    String logOutputThreshold
    String logOutputThresholdAction="halt"
    String logOutputThresholdStatus

    static constraints = {
        importFrom(SharedLogConfigConstraints)
    }
}
