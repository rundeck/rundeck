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
        propertiesLoaded.size() == 2
        propertiesLoaded.contains("rundeck.config.location")
        propertiesLoaded.contains("rundeck-config-groovy")

    }

    def "load rundeck-config.properties if set in system property RDECK_CONFIG_LOCATION"() {

        when:
        File tmpProp = File.createTempFile("app-test",".properties")
        tmpProp << "myprop=avalue"
        Application app = new Application()
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION,tmpProp.absolutePath)
        Properties props = app.loadRundeckPropertyFile()

        then:
        props.get("myprop") == "avalue"

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

    def "do not try to load rundeck-config.groovy if set in system property RDECK_CONFIG_LOCATION"() {

        when:
        File tmpGroovy = File.createTempFile("app-test",".groovy")
        tmpGroovy << "grails { mail {} } "
        Application app = new Application()
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION,tmpGroovy.absolutePath)
        Properties props = app.loadRundeckPropertyFile()

        then:
        props.isEmpty()

        cleanup:
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)
    }

    def "RunPreboostrap"() {
        when:
        Application.runPreboostrap()
        then:
        System.getProperty("TestPreboostrap") == "ran successfully"
    }

    def "LoadRundeckPropertyFile - check config initted"() {
        setup:
        Properties p1 = new Properties()
        p1.setProperty("prop1","val1")
        File tmpFile = File.createTempFile("tmp","properties")
        tmpFile.deleteOnExit()
        tmpFile.withPrintWriter { w -> p1.store(w,"") }
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION,tmpFile.absolutePath)
        if(configInitted) System.setProperty("rundeck.config.initted", "true")

        when:
        Application app = new Application()
        Properties rdkProps = app.loadRundeckPropertyFile()

        then:
        rdkProps.getProperty("prop1") == expectedProp1Val

        where:
        configInitted   | expectedProp1Val
        false           | "val1"
        true            | null

    }

    @Unroll
    def "LoadGroovyRundeckConfigIfExists - check config initted: #configInitted"() {
        setup:
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION,"")
        File tdir = File.createTempDir()
        File tmpFile = new File(tdir,"rundeck-config.groovy")
        tmpFile.deleteOnExit()
        tmpFile << "prop1 = val1"
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR,tdir.absolutePath)
        if(configInitted) System.setProperty("rundeck.config.initted", "true")
        else System.clearProperty("rundeck.config.initted")

        when:
        Application app = new Application()
        TestEnvironment tenv = new TestEnvironment()
        app.loadGroovyRundeckConfigIfExists(tenv)
        boolean propSourceExists = tenv.propertySources.find { it.name == "rundeck-config-groovy"} != null

        then:
        propSourceExists == shouldHavePropSource

        where:
        configInitted   | shouldHavePropSource
        false           | true
        true            | false
    }

    class TestEnvironment extends StandardEnvironment {
        MutablePropertySources propertySources = new MutablePropertySources()

        @Override
        public MutablePropertySources getPropertySources() {
            return this.propertySources;
        }

    }

}
