package webhooks

import org.rundeck.data.Pageable
import org.rundeck.data.webhook.WebhookDataService
import org.rundeck.data.webhook.WebhookEntity

import javax.inject.Inject

class ExtWebhookDataService implements WebhookDataService {

    @Inject
    WebhookServiceClient client

    @Override
    WebhookEntity get(Serializable id) {
        return client.getWebhook(id)
    }

    @Override
    WebhookEntity findByUuid(String uuid) {
        return client.getWebhook(uuid)
    }

    @Override
    Long countByAuthToken(String authToken) {
        return null
    }

    @Override
    boolean existsWebhookNameInProject(String name, String project) {
        return client.existsWebhookNameInProject(name,project).exists
    }

    @Override
    WebhookEntity findByAuthToken(String authToken) {
        return client.findByAuthToken(authToken)
    }

    @Override
    WebhookEntity save(WebhookEntity entity) {
        if(entity.getId()) {
            return client.updateWebhook(entity)
        }
        return client.createWebhook(entity)
    }

    @Override
    List<WebhookEntity> listByProject(String project, Pageable pageable) {
        return client.listByProject(project)
    }

    @Override
    void delete(Serializable id) {

    }

    @Override
    void deleteAllForProject(String project) {

    }
}
