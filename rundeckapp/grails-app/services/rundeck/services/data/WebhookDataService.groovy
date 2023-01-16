package rundeck.services.data

import grails.gorm.services.Service
import webhooks.Webhook

@Service(Webhook)
interface WebhookDataService {

    void delete(Long id)
}