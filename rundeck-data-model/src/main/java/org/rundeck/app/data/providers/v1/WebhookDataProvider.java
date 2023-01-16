package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.webhook.RdWebhook;

import java.util.List;

public interface WebhookDataProvider extends DataProvider {
    RdWebhook get(Long id);
    RdWebhook getWebhook(Long id);
    RdWebhook getWebhookByToken(String token);
    RdWebhook getWebhookWithProject(Long id, String project);
    RdWebhook getWebhookByUuid(String uuid);
    RdWebhook buildWebhook();
    RdWebhook findByUuidAndProject(String uuid, String project);
    List<RdWebhook> findAllByProject(String project);
    List<RdWebhook> findAllByNameAndProjectAndUuidNotEqual(String name, String project, String Uuid);
    Integer countByAuthToken(String authToken);
    Integer countByNameAndProject(String name, String project);
    void delete(Long id);
}
