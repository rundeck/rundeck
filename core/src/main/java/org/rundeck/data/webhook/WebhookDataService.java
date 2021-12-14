package org.rundeck.data.webhook;

import org.rundeck.data.Pageable;

import java.io.Serializable;
import java.util.List;

public interface WebhookDataService {

    WebhookEntity get(Serializable id);
    WebhookEntity findByUuid(String uuid);
    Long countByAuthToken(String authToken);
    boolean existsWebhookNameInProject(String name, String project);
    WebhookEntity findByAuthToken(String authToken);
    WebhookEntity save(WebhookEntity entity);
    List<WebhookEntity> listByProject(String project, Pageable pageable);
    void delete(Serializable id);
    void deleteAllForProject(String project);
}
