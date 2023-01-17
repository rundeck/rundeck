package org.rundeck.app.data.providers

import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import groovy.util.logging.Slf4j
import org.rundeck.app.data.model.v1.webhook.RdWebhook;
import org.rundeck.app.data.providers.v1.WebhookDataProvider
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.data.WebhookDataService
import webhooks.Webhook

import javax.transaction.Transactional;

@GrailsCompileStatic
@Slf4j
@Transactional
class GormWebhookDataProvider implements WebhookDataProvider {

    @Autowired
    WebhookDataService webhookDataService

    @Override
    RdWebhook get(Long id){
        return Webhook.get(id);
    }

    @Override
    Webhook getWebhookByToken (String token) {
        return Webhook.findByAuthToken(token)
    }

    @Override
    Webhook getWebhook(Long id){
        return Webhook.get(id)
    }

    @Override
    Webhook getWebhookWithProject(Long id, String project) {
        return Webhook.findByIdAndProject(id,project)
    }

    @Override
    Webhook getWebhookByUuid(String uuid) {
        return Webhook.findByUuid(uuid)
    }

    @Override
    Webhook buildWebhook() {
        return new Webhook();
    }

    @Override
    Webhook findByUuidAndProject(String uuid, String project) {
        return Webhook.findByUuidAndProject(uuid, project);
    }

    @Override
    Webhook findByName(String name) {
        return Webhook.findByName(name);
    }

    @Override
    List<Webhook> findAllByProject(String project) {
        return Webhook.findAllByProject(project);
    }

    @Override
    List<Webhook> findAllByNameAndProjectAndUuidNotEqual(String name, String project, String uuid) {
        return Webhook.findAllByNameAndProjectAndUuidNotEqual(name, project, uuid);
    }

    @Override
    Integer countByAuthToken(String authToken) {
        return Webhook.countByAuthToken(authToken);
    }

    @Override
    Integer countByNameAndProject(String name, String project) {
        return Webhook.countByNameAndProject(name, project);
    }

    @Override
    void delete(Long id){
        webhookDataService.delete(id);
    }

}
