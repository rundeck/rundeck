package webhooks.authenticator

import webhooks.Webhook
import javax.servlet.http.HttpServletRequest

interface WebhookAuthenticator {
    boolean authenticate(Webhook webhook, HttpServletRequest request)
}