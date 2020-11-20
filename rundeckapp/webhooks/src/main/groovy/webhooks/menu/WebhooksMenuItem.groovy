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
package webhooks.menu

import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.web.mapping.LinkGenerator
import groovy.transform.CompileStatic
import org.rundeck.app.gui.AuthMenuItem
import org.rundeck.app.gui.MenuItem
import org.springframework.beans.factory.annotation.Autowired
import webhooks.WebhookConstants

@CompileStatic
class WebhooksMenuItem implements MenuItem, AuthMenuItem {
    String title = "Webhooks"
    String titleCode = "Webhooks.title"
    MenuType type = MenuType.PROJECT

    @Autowired
    LinkGenerator grailsLinkGenerator
    @Autowired
    AuthContextEvaluator rundeckAuthContextEvaluator

    @Override
    String getProjectHref(final String project) {
        return grailsLinkGenerator.link(
                action: "admin",
                controller: "webhook",
                plugin: "webhooks",
                params: [project: project]
        )
    }

    @Override
    boolean isEnabled(final UserAndRolesAuthContext auth, final String project) {
        return rundeckAuthContextEvaluator.
            authorizeProjectResourceAny(
                auth,
                WebhookConstants.RESOURCE_TYPE_WEBHOOK,
                [WebhookConstants.ACTION_READ],
                project
            )
    }
}
