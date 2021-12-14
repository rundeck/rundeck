package webhooks

import org.rundeck.data.Pageable
import org.rundeck.data.webhook.WebhookDataService
import org.rundeck.data.webhook.WebhookEntity
import org.springframework.beans.factory.annotation.Autowired
import webhooks.Webhook
import webhooks.data.WebhookGormDataService

class GormWebhookDataService implements WebhookDataService {

    @Autowired
    WebhookGormDataService webhookGormDataService

    @Override
    WebhookEntity get(Serializable id) {
        return webhookGormDataService.get(id)
    }

    @Override
    WebhookEntity findByUuid(String uuid) {
        return webhookGormDataService.findByUuid(uuid)
    }

    @Override
    Long countByAuthToken(String authToken) {
        return webhookGormDataService.countByAuthToken(authToken)
    }

    @Override
    boolean existsWebhookNameInProject(String name, String project) {
        return webhookGormDataService.countByNameAndProject(name,project) > 0
    }

    @Override
    WebhookEntity findByAuthToken(String authToken) {
        return webhookGormDataService.findByAuthToken(authToken)
    }

    @Override
    WebhookEntity save(WebhookEntity entity) {
        return webhookGormDataService.save(Webhook.fromWebhookEntity(entity))
    }

    @Override
    List<WebhookEntity> listByProject(String project, Pageable pageable) {
        return webhookGormDataService.findAllByProject(project,[offset:pageable.offset,max:pageable.max])
    }

    @Override
    void delete(Serializable id) {
        webhookGormDataService.delete(id)
    }

    @Override
    void deleteAllForProject(String project) {
        Webhook.findAllByProject(project).each { webhook ->
            delete(webhook)
        }
    }
}
