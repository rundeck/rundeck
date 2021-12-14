package webhooks.data

import grails.gorm.services.Service
import webhooks.Webhook

@Service(Webhook)
interface WebhookGormDataService {
    Webhook get(Serializable id)
    List<Webhook> findAllByProject(String project, Map params)
    Webhook findByAuthToken(String authToken)
    Webhook findByUuid(String authToken)

}
