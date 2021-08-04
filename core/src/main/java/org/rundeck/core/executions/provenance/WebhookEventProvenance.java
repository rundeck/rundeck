package org.rundeck.core.executions.provenance;


import lombok.*;

@Getter

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WebhookEventProvenance  implements Provenance<WebhookEventProvenance.EventData>{
    private EventData data;

    @Override
    public String toString() {
        return "Webhook Event " + data.id;
    }


    @Getter

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    static class EventData {
        private String id;

    }


    public static WebhookEventProvenance from(String id) {
        return new WebhookEventProvenance(new WebhookEventProvenance.EventData(id));
    }

    public static WebhookEventProvenance from(com.dtolabs.rundeck.plugins.webhook.WebhookData data) {
        return new WebhookEventProvenance(new WebhookEventProvenance.EventData(data.getId()));
    }
}
