package webhooks.authenticator

import com.fasterxml.jackson.databind.ObjectMapper
import webhooks.Webhook
import javax.servlet.http.HttpServletRequest

class AuthorizationHeaderAuthenticator implements WebhookAuthenticator {
    static final ObjectMapper mapper = new ObjectMapper()
    @Override
    boolean authenticate(Webhook webhook, HttpServletRequest request) {
        String hdrValue = request.getHeader("Authorization")
        if(!hdrValue) return false
        Config config = mapper.readValue(webhook.authConfigJson, Config)
        return config.secret == hdrValue.sha256()
    }

    static class Config {
        String secret
    }
}
