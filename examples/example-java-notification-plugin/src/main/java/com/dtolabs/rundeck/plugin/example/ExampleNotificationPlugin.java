package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import java.util.*;

@Plugin(service="Notification",name="example")
@PluginDescription(title="Example Plugin", description="An example Plugin for Rundeck Notifications.")
public class ExampleNotificationPlugin implements NotificationPlugin{

    @PluginProperty(name = "example",title = "Example String",description = "Example description")
    private String example;

    public ExampleNotificationPlugin(){

    }

    public boolean postNotification(String trigger, Map executionData, Map config) {
        System.err.printf("Trigger %s fired for %s, configuration: %s\n",trigger,executionData,config);
        System.err.printf("Local field example is: %s\n",example);
        return true;
    }

}