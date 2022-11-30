package org.rundeck.app.data.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.notification.NotificationData
import rundeck.Notification

@JsonIgnoreProperties(["errors"])
class RdNotification implements NotificationData, Validateable {
    Long id
    String eventTrigger;
    String type;
    String format;
    Map<String, Object> configuration;

    static constraints = {
        importFrom(Notification)
        id(nullable: true)
    }
}
