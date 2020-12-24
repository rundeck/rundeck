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
package webhooks.importer

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import spock.lang.Specification

class WebhooksProjectImporterSpec extends Specification {

    def "doImport"() {
        given:
        WebhooksProjectImporter importer = new WebhooksProjectImporter()
        importer.webhookService = Mock(MockWebhookService)
        def authContext = Mock(UserAndRolesAuthContext)
        def files=['webhooks.yaml':new File(getClass().getClassLoader().getResource("webhooks.yaml").toURI())]
        when:
        def errors = importer.doImport(authContext, "webhook", files, [regenAuthTokens: false])

        then:
        errors.isEmpty()
        2 * importer.webhookService.importWebhook(_,_,_) >> {
            [msg:"ok"]
        }
    }

    static interface MockWebhookService {
        def importWebhook(UserAndRolesAuthContext authContext, def hookData, boolean regenFlag)
    }
}
