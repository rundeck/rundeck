package webhooks

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import org.rundeck.data.Exists
import org.rundeck.data.webhook.WebhookEntity

@Client("http://localhost:8080/webhook")
@Header(name="X-Tenant",value="rundeck")
interface WebhookServiceClient {

    @Get("/findByProject/{project}")
    List<WebhookEntity> listByProject(String project)

    @Get("/findByAuthToken/{token}")
    WebhookEntity findByAuthToken(String token)

    @Get("/findByUuid/{uuid}")
    WebhookEntity findByUuid(String uuid)

    @Post("/e/")
    WebhookEntity createWebhook(@Body WebhookEntity entity)

    @Get("/e/{id}")
    WebhookEntity getWebhook(String id)

    @Post("/e/{id}")
    WebhookEntity updateWebhook(@Body WebhookEntity entity)

    @Get("/exists/{project}/{name}")
    Exists existsWebhookNameInProject(String name, String project)
}
