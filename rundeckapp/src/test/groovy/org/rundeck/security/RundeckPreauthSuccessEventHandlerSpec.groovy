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

import grails.events.bus.EventBus
import org.springframework.security.core.Authentication
import rundeck.services.ConfigurationService
import rundeck.services.UserService
import spock.lang.Specification
import spock.lang.Unroll

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse

class RundeckPreauthSuccessEventHandlerSpec extends Specification {
    @Unroll
    def "OnAuthenticationSuccess"() {
        setup:
        def username = 'auser'
        RundeckPreauthSuccessEventHandler handler = new RundeckPreauthSuccessEventHandler()
        handler.configurationService = Mock(ConfigurationService) {
            getBoolean("security.authorization.preauthenticated.userSyncEnabled",false) >> syncEnabled
            getString("security.authorization.preauthenticated.userFirstNameHeader",RundeckPreauthSuccessEventHandler.DEFAULT_FIRST_NAME_HDR) >> { firstNameHdr ?: RundeckPreauthSuccessEventHandler.DEFAULT_FIRST_NAME_HDR }
            getString("security.authorization.preauthenticated.userLastNameHeader",RundeckPreauthSuccessEventHandler.DEFAULT_LAST_NAME_HDR) >> { lastNameHdr ?: RundeckPreauthSuccessEventHandler.DEFAULT_LAST_NAME_HDR }
            getString("security.authorization.preauthenticated.userEmailHeader",RundeckPreauthSuccessEventHandler.DEFAULT_EMAIL_HDR) >> { emailHdr ?: RundeckPreauthSuccessEventHandler.DEFAULT_EMAIL_HDR }
        }
        handler.targetEventBus = Mock(EventBus)
        Authentication auth = Mock(Authentication) {
            getPrincipal() >> new RundeckUserDetailsService.RundeckUserDetails(username, "role1,role2")
        }
        HttpServletResponse rsp = Stub(HttpServletResponse)
        HttpServletRequest rq = new HdrTesterHttpServletRequest(hdrs,Stub(HttpServletRequest))

        when:
        handler.onAuthenticationSuccess(rq,rsp,auth)

        then:
            userSvcCall * handler.eventBus.notify(
                UserService.G_EVENT_LOGIN_PROFILE_CHANGE,
                {
                    it.username == username
                    it.lastName == expectedLast
                    it.firstName == expectedFirst
                    it.email == expectedEmail
                }
            )

        where:
        userSvcCall | syncEnabled | firstNameHdr | lastNameHdr | emailHdr       | expectedFirst | expectedLast | expectedEmail  | hdrs
        0           | false       | null         | null        | null           | null          | null         | null           | [:]
        1           | true        | null         | null        | null           | "bob"         | "thebuilder" | "bob@build.it" | [(RundeckPreauthSuccessEventHandler.DEFAULT_FIRST_NAME_HDR):expectedFirst,(RundeckPreauthSuccessEventHandler.DEFAULT_LAST_NAME_HDR):expectedLast,(RundeckPreauthSuccessEventHandler.DEFAULT_EMAIL_HDR):expectedEmail]
        1           | true        | "custFirst"  | "custLast"  | "custEmail"    | "joe"         | "theplumber" | "joe@plumb.it" | ["custFirst":expectedFirst,"custLast":expectedLast,"custEmail":expectedEmail]
    }

    class HdrTesterHttpServletRequest extends HttpServletRequestWrapper {

        Map<String,String> headers = [:]

        HdrTesterHttpServletRequest(Map<String,String> headers, HttpServletRequest rq) {
            super(rq)
            this.headers = headers
        }

        @Override
        String getHeader(final String name) {
            return headers[name]
        }
        HdrTesterHttpServletRequest(final HttpServletRequest request) {
            super(request)
        }
    }
}
