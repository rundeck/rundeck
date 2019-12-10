/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
package webhooks.plugins

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.webhook.WebhookEventException
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.plugins.webhook.WebhookEventContext
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import org.apache.log4j.Logger

@Plugin(name = "log-webhook-event",service= ServiceNameConstants.WebhookEvent)
@PluginDescription(title="Log Events",description = "Logs incoming webhook events to log4j logger 'org.rundeck.webhook.events'")
class LogWebhookEventPlugin implements WebhookEventPlugin {
    private static final Logger LOG = Logger.getLogger("org.rundeck.webhook.events")

    @Override
    void onEvent(final WebhookEventContext context, final WebhookData data) throws WebhookEventException {
        LOG.info("Log Webhook Event Plugin - Webhook event information:")
        LOG.info("id: ${data.id}")
        LOG.info("name: ${data.webhook}")
        LOG.info("project: ${data.project}")
        LOG.info("sender: ${data.sender}")
        LOG.info("contentType: ${data.contentType}")
        LOG.info("data:")
        LOG.info(data.data.text)
    }
}
