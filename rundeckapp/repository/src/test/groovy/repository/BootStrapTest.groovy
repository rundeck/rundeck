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
package repository

import grails.core.GrailsApplication
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

import javax.servlet.ServletContext


class BootStrapTest extends Specification implements GrailsUnitTest {

    def "init with sync enabled"() {

        setup:
        grailsApplication.config.rundeck.feature.repository.enabled=true
        grailsApplication.config.rundeck.feature.repository.syncOnBootstrap=true

        when:
        BootStrap bootStrap = new BootStrap()
        bootStrap.grailsApplication = grailsApplication
        bootStrap.repositoryPluginService = Mock(RepositoryPluginService)
        1 * bootStrap.repositoryPluginService.syncInstalledArtifactsToPluginTarget()
        bootStrap.init(Stub(ServletContext))

        then:
        true

    }

    def "init with sync disabled"() {

        setup:
        grailsApplication.config.rundeck.feature.repository.enabled=true
        grailsApplication.config.rundeck.feature.repository.syncOnBootstrap=false

        when:
        BootStrap bootStrap = new BootStrap()
        bootStrap.grailsApplication = grailsApplication
        bootStrap.repositoryPluginService = Mock(RepositoryPluginService)
        0 * bootStrap.repositoryPluginService.syncInstalledArtifactsToPluginTarget()
        bootStrap.init(Stub(ServletContext))

        then:
        true

    }

    def "init with repository disabled"() {

        setup:
        grailsApplication.config.rundeck.feature.repository.enabled=false
        grailsApplication.config.rundeck.feature.repository.syncOnBootstrap=true

        when:
        BootStrap bootStrap = new BootStrap()
        bootStrap.grailsApplication = grailsApplication
        bootStrap.repositoryPluginService = Mock(RepositoryPluginService)
        0 * bootStrap.repositoryPluginService.syncInstalledArtifactsToPluginTarget()
        bootStrap.init(Stub(ServletContext))

        then:
        true

    }
}
