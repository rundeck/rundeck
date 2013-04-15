package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import com.dtolabs.rundeck.core.plugins.Plugin;
import java.util.*;

@Plugin(service="Notification",name="example")
public class ExampleNotificationPlugin implements NotificationPlugin{

    public ExampleNotificationPlugin(){

    }

    public boolean postNotification(String trigger, Map executionData, Map config) {
        System.err.printf("Trigger %s fired for %s, configuration: %s",trigger,executionData,config);
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

    public String renderHtmlForm(String inputPrefix, Map config) {
        return 
"<label>Test input:\n"+
"<input type=\"text\" name=\""+inputPrefix+"test\">\n"+
"</label>";
    }

    public Map validateForm(Map config) {
        return null;
    }
}