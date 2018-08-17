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

import grails.util.Environment
import spock.lang.Specification
import spock.lang.Unroll

class RundeckInitializerTest extends Specification {

    def "ExpandTemplates"() {
        given:
        File sourceTemplates = new File(System.getProperty("user.dir"),"templates")
        File destination = new File(File.createTempFile("test-et","zzz").parentFile,"expandTest-"+UUID.randomUUID().toString().substring(0,8))

        Properties testTemplateProperties = new Properties()

        RundeckInitConfig cfg = new RundeckInitConfig()
        cfg.runtimeConfiguration = testTemplateProperties
        RundeckInitializer initializer = new RundeckInitializer(cfg)

        int sourceFileCount = 0
        sourceTemplates.traverse(type: groovy.io.FileType.FILES) { sourceFileCount++ }

        when:
        initializer.expandTemplatesNonJarMode(sourceTemplates,testTemplateProperties,destination,false)

        then:
        assert destination.exists()
        int filecount
        destination.traverse(type: groovy.io.FileType.FILES) { filecount++ }
        assert filecount == sourceFileCount
    }

    def "copy to destination and expand templates log4j file war mode"() {
        given:
        File sourceTemplates = new File(System.getProperty("user.dir"),"templates")
        File destination = new File(File.createTempFile("test-et","zzz").parentFile,"expandTest-"+UUID.randomUUID().toString().substring(0,8))
        File warWebAppDir = new File(File.createTempFile("webappWar","zzz").parentFile,"expandTest-"+UUID.randomUUID().toString().substring(0,8))

        destination.mkdir()
        warWebAppDir.mkdir()

        Properties testTemplateProperties = new Properties()

        RundeckInitConfig cfg = new RundeckInitConfig()
        cfg.runtimeConfiguration = testTemplateProperties
        RundeckInitializer initializer = new RundeckInitializer(cfg)
        initializer.thisJar = warWebAppDir
        File log4Jfile = new File(sourceTemplates,"config/log4j.properties.template")
        Environment.metaClass.static.isWarDeployed = { return true }

        when:
        initializer.copyToDestinationAndExpandProperties(destination,sourceTemplates.toPath(),log4Jfile,testTemplateProperties,false)

        then:
        warWebAppDir.exists()
        int filecount
        warWebAppDir.traverse(type: groovy.io.FileType.FILES) { filecount++ }
        filecount == 1
    }

    def "ensure rundeck config has log4j path commented war mode"() {
        given:
        File sourceTemplates = new File(System.getProperty("user.dir"),"templates")
        File destination = new File(File.createTempFile("test-et","zzz").parentFile,"expandTest-"+UUID.randomUUID().toString().substring(0,8))

        destination.mkdir()

        Properties testTemplateProperties = new Properties()

        RundeckInitConfig cfg = new RundeckInitConfig()
        cfg.runtimeConfiguration = testTemplateProperties
        RundeckInitializer initializer = new RundeckInitializer(cfg)
        
        File rcprops = new File(sourceTemplates,"config/rundeck-config.properties.template")
        Environment.metaClass.static.isWarDeployed = { return true }

        when:
        initializer.copyToDestinationAndExpandProperties(destination,sourceTemplates.toPath(),rcprops,testTemplateProperties,false)
        List<String> rcpropsContents = new File(destination,"config/rundeck-config.properties").readLines()
        then:
        rcpropsContents.findAll { it.startsWith("#rundeck.log4j.config.file")}
    }

    @Unroll
    def "translate legacy system property #origProp to grails 3 compatible settings"() {
        given:
        System.setProperty("rundeck.config.location","config")
        Properties testTemplateProperties = new Properties()
        RundeckInitConfig cfg = new RundeckInitConfig()
        cfg.cliOptions.baseDir = "base"
        cfg.cliOptions.configDir = "config"
        cfg.cliOptions.serverBaseDir = "server"
        cfg.runtimeConfiguration = testTemplateProperties
        RundeckInitializer initializer = new RundeckInitializer(cfg)
        initializer.thisJar = File.createTempFile("this","jar")
        initializer.initConfigurations()
        !System.getProperty(origProp)

        when:
        System.setProperty(origProp, val)
        initializer.setSystemProperties()

        then:
        System.getProperty(newProp) == val

        where:
        origProp                            | newProp                    | val
        'server.http.port'                  | 'server.port'              | '4441'
        'server.http.host'                  | 'server.host'              | 'hostoverride'
        'server.http.host'                  | 'server.address'           | 'hostoverride'
        'server.web.context'                | 'server.contextPath'       | '/rundeck'
        'rundeck.jetty.connector.forwarded' | 'server.useForwardHeaders' | 'true'
    }
}
