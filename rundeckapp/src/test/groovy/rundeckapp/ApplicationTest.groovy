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
package rundeckapp

import org.springframework.core.env.Environment
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.StandardEnvironment
import rundeckapp.init.RundeckInitConfig
import rundeckapp.init.RundeckInitializer
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class ApplicationTest extends Specification {

    def "setEnvironment with both property file and groovy"() {

        when:
        File tmpCfgDir = File.createTempDir()
        File tmpGroovy = new File(tmpCfgDir, "rundeck-config.groovy")
        tmpGroovy << "grails { mail {} } "
        File tmpProp = File.createTempFile("app-test",".properties")
        tmpProp << "myprop=avalue"
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION,tmpProp.absolutePath)
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR,tmpCfgDir.absolutePath)

        Application.rundeckConfig = new RundeckInitConfig()
        Properties runtimeProps = new Properties()
        runtimeProps.setProperty(RundeckInitializer.PROP_REALM_LOCATION,"fake")
        runtimeProps.setProperty(RundeckInitializer.PROP_LOGINMODULE_NAME,"fake")
        Application.rundeckConfig.runtimeConfiguration = runtimeProps
        Application app = new Application()
        TestEnvironment env = new TestEnvironment()

        app.setEnvironment(env)
        List<String> propertiesLoaded = env.propertySources.iterator().collect { it.name }

        then:
        propertiesLoaded.size() == 3
        propertiesLoaded.contains("hardcoded-rundeck-props")
        propertiesLoaded.contains("rundeck.config.location")
        propertiesLoaded.contains("rundeck-config-groovy")

        cleanup:
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)
    }

    def "setEnvironment adds liquibase migration prop with migration cli option"() {

        when:
        Application.rundeckConfig = new RundeckInitConfig()
        Application.rundeckConfig.cliOptions.migrate = true
        Properties runtimeProps = new Properties()
        runtimeProps.setProperty(RundeckInitializer.PROP_REALM_LOCATION,"fake")
        runtimeProps.setProperty(RundeckInitializer.PROP_LOGINMODULE_NAME,"fake")
        Application.rundeckConfig.runtimeConfiguration = runtimeProps
        Application app = new Application()
        TestEnvironment env = new TestEnvironment()

        app.setEnvironment(env)
        List<String> propertiesLoaded = env.propertySources.iterator().collect { it.name }

        then:
        propertiesLoaded.contains("ensure-migration-flag")

    }

    def "load default rundeck-config.groovy if file exist and RDECK_CONFIG_LOCATION not set"() {

        when:
        File defaultGroovy = File.createTempFile("rundeck-config",".groovy")
        defaultGroovy << "grails { mail {} } "
        Application app = new Application()
        TestEnvironment env = new TestEnvironment()
        app.loadGroovyRundeckConfigIfExists(env)

        List<String> propertiesLoaded = env.propertySources.iterator().collect { it.name }

        then:
        propertiesLoaded.size() == 1
        propertiesLoaded.contains("rundeck-config-groovy")

    }

    def "load my-custom-file.groovy as rundeck-config-groovy if set in system property RDECK_CONFIG_LOCATION"() {

        when:
        File customGroovy = File.createTempFile("my-custom-file",".groovy")
        customGroovy << "grails { customvalue {} } "
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION,customGroovy.absolutePath)

        Application app = new Application()
        TestEnvironment env = new TestEnvironment()
        app.loadGroovyRundeckConfigIfExists(env)

        List<String> propertiesLoaded = env.propertySources.iterator().collect { it.source }

        then:
        propertiesLoaded['grails'][0].toString().compareToIgnoreCase("customvalue")

    }

    def "load my-custom-file.groovy over default rundeck-config.groovy if both exist and RDECK_CONFIG_LOCATION is set"() {

        when:
        File defaultGroovy = File.createTempFile("rundeck-config",".groovy")
        File customGroovy = File.createTempFile("my-custom-file",".groovy")
        defaultGroovy << "grails { mail {} } "
        customGroovy << "grails { customvalue {} } "

        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION,customGroovy.absolutePath)
        Application app = new Application()
        TestEnvironment env = new TestEnvironment()
        app.loadGroovyRundeckConfigIfExists(env)

        List<String> propertiesLoaded = env.propertySources.iterator().collect { it.source }

        then:
        propertiesLoaded['grails'][0].toString().compareToIgnoreCase("customvalue")

        cleanup:
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)

    }

    def "RunPreboostrap"() {
        when:
        Application.runPrebootstrap()
        then:
        System.getProperty("TestPreboostrap") == "ran successfully"
    }

    class TestEnvironment extends StandardEnvironment {
        MutablePropertySources propertySources = new MutablePropertySources()

        @Override
        public MutablePropertySources getPropertySources() {
            return this.propertySources;
        }

    }

}
