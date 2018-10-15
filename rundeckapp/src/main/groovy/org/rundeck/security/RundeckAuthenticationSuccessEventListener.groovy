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

import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.jaas.JaasAuthenticationToken
import org.springframework.security.authentication.jaas.event.JaasAuthenticationSuccessEvent
import rundeck.services.UserService
import com.dtolabs.rundeck.jetty.jaas.JettyCachingLdapLoginModule

import javax.security.auth.Subject


class RundeckAuthenticationSuccessEventListener implements ApplicationListener<JaasAuthenticationSuccessEvent> {

    UserService userService
    def grailsApplication

    @Override
    void onApplicationEvent(final JaasAuthenticationSuccessEvent event) {
        if(grailsApplication.config.rundeck.security.syncLdapUser?.toBoolean() == true) {
            println "in sync"
            Subject subject = ((JaasAuthenticationToken) event.authentication).loginContext.subject
            subject.principals.each {
                println it.name
            }
            def lastNamePrincipal = subject.principals.find {
                it instanceof JettyCachingLdapLoginModule.LdapLastNamePrincipal
            }
            def firstNamePrincipal = subject.principals.find {
                it instanceof JettyCachingLdapLoginModule.LdapFirstNamePrincipal
            }
            def emailPrincipal = subject.principals.find {
                it instanceof JettyCachingLdapLoginModule.LdapEmailPrincipal
            }
            userService.syncUserDemographicsFromLdap(
                    event.authentication.principal,
                    lastNamePrincipal?.name,
                    firstNamePrincipal?.name,
                    emailPrincipal?.name
            )
        }
    }
}
