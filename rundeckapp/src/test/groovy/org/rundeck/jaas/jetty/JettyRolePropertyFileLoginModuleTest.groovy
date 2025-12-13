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
package org.rundeck.jaas.jetty

import org.rundeck.jaas.PropertyFileLoginModule
import spock.lang.Specification

import javax.security.auth.Subject


class JettyRolePropertyFileLoginModuleTest extends Specification {
    def "Initialize without hotReload set"() {
        given: "A temporary properties file"
        def tempFile = File.createTempFile("test-realm", ".properties")
        tempFile.deleteOnExit()
        tempFile.text = "testuser:testpass,user"
        
        when:
        JettyRolePropertyFileLoginModule module = new JettyRolePropertyFileLoginModule()
        module.initialize(new Subject(), null, [:], [useFirstPass:"true", file: tempFile.absolutePath])

        then:
        !module.module.isReloadEnabled()
    }

    def "Initialize hotReload set to true"() {
        given: "A temporary properties file"
        def tempFile = File.createTempFile("test-realm", ".properties")
        tempFile.deleteOnExit()
        tempFile.text = "testuser:testpass,user"
        
        when:
        JettyRolePropertyFileLoginModule module = new JettyRolePropertyFileLoginModule()
        module.initialize(new Subject(),null,[:],[useFirstPass:"true",hotReload:"true", file: tempFile.absolutePath])

        then:
        module.module.isReloadEnabled()
    }
}
