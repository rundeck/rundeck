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

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.core.projects.ProjectDataExporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml


class WebhooksProjectExporter implements ProjectDataExporter {
    private static final Logger logger = LoggerFactory.getLogger(WebhooksProjectExporter)
    private static final ObjectMapper mapper = new ObjectMapper()

    def webhookService

    @Override
    String getSelector() {
        return "webhooks"
    }

    @Override
    void export(String project, def zipBuilder) {
        logger.info("Project Webhook export running")
        Yaml yaml = new Yaml()
        def export = [webhooks:[]]
        webhookService.listWebhooksByProject(project).each { hk ->
            logger.debug("exporting hook: " + hk.name)
            def data = [name:hk.name,project:hk.project,eventPlugin: hk.eventPlugin,pluginConfiguration: mapper.writeValueAsString(hk.config)]
            data.apiToken = [token:hk.authToken,user:hk.user,creator:hk.creator,roles:hk.roles]
            export.webhooks.add(data)
        }
        zipBuilder.file("webhooks.yaml") { writer ->
            yaml.dump(export,writer)
        }
    }

}
