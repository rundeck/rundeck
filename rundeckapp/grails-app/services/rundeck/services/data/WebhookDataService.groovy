package rundeck.services.data

import grails.gorm.services.Service
import webhooks.Webhook

@Service(Webhook)
interface WebhookDataService {

    Webhook save(Webhook webhook)
    void delete(Long id)

}