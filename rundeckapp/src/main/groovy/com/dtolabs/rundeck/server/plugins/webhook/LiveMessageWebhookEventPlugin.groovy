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

import com.dtolabs.rundeck.core.live.LiveEventData
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.webhook.WebhookEventException
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.plugins.webhook.WebhookEventContext
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import rundeck.services.LiveEventBusProviderService

@Plugin(name = "Live Message Sender",service= ServiceNameConstants.WebhookEvent)
class LiveMessageWebhookEventPlugin implements WebhookEventPlugin {

    @PluginProperty(title ="Live Channel",description = "The destination channel for the message")
    String channel

    @Override
    void onEvent(final WebhookEventContext context, final WebhookData data) throws WebhookEventException {
        LiveEventBusProviderService eventBusProvider = context.services.getService(LiveEventBusProviderService)
        LiveEventData<String> liveEvent = new LiveEventData<>(channel,data.data.text)
        eventBusProvider.eventBus.post(liveEvent)
    }
}
