package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.webhook.RdWebhook;

public interface WebhookDataProvider extends DataProvider {
    RdWebhook get(Long id);
    RdWebhook getWebhook(Long id);
    RdWebhook getWebhookByToken(String token);
    RdWebhook getWebhookWithProject(Long id, String project);
    RdWebhook getWebhookByUuid(String uuid);
}
