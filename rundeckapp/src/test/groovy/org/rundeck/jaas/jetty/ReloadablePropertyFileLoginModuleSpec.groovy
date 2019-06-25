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
package org.rundeck.jaas.jetty

import spock.lang.Specification

import javax.security.auth.Subject


class ReloadablePropertyFileLoginModuleSpec extends Specification {

    def "Ensure that only role principals are returned in getUserInfo"() {
        setup:
        File tmpFile = File.createTempFile("realm",".properties")
        tmpFile << "testuser:test,one,two"

        when:
        ReloadablePropertyFileLoginModule module = new ReloadablePropertyFileLoginModule()
        module.initialize(new Subject(), null, [:], ["file":tmpFile.absolutePath])
        def userInfo = module.getUserInfo("testuser")

        then:
        userInfo.roleNames == ["one","two"]

        cleanup:
        tmpFile.delete()
    }

}
