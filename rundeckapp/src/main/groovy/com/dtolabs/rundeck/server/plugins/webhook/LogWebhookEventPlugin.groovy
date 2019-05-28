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
package com.dtolabs.rundeck.server.plugins.webhook

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Plugin(name = "log-webhook-event",service= ServiceNameConstants.WebhookEvent)
@PluginDescription(title="Log Webhook Events",description = "Can be used to log any incoming webhook events")
class LogWebhookEventPlugin implements WebhookEventPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(LogWebhookEventPlugin)

    @PluginProperty(title="Supplemental Text")
    String supplementalText

    @Override
    void onEvent(final WebhookData data) {
        //LOG.info("Webhook Event: ${data.webhook} ${data.project} ${data.timestamp} ${data.sender}")
        //LOG.info(data.data.text)
        println("Webhook Event: ${data.webhook} ${data.project} ${data.timestamp} ${data.sender}")
        println "supplemental text: ${supplementalText}"
        println(data.data.text)
    }
}
