package org.rundeck.app.data.model.v1.webhook;

public interface RdWebhook {
    String getUuid();
    String getName();
    String getProject();
    String getAuthToken();
    String getAuthConfigJson();
    String getEventPlugin();
    String getPluginConfigurationJson();
    boolean getEnabled();

}
