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

import spock.lang.Specification


class WebhooksProjectExporterSpec extends Specification {
    def "Export"() {
        given:
        ZipBuilder zipBuilder = new ZipBuilder()
        WebhooksProjectExporter exporter = new WebhooksProjectExporter()
        exporter.webhookService = Mock(MockWebhookService) {
            listWebhooksByProject(_) >> {
                [
                        [name:"Wh1",project:"webhook",user:"admin",creator:"admin",roles:"admin,user",authToken:"abc12345",eventPlugin:"log-webhook-event",config:[:]],
                        [name:"JobHook",project:"webhook",user:"admin",creator:"admin",roles:"admin,user",authToken:"abc12345",eventPlugin:"webhook-run-job",config:["jobId":"ae210c4c-8c9a-45ef-9916-b9dbe0b0336d"]]
                ]
            }
        }

        when:
        exporter.export("Test", zipBuilder)

        then:
        zipBuilder.fileName == "webhooks.yaml"
        zipBuilder.writer.toString().trim() == TEST_OUTPUT
    }

    class ZipBuilder {

        StringWriter writer = new StringWriter()
        String fileName

        void file(String name, Closure cls) {
            this.fileName = name
            cls.call(writer)
        }
    }

    static interface MockWebhookService {
        def listWebhooksByProject(String project)
    }

    private static final String TEST_OUTPUT = """webhooks:
- name: Wh1
  project: webhook
  eventPlugin: log-webhook-event
  pluginConfiguration: '{}'
  apiToken: {token: abc12345, user: admin, creator: admin, roles: 'admin,user'}
- name: JobHook
  project: webhook
  eventPlugin: webhook-run-job
  pluginConfiguration: '{"jobId":"ae210c4c-8c9a-45ef-9916-b9dbe0b0336d"}'
  apiToken: {token: abc12345, user: admin, creator: admin, roles: 'admin,user'}"""
}
