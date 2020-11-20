/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.app.gui

import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import grails.web.mapping.LinkGenerator
import org.rundeck.app.gui.AuthMenuItem
import org.rundeck.core.auth.AuthConstants
import org.springframework.beans.factory.annotation.Autowired


class UserSummaryMenuItem implements AuthMenuItem {
    @Autowired
    LinkGenerator grailsLinkGenerator

    @Override
    MenuType getType() {
        return MenuType.SYSTEM_CONFIG
    }

    @Override
    String getTitleCode() {
        return "gui.menu.Users"
    }

    @Override
    String getTitle() {
        return "User Summary"
    }

    @Override
    String getHref() {
        return grailsLinkGenerator.link(
                action: "userSummary",
                controller: "menu"
        )
    }

    @Override
    boolean isEnabled(final UserAndRolesAuthContext auth) {
        return auth.evaluate(AuthorizationUtil.resourceType('system'),
                             AuthConstants.ACTION_ADMIN,
                             AuthorizationUtil.RUNDECK_APP_ENV)
                            .authorized
    }
}
