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
package webhooks

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.data.webhook.WebhookEntity

class Webhook extends WebhookEntity {
    static ObjectMapper mapper = new ObjectMapper()
    @JsonIgnore
    String pluginConfigurationJson

    static constraints = {
        uuid(nullable: true)
        name(nullable: false)
        project(nullable: false)
        authToken(nullable: false)
        eventPlugin(nullable: false)
    }

    static transients = ['pluginConfiguration']
    static mapping = {
        pluginConfigurationJson type: 'text'
    }

    static String cleanAuthToken(String authtoken) {
        if(authtoken.contains("#")) return authtoken.substring(0,authtoken.indexOf("#"))
        return authtoken
    }

    static Webhook fromWebhookEntity(WebhookEntity e) {
        Webhook w = e.uuid ? Webhook.get(e.id) : new Webhook()
        if(!w) w = new Webhook()
        w.name = e.name
        w.uuid = e.uuid
        w.enabled = e.enabled
        w.project = e.project
        w.authToken = e.authToken
        w.eventPlugin = e.eventPlugin
        w.projectConfiguration = e.pluginConfiguration
        w
    }
}
