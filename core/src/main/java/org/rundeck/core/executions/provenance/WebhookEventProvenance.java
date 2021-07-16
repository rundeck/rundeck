package org.rundeck.core.executions.provenance;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class WebhookEventProvenance  implements Provenance<WebhookEventProvenance.EventData>{
    private final EventData data;

    WebhookEventProvenance(final EventData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Webhook Event " + data.id;
    }


    @Getter
    @RequiredArgsConstructor
    static class EventData {
        private final String id;

    }


    public static WebhookEventProvenance from(String id) {
        return new WebhookEventProvenance(new WebhookEventProvenance.EventData(id));
    }

    public static WebhookEventProvenance from(com.dtolabs.rundeck.plugins.webhook.WebhookData data) {
        return new WebhookEventProvenance(new WebhookEventProvenance.EventData(data.getId()));
    }
}
