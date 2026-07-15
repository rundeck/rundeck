package webhooks.authenticator

import org.rundeck.app.data.model.v1.webhook.RdWebhook
import jakarta.servlet.http.HttpServletRequest

interface WebhookAuthenticator {
    boolean authenticate(RdWebhook webhook, HttpServletRequest request)
}