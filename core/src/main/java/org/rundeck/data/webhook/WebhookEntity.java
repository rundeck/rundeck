package org.rundeck.data.webhook;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class WebhookEntity {
    Serializable id;
    String uuid;
    String name;
    String project;
    String authToken;
    String eventPlugin;
    Map pluginConfiguration;
    boolean enabled = true;
}
