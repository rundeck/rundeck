package com.dtolabs.rundeck.server.plugins.notification

import com.dtolabs.rundeck.plugins.notification.NotificationPlugin

/**
 * Created with IntelliJ IDEA.
 * User: greg
 * Date: 4/11/13
 * Time: 6:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestNotificationPlugin implements NotificationPlugin{
    boolean postNotification(String trigger, Map executionData, Map config) {
        System.err.println("Trigger ${trigger} fired for ${executionData}, configuration: ${config}")
        true
    }

    public Map getConfigurationProperties() {
        return [test:[type:'String',title:'Test String']]
    }

    public String renderHtmlForm(String inputPrefix, Map config) {
        """
<label>Test input:
<input type="text" name="${inputPrefix}test">
</label>
"""
    }

    public Map validateForm(Map config) {
        return null
    }
}
