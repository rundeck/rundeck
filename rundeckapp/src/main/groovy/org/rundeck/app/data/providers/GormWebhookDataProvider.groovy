package org.rundeck.app.data.providers

import grails.compiler.GrailsCompileStatic
import groovy.util.logging.Slf4j;
import org.rundeck.app.data.model.v1.webhook.RdWebhook;
import org.rundeck.app.data.providers.v1.WebhookDataProvider
import webhooks.Webhook

import javax.transaction.Transactional;

@GrailsCompileStatic
@Slf4j
@Transactional
public class GormWebhookDataProvider implements WebhookDataProvider {

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
    Webhook getWebhookWithProject(Long id, String project){
        return Webhook.findByIdAndProject(id,project)
    }

    @Override
    Webhook getWebhookByUuid(String uuid) {
        return Webhook.findByUuid(uuid)
    }
}
