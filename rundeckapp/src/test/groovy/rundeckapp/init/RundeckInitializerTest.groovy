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
package rundeckapp.init

import org.springframework.core.io.ClassPathResource
import spock.lang.Specification

import java.nio.file.Path


class RundeckInitializerTest extends Specification {
    def "Initialize"() {
    }

    def "CreateDirectories"() {
    }

    def "ExpandTemplates"() {
        given:
        File destination = new File(File.createTempFile("test-et","zzz").parentFile,"expandTest")

        Properties testTemplateProperties = new Properties()

        RundeckInitConfig cfg = new RundeckInitConfig()
        cfg.runtimeConfiguration = testTemplateProperties
        RundeckInitializer initializer = new RundeckInitializer(cfg)

        when:
        initializer.expandTemplates(testTemplateProperties,destination,false)

        then:
        assert destination.exists()
        int filecount
        destination.traverse(type: groovy.io.FileType.FILES) { filecount++ }
        assert filecount == 6
    }

    def "TemplateResourceTest"() {
        def expectedTemplates = [
                "config/realm.properties.template",
                "config/jaas-loginmodule.conf.template",
                "config/rundeck-config.properties.template",
                "config/ssl.properties.template",
                "exp/webapp/WEB-INF/classes/log4j.properties.template",
                "sbin/rundeckd.template"
        ]
        def foundTemplates = []
        when:
        def templateDir = new ClassPathResource("templates")

        Path dirPath = templateDir.file.toPath()
        templateDir.file.traverse(type: groovy.io.FileType.FILES, nameFilter: ~/.*\.template/) {
            foundTemplates.add(dirPath.relativize(it.toPath()).toString())
        }

        then:
        assert templateDir != null
        assert expectedTemplates.size() == foundTemplates.size()
        expectedTemplates.each { assert foundTemplates.contains(it) }
    }
}
