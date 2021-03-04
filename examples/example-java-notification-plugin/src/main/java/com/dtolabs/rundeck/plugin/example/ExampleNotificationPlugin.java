package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.core.plugins.configuration.DynamicProperties;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants;
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption;
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import org.rundeck.app.spi.Services;

import java.util.*;

@Plugin(service = "Notification", name = "example")
@PluginDescription(title = "Example Plugin", description = "An example Plugin for Rundeck Notifications.")
public class ExampleNotificationPlugin
        implements NotificationPlugin, DynamicProperties
{

    @PluginProperty(name = "example", title = "Example String", description = "Example description")
    private String example;

    @PluginProperty(name = "customFields", title = "Custom Fields",  required = false)
    @RenderingOptions({
                              @RenderingOption(
                                      key = StringRenderingConstants.DISPLAY_TYPE_KEY,
                                      value = "DYNAMIC_FORM"
                              )
                      })
    private String customFields;

    public ExampleNotificationPlugin() {

    }

    public boolean postNotification(String trigger, Map executionData, Map config) {
        System.err.printf("Trigger %s fired for %s, configuration: %s\n", trigger, executionData, config);
        System.err.printf("Local field example is: %s\n", example);
        return true;
    }


    @Override
    public Map<String, Object> dynamicProperties(
            final Map<String, Object> projectAndFrameworkValues,
            Services services
    )
    {
        Map<String, Object> list = new LinkedHashMap<>();

        Map<String, String> fields = new TreeMap<>();
        fields.put("test1", "atest");
        fields.put("test2", "another");

        list.put("customFields", fields);


        return list;
    }
}
