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
    void setUuid(String uuid);
    void setName(String name);
    void setProject(String project);
    void setAuthToken(String authTpken);
    void setAuthConfigJson(String authConfigJson);
    void setEventPlugin(String eventPlugin);
    void setPluginConfigurationJson(String plugingConfigurationJson);
    void setEnabled(boolean enabled);
}
