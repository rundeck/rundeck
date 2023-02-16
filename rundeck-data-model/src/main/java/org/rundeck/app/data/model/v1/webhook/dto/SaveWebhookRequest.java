package org.rundeck.app.data.model.v1.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaveWebhookRequest {
    Long id;
    String uuid;
    String name;
    String project;
    String authToken;
    String authConfigJson;
    String eventPlugin;
    String pluginConfigurationJson;
    boolean enabled = true;
    String roles;
}
