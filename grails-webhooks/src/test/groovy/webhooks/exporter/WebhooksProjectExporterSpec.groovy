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
    def "Export - no auth tokens"() {
        given:
        ZipBuilder zipBuilder = new ZipBuilder()
        WebhooksProjectExporter exporter = new WebhooksProjectExporter()
        exporter.webhookService = Mock(MockWebhookService) {
            listWebhooksByProject(_) >> {
                [
                        [name:"Wh1",project:"webhook",uuid:"3d51b2b4-0d81-465a-af1d-392feea901a2",user:"admin",creator:"admin",roles:"admin,user",authToken:"abc12345",eventPlugin:"log-webhook-event",enabled:true,config:[:]],
                        [name:"JobHook",project:"webhook",uuid:"5bc363af-2766-4357-9bc9-0f452bb3ccbf",user:"admin",creator:"admin",roles:"admin,user",authToken:"abc12345",eventPlugin:"webhook-run-job",enabled:false,config:["jobId":"ae210c4c-8c9a-45ef-9916-b9dbe0b0336d"]]
                ]
            }
        }

        when:
        exporter.export("Test", zipBuilder, options)

        then:
        zipBuilder.fileName == "webhooks.yaml"
        zipBuilder.writer.toString().trim() == TEST_OUTPUT_NO_TOKENS
        where:
        options << [
            [(WebhooksProjectExporter.INLUDE_AUTH_TOKENS): 'false'],
            [(WebhooksProjectExporter.INLUDE_AUTH_TOKENS): ''],
            [:],
        ]
    }

    def "Export - with auth tokens"() {
        given:
        ZipBuilder zipBuilder = new ZipBuilder()
        WebhooksProjectExporter exporter = new WebhooksProjectExporter()
        exporter.webhookService = Mock(MockWebhookService) {
            listWebhooksByProject(_) >> {
                [
                        [name:"Wh1",project:"webhook",uuid:"3d51b2b4-0d81-465a-af1d-392feea901a2",user:"admin",creator:"admin",roles:"admin,user",authToken:"abc12345",eventPlugin:"log-webhook-event",enabled:true,config:[:]],
                        [name:"JobHook",project:"webhook",uuid:"5bc363af-2766-4357-9bc9-0f452bb3ccbf",user:"admin",creator:"admin",roles:"admin,user",authToken:"xyz12345",eventPlugin:"webhook-run-job",enabled:false,config:["jobId":"ae210c4c-8c9a-45ef-9916-b9dbe0b0336d"]]
                ]
            }
        }

        when:
        exporter.export("Test", zipBuilder, [(WebhooksProjectExporter.INLUDE_AUTH_TOKENS): 'true'])

        then:
        zipBuilder.fileName == "webhooks.yaml"
        zipBuilder.writer.toString().trim() == TEST_OUTPUT_WITH_TOKENS
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

    private static final String TEST_OUTPUT_NO_TOKENS = """webhooks:
- {uuid: 3d51b2b4-0d81-465a-af1d-392feea901a2, name: Wh1, project: webhook, eventPlugin: log-webhook-event,
  config: '{}', enabled: true, user: admin, roles: 'admin,user'}
- {uuid: 5bc363af-2766-4357-9bc9-0f452bb3ccbf, name: JobHook, project: webhook, eventPlugin: webhook-run-job,
  config: '{"jobId":"ae210c4c-8c9a-45ef-9916-b9dbe0b0336d"}', enabled: false, user: admin,
  roles: 'admin,user'}"""

    private static final String TEST_OUTPUT_WITH_TOKENS = """webhooks:
- {uuid: 3d51b2b4-0d81-465a-af1d-392feea901a2, name: Wh1, project: webhook, eventPlugin: log-webhook-event,
  config: '{}', enabled: true, user: admin, roles: 'admin,user', authToken: abc12345}
- {uuid: 5bc363af-2766-4357-9bc9-0f452bb3ccbf, name: JobHook, project: webhook, eventPlugin: webhook-run-job,
  config: '{"jobId":"ae210c4c-8c9a-45ef-9916-b9dbe0b0336d"}', enabled: false, user: admin,
  roles: 'admin,user', authToken: xyz12345}"""
}
