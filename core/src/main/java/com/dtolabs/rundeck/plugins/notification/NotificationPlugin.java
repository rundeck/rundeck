package com.dtolabs.rundeck.plugins.notification;

import java.util.Map;

/**
 * ${CLASSNAME} is ...
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
     */
    public boolean postNotification(String trigger,Map executionData,Map config);
    public Map getConfigurationProperties();
    public Map validateForm(Map config);
}
