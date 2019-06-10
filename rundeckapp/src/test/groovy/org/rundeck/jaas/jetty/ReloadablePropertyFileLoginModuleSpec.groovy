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

    def "Check reloadable"() {
        setup:
        File tmpFile = File.createTempFile("realm",".properties")
        tmpFile << "testuser:test,one,two\n"
        ReloadablePropertyFileLoginModule module = new ReloadablePropertyFileLoginModule()

        when:
        module.setReloadEnabled(reloadEnabled)
        module.initialize(new Subject(),null,[:],["file":tmpFile.absolutePath,"refreshInterval":"2"])
        Thread.sleep(500)
        tmpFile << "reloaduser2:test,agroup\n"
        def attempt1 = module.getUserInfo("reloaduser2")
        Thread.sleep(7500)
        def attempt2 = module.getUserInfo("reloaduser2")

        then:
        attempt1 == attempt1Value
        attempt2?.userName == attempt2Value

        cleanup:
        tmpFile.delete()

        where:
        reloadEnabled | attempt1Value | attempt2Value
        true          | null          | "reloaduser2"
        false          | null         | null
    }

//    def "Check reload disabled"() {
//        setup:
//        File tmpFile = File.createTempFile("realm",".properties")
//        tmpFile << "testuser:test,one,two"
//        ReloadablePropertyFileLoginModule module = new ReloadablePropertyFileLoginModule()
//
//        when:
//        module.setReloadEnabled(false)
//        module.initialize(new Subject(),null,[:],["file":tmpFile.absolutePath,"refreshInterval":"6"])
//        Thread.sleep(500)
//        tmpFile << "reloduser2:test,agroup"
//        def attempt1 = module.getUserInfo("reloaduser2")
//        Thread.sleep(7500)
//        def attempt2 = module.getUserInfo("reloaduser2")
//
//        then:
//        !attempt1
//        attempt2
//
//        cleanup:
//        tmpFile.delete()
//    }
}
