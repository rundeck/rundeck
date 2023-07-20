package org.rundeck.app.data.model.v1.job.notification;

import java.util.Map;

public interface NotificationData {
    String getEventTrigger();
    String getType();
    String getFormat();
    Map<String, Object> getConfiguration();

}
