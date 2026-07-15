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
package org.rundeck.security

import grails.events.bus.EventBusAware
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import rundeck.services.ConfigurationService
import rundeck.services.UserService

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@CompileStatic
class RundeckPreauthSuccessEventHandler implements AuthenticationSuccessHandler, EventBusAware {
    private static final Logger LOG = LoggerFactory.getLogger(RundeckPreauthSuccessEventHandler)
    public static final String DEFAULT_FIRST_NAME_HDR   = "X-Forwarded-User-FirstName"
    public static final String DEFAULT_LAST_NAME_HDR    = "X-Forwarded-User-LastName"
    public static final String DEFAULT_EMAIL_HDR        = "X-Forwarded-User-Email"
    ConfigurationService configurationService

    @Override
    void onAuthenticationSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication
    ) throws IOException, ServletException {
        if(configurationService.getBoolean("security.authorization.preauthenticated.userSyncEnabled",false)) {
            String username = authentication.principal instanceof UserDetails ? ((UserDetails)authentication.principal).username : request.getRemoteUser()
            String firstName = request.getHeader(
                    configurationService.getString(
                            "security.authorization.preauthenticated.userFirstNameHeader",
                            DEFAULT_FIRST_NAME_HDR
                    )
            )
            String lastName = request.getHeader(
                    configurationService.getString(
                            "security.authorization.preauthenticated.userLastNameHeader",
                            DEFAULT_LAST_NAME_HDR
                    )
            )
            String email = request.getHeader(
                    configurationService.getString(
                            "security.authorization.preauthenticated.userEmailHeader",
                            DEFAULT_EMAIL_HDR
                    )
            )
            if(LOG.isDebugEnabled()) {
                LOG.debug("Updating user profile from preauthenticated headers for user: ${username}")
                LOG.debug("First Name: " + firstName)
                LOG.debug("Last Name: " + lastName)
                LOG.debug("Email: " + email)
            }
            eventBus.
                notify(
                    UserService.G_EVENT_LOGIN_PROFILE_CHANGE,
                    new UserService.UserProfileData(
                        username: username,
                        lastName: lastName,
                        firstName: firstName,
                        email: email
                    )
                )
        }
    }
}
