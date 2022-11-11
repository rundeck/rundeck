package org.rundeck.app.data.model.v1.job.notification;

public interface NotificationData {
    String getEventTrigger();
    String getType();
    String getFormat();
    String getContent();
}
