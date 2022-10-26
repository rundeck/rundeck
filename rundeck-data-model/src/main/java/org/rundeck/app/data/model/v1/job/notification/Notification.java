package org.rundeck.app.data.model.v1.job.notification;

import java.util.Map;

public interface Notification {
    String getEventTrigger();
    String getType();
    String getFormat();
    Map<String,Object> getContent();
}
