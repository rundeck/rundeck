package org.rundeck.core.executions.provenance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class WebhookProvenance
        implements Provenance<WebhookProvenance.WebhookData>
{
    private final WebhookData data;

    WebhookProvenance(final WebhookData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Webhook " + data.webhookUUID;
    }


    @Getter
    @RequiredArgsConstructor
    static class WebhookData {
        private final String webhookUUID;

    }


    public static WebhookProvenance from(String webhookUUID) {
        return new WebhookProvenance(new WebhookData(webhookUUID));
    }

    public static WebhookProvenance from(com.dtolabs.rundeck.plugins.webhook.WebhookData data) {
        return new WebhookProvenance(new WebhookData(data.getWebhookUUID()));
    }
}
