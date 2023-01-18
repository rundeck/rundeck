package org.rundeck.app.data.model.v1.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rundeck.app.data.model.v1.webhook.RdWebhook;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaveWebhookResponse {
    RdWebhook webhook;
    boolean isSaved;
    Object errors;
}
