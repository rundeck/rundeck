/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

import com.dtolabs.rundeck.jetty.jaas.LdapEmailPrincipal
import com.dtolabs.rundeck.jetty.jaas.LdapFirstNamePrincipal
import com.dtolabs.rundeck.jetty.jaas.LdapLastNamePrincipal
import grails.events.bus.EventBusAware
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.jaas.JaasAuthenticationToken
import org.springframework.security.authentication.jaas.event.JaasAuthenticationSuccessEvent
import rundeck.services.ConfigurationService
import rundeck.services.UserService

import javax.security.auth.Subject


class RundeckJaasAuthenticationSuccessEventListener implements ApplicationListener<JaasAuthenticationSuccessEvent>,
    EventBusAware {
    private static final Logger LOG = LoggerFactory.getLogger(RundeckJaasAuthenticationSuccessEventListener)
    ConfigurationService configurationService

    @Override
    void onApplicationEvent(final JaasAuthenticationSuccessEvent event) {
        try {
            if (configurationService.getBoolean('security.syncLdapUser', false)) {
                Subject subject = ((JaasAuthenticationToken) event.authentication).loginContext.subject

                String username = event.authentication.principal.toString()
                def lastNamePrincipal = subject.principals.find {
                    it instanceof LdapLastNamePrincipal
                }
                def firstNamePrincipal = subject.principals.find {
                    it instanceof LdapFirstNamePrincipal
                }
                def emailPrincipal = subject.principals.find {
                    it instanceof LdapEmailPrincipal
                }
                LOG.debug("Updating user profile from ldap attributes for user: ${username}")
                LOG.debug("First Name: " + firstNamePrincipal?.name)
                LOG.debug("Last Name: " + lastNamePrincipal?.name)
                LOG.debug("Email: " + emailPrincipal?.name)

                eventBus.
                    notify(
                        UserService.G_EVENT_LOGIN_PROFILE_CHANGE,
                        new UserService.UserProfileData(
                            username: username,
                            lastName: lastNamePrincipal?.name,
                            firstName: firstNamePrincipal?.name,
                            email: emailPrincipal?.name
                        )
                    )
            }
        } catch(Exception ex) {
            LOG.error("Unable to update user profile from LDAP.",ex)
        }
    }
}
