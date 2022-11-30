package org.rundeck.app.data.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.schedule.ScheduleData
import rundeck.ScheduledExecution

@JsonIgnoreProperties(["errors"])
class RdSchedule implements ScheduleData, Validateable {
    String minute = "0"
    String hour = "0"
    String dayOfMonth = "?"
    String month = "*"
    String dayOfWeek = "*"
    String seconds = "0"
    String year = "*"
    String crontabString

    String generateCrontabExpression() {
        return [seconds?seconds:'0',minute,hour,dayOfMonth?.toUpperCase(),month?.toUpperCase(),dayOfMonth=='?'?dayOfWeek?.toUpperCase():'?',year?year:'*'].join(" ")
    }

    static constraints = {
        importFrom(ScheduledExecution)
    }
}
