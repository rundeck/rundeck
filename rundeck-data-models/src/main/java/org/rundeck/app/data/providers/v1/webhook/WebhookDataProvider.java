/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rundeck.app.data.providers.v1.webhook;

import org.rundeck.app.data.model.v1.webhook.RdWebhook;
import org.rundeck.app.data.model.v1.webhook.dto.SaveWebhookRequest;
import org.rundeck.app.data.model.v1.webhook.dto.SaveWebhookResponse;
import org.rundeck.app.data.providers.v1.DataProvider;
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
     * @throws DataAccessException if an error occurs
     */
    SaveWebhookResponse createWebhook(SaveWebhookRequest saveWebhookRequest) throws DataAccessException;
    /**
     * Retrieves SaveWebhookResponse with the result of the webhook update
     *
     * @param saveWebhookRequest of the webhook to be updated
     * @return A SaveWebhookResponse with the result of the webhook update
     * @throws DataAccessException if an error occurs
     */
    SaveWebhookResponse updateWebhook(SaveWebhookRequest saveWebhookRequest) throws DataAccessException;

}
