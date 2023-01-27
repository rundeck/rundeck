package org.rundeck.app.data.providers

import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import groovy.util.logging.Slf4j
import org.rundeck.app.data.model.v1.webhook.RdWebhook
import org.rundeck.app.data.model.v1.webhook.dto.SaveWebhookRequest
import org.rundeck.app.data.model.v1.webhook.dto.SaveWebhookResponse;
import org.rundeck.app.data.providers.v1.WebhookDataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.validation.Errors
import rundeck.services.data.WebhookDataService
import webhooks.Webhook

import javax.transaction.Transactional;

@GrailsCompileStatic
@Slf4j
@Transactional
class GormWebhookDataProvider implements WebhookDataProvider {

    @Autowired
    WebhookDataService webhookDataService
    @Autowired
    MessageSource messageSource

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

    @Override
    SaveWebhookResponse createWebhook(SaveWebhookRequest saveWebhookRequest) {
        Webhook hook = new Webhook(uuid: saveWebhookRequest.uuid, name: saveWebhookRequest.name,
                project: saveWebhookRequest.project, authToken: saveWebhookRequest.authToken,
                authConfigJson: saveWebhookRequest.authConfigJson, eventPlugin: saveWebhookRequest.eventPlugin,
                pluginConfigurationJson: saveWebhookRequest.pluginConfigurationJson,
                enabled: saveWebhookRequest.enabled);

        Boolean isUpdated = hook.save(flush: true)
        String errors = hook.errors.allErrors.collect { messageSource.getMessage(it,null) }.join(",")
        return new SaveWebhookResponse(webhook: hook, isSaved: isUpdated, errors: errors);
    }

    @Override
    SaveWebhookResponse updateWebhook(SaveWebhookRequest saveWebhookRequest) {
        Webhook hook = getWebhook(saveWebhookRequest.id)
        hook.setUuid(saveWebhookRequest.uuid)
        hook.setName(saveWebhookRequest.name)
        hook.setProject(saveWebhookRequest.project)
        hook.setAuthToken(saveWebhookRequest.authToken)
        hook.setAuthConfigJson(saveWebhookRequest.authConfigJson)
        hook.setEventPlugin(saveWebhookRequest.eventPlugin)
        hook.setPluginConfigurationJson(saveWebhookRequest.pluginConfigurationJson)
        hook.setEnabled(saveWebhookRequest.enabled)
        Boolean isUpdated = hook.save(flush: true)
        String errors = hook.errors.allErrors.collect { messageSource.getMessage(it,null) }.join(",")
        return new SaveWebhookResponse(webhook: hook, isSaved: isUpdated, errors: errors);
    }
}
