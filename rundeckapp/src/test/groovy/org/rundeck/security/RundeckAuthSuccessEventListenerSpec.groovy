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

import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import rundeck.services.FrameworkService
import spock.lang.Specification


class RundeckAuthSuccessEventListenerSpec extends Specification {
    def "OnApplicationEvent firstLoginNotExist"() {
        when:
        File tmpDir = File.createTempDir()
        File testFirstLogin = new File(tmpDir, FrameworkService.FIRST_LOGIN_FILE)
        RundeckAuthSuccessEventListener listener = new RundeckAuthSuccessEventListener()
        listener.frameworkService = Mock(FrameworkService) {
            getFirstLoginFile() >> { testFirstLogin  }
        }
        listener.onApplicationEvent(new AuthenticationSuccessEvent(new TestingAuthenticationToken("test","test")))
        then:
        testFirstLogin.text.startsWith("test")
    }

    def "OnApplicationEvent firstLoginExists"() {
        when:
        String content = "firstlogin:TIMESTAMP"
        File testFirstLogin = File.createTempFile("tmp","exists")
        testFirstLogin << content
        RundeckAuthSuccessEventListener listener = new RundeckAuthSuccessEventListener()
        listener.frameworkService = Mock(FrameworkService) {
            getFirstLoginFile() >> { testFirstLogin  }
        }
        listener.onApplicationEvent(new AuthenticationSuccessEvent(new TestingAuthenticationToken("test","test")))
        then:
        testFirstLogin.text == content
    }
}
