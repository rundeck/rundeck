package org.rundeck.app.data.model.v1.webhook;

public interface RdWebhook {
    Long getId();
    String getUuid();
    String getName();
    String getProject();
    String getAuthToken();
    String getAuthConfigJson();
    String getEventPlugin();
    String getPluginConfigurationJson();
    boolean getEnabled();
}
