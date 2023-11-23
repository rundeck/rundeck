package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.webhook.RdWebhook;
import org.rundeck.app.data.model.v1.webhook.dto.SaveWebhookRequest;
import org.rundeck.app.data.model.v1.webhook.dto.SaveWebhookResponse;
import org.rundeck.spi.data.DataAccessException;

import java.util.List;

public interface WebhookDataProvider extends DataProvider {
    RdWebhook getWebhook(Long id);
    RdWebhook getWebhookByToken(String token);
    RdWebhook getWebhookWithProject(Long id, String project);
    RdWebhook getWebhookByUuid(String uuid);
    RdWebhook findByUuidAndProject(String uuid, String project);
    RdWebhook findByName(String name);
    List<RdWebhook> findAllByProject(String project);
    List<RdWebhook> findAllByNameAndProjectAndUuidNotEqual(String name, String project, String Uuid);
    Integer countByAuthToken(String authToken);
    Integer countByNameAndProject(String name, String project);
    void delete(Long id) throws DataAccessException;
    void deleteByUuid(String uuid) throws DataAccessException;
    /**
     * Retrieves SaveWebhookResponse with the result of the webhook creation
     *
     * @param saveWebhookRequest of the webhook to be created
     * @return A SaveWebhookResponse with the result of the webhook creation
     */
    SaveWebhookResponse createWebhook(SaveWebhookRequest saveWebhookRequest) throws DataAccessException;
    /**
     * Retrieves SaveWebhookResponse with the result of the webhook update
     *
     * @param saveWebhookRequest of the webhook to be updated
     * @return A SaveWebhookResponse with the result of the webhook update
     */
    SaveWebhookResponse updateWebhook(SaveWebhookRequest saveWebhookRequest) throws DataAccessException;

}
