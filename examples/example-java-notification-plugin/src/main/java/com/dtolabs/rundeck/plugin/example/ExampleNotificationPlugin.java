package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import java.util.*;

@Plugin(service="Notification",name="example")
@PluginDescription(title="Example Plugin", description="An example Plugin for Rundeck Notifications.")
public class ExampleNotificationPlugin implements NotificationPlugin{

    @PluginProperty(name = "test",title = "Test String",description = "a description")
    private String test;

    public ExampleNotificationPlugin(){

    }

    public boolean postNotification(String trigger, Map executionData, Map config) {
        System.err.printf("Trigger %s fired for %s, configuration: %s\n",trigger,executionData,config);
        System.err.printf("Local field test is: %s\n",test);
        return true;
    }

    public Map getConfigurationProperties() {
        HashMap<String,Object> map = new HashMap<String,Object>(){{
            put("test",new HashMap<String,String>(){{
                put("type","String");
                put("title","Test String");
            }});

        }};
        return map;
    }

    public Map validateForm(Map config) {
        return null;
    }
}