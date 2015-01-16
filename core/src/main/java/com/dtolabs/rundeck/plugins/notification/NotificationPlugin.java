package com.dtolabs.rundeck.plugins.notification;

import java.util.Map;

/**
 * NotificationPlugin interface for a Notification plugin
 * Created by greg
 * Date: 3/11/13
 * Time: 2:20 PM
 */
public interface NotificationPlugin {
    /**
     * Post a notification for the given trigger, dataset, and configuration
     * @param trigger event type causing notification
     * @param executionData execution data
     * @param config notification configuration
     *               @return true if successul
     */
    public boolean postNotification(String trigger,Map executionData,Map config);
}
