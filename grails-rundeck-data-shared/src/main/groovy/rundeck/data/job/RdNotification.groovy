package rundeck.data.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.notification.NotificationData
import rundeck.data.validation.shared.SharedNotificationConstraints

@JsonIgnoreProperties(["errors"])
class RdNotification implements NotificationData, Validateable {
    String eventTrigger;
    String type;
    String format;
    Map<String, Object> configuration;

    static constraints = {
        importFrom(SharedNotificationConstraints)
    }

    //match methods on Notification that convert legacy formats
    Map<String,Object> mailConfiguration() {
        return configuration;
    }

    //match methods on Notification that convert legacy formats
    Map<String,Object> urlConfiguration() {
        return configuration;
    }
}
