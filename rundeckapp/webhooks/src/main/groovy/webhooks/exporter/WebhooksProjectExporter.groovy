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
package webhooks.exporter

import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.core.projects.ProjectDataExporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor


class WebhooksProjectExporter implements ProjectDataExporter {
    private static final Logger logger = LoggerFactory.getLogger(WebhooksProjectExporter)
    private static final ObjectMapper mapper = new ObjectMapper()
    public static final String INLUDE_AUTH_TOKENS = 'inludeAuthTokens'
    public static final String WEBHOOKS_YAML_FILE = "webhooks.yaml"

    def webhookService

    final String name = 'webhooks'
    final List<Property> exportProperties = [
        PropertyBuilder.builder().
            booleanType(INLUDE_AUTH_TOKENS).
            title('Include Webhook Auth Tokens').
            description('If not included, tokens will be regenerated upon import.').
            build()
    ]

    @Override
    void export(String project, def zipBuilder, Map<String, String> exportOptions) {
        logger.info("Project Webhook export running")
        Yaml yaml = new Yaml(new SafeConstructor())
        def export = [webhooks:[]]
        webhookService.listWebhooksByProject(project).each { hk ->
            logger.debug("exporting hook: " + hk.name)
            def data = [uuid:hk.uuid,
                        name:hk.name,
                        project:hk.project,
                        eventPlugin: hk.eventPlugin,
                        config: mapper.writeValueAsString(hk.config),
                        enabled: hk.enabled,
                        user: hk.user,
                        roles: hk.roles
            ]

            if (exportOptions[INLUDE_AUTH_TOKENS] == 'true') {
                data.authToken = hk.authToken
            }
            export.webhooks.add(data)
        }
        zipBuilder.file(WEBHOOKS_YAML_FILE) { writer ->
            yaml.dump(export,writer)
        }
    }

}
