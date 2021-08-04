package org.rundeck.core.executions.provenance;

import lombok.*;

@Getter

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WebhookProvenance
        implements Provenance<WebhookProvenance.WebhookData>
{
    private WebhookData data;

    @Override
    public String toString() {
        return "Webhook " + data.webhookUUID;
    }


    @Getter

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    static class WebhookData {
        private String webhookUUID;

    }


    public static WebhookProvenance from(String webhookUUID) {
        return new WebhookProvenance(new WebhookData(webhookUUID));
    }

    public static WebhookProvenance from(com.dtolabs.rundeck.plugins.webhook.WebhookData data) {
        return new WebhookProvenance(new WebhookData(data.getWebhookUUID()));
    }
}
