/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

import grails.boot.GrailsApp
import org.springframework.boot.WebApplicationType
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.StandardEnvironment
import rundeckapp.Application
import spock.lang.Specification

/**
 * Regression tests for RUN-4996 / https://github.com/rundeck/rundeck/issues/10352: a
 * grails.serverURL set only in a custom rundeck-config.properties/.groovy file (via
 * -Drundeck.config.location) was invisible to any bean that reads config eagerly at bean-definition
 * build time (e.g. the default grailsLinkGenerator bean), because that registration previously only
 * happened from Application#setEnvironment(), an ordinary EnvironmentAware callback that fires too
 * late -- only once Application itself is instantiated as a bean, which is after Grails has already
 * built its plugin bean definitions.
 */
class RundeckConfigEnvironmentPostProcessorTest extends Specification {

    def "postProcessEnvironment registers the same property sources as Application#setEnvironment()"() {
        given: "a controlled rundeckConfig, so this test doesn't trigger a real, uncontrolled runPrebootstrap() pass (which mutates JVM-wide state) depending on which specs happened to run before it"
        def previousRundeckConfig = Application.rundeckConfig
        Application.rundeckConfig = new RundeckInitConfig()
        Properties runtimeProps = new Properties()
        runtimeProps.setProperty(RundeckInitializer.PROP_REALM_LOCATION, "fake")
        runtimeProps.setProperty(RundeckInitializer.PROP_LOGINMODULE_NAME, "fake")
        Application.rundeckConfig.runtimeConfiguration = runtimeProps
        StandardEnvironment environment = new StandardEnvironment()
        def postProcessor = new RundeckConfigEnvironmentPostProcessor()

        when:
        postProcessor.postProcessEnvironment(environment, null)

        then:
        List<String> propertiesLoaded = environment.propertySources.iterator().collect { it.name }
        propertiesLoaded.contains("hardcoded-rundeck-props")
        propertiesLoaded.contains("rundeck.config.location")

        cleanup:
        Application.rundeckConfig = previousRundeckConfig
    }

    /**
     * This is the real, load-bearing test: it drives an actual GrailsApp (a real
     * org.springframework.boot.SpringApplication subclass -- the same class Application's own
     * execRunApp() uses to boot in production) through Spring Boot's genuine startup sequence, up
     * through environment preparation, then aborts before any ApplicationContext or bean definition
     * is created. If RundeckConfigEnvironmentPostProcessor is correctly discovered (via
     * META-INF/spring.factories) and invoked during that phase, grails.serverURL from a custom
     * rundeck.config.location properties file must already be visible on the environment at this
     * point -- proving the property is available before Grails' plugin bean-definition-building
     * phase (which runs later, during ApplicationContext refresh) could possibly read it.
     */
    def "grails.serverURL from a custom rundeck.config.location properties file is visible before any ApplicationContext is created"() {
        given: "a rundeck-config.properties file with a custom grails.serverURL"
        File tmpCfgDir = File.createTempDir()
        tmpCfgDir.deleteOnExit()
        File tmpProp = new File(tmpCfgDir, "rundeck-config.properties")
        tmpProp << "grails.serverURL=https://myhost.example.com:8443\n"
        tmpProp.deleteOnExit()
        String previousLocation = System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION, tmpProp.absolutePath)
        // ReloadableRundeckPropertySource is a static, JVM-wide singleton that loads its file once
        // at class-init time; force it to pick up this test's file rather than whatever it may have
        // already cached from an earlier test run in the same JVM. refreshRundeckPropertyFile() only
        // clears/repopulates its backing Properties when the config location is set and isn't a
        // .groovy file, so a plain reload() in cleanup can't be trusted to undo this -- snapshot the
        // live contents now and restore them verbatim in cleanup instead.
        Properties previousRundeckProps = new Properties()
        previousRundeckProps.putAll(ReloadableRundeckPropertySource.getRundeckPropertySourceInstance().source as Properties)
        ReloadableRundeckPropertySource.reload()

        and: "a controlled rundeckConfig, so this doesn't trigger a real, uncontrolled runPrebootstrap() pass depending on which specs happened to run before it"
        def previousRundeckConfig = Application.rundeckConfig
        Application.rundeckConfig = new RundeckInitConfig()
        Properties runtimeProps = new Properties()
        runtimeProps.setProperty(RundeckInitializer.PROP_REALM_LOCATION, "fake")
        runtimeProps.setProperty(RundeckInitializer.PROP_LOGINMODULE_NAME, "fake")
        Application.rundeckConfig.runtimeConfiguration = runtimeProps

        and: "a listener that captures the environment the instant it's prepared, then aborts before any bean-definition work happens"
        // A plain closure coerced with `as ApplicationListener<...>` does NOT preserve the generic
        // event type at runtime (Groovy's interface coercion erases it), and Spring's event
        // multicaster relies on reflecting that generic to decide which events to deliver -- so a
        // coerced closure actually receives *every* ApplicationEvent, not just the declared one.
        // An explicit typed class preserves the generic signature correctly. It holds its own field
        // (rather than closing over a local variable) to avoid any ambiguity about whether a
        // mutation from inside the listener would be visible to the outer test method.
        def captor = new EnvironmentCaptor()

        GrailsApp app = new GrailsApp(Application)
        app.setWebApplicationType(WebApplicationType.NONE)
        app.addListeners(captor)

        when: "the real Spring Boot startup sequence runs, up through environment preparation"
        try {
            app.run()
        } catch (Throwable ignoredAbort) {
            // expected: we deliberately abort right after environment preparation, before any
            // ApplicationContext/bean-definition work -- which is both unnecessary for this specific
            // ordering claim and far slower to actually run to completion.
        }

        then: "grails.serverURL from the custom config-location file was already visible -- proving RundeckConfigEnvironmentPostProcessor ran, and ran before any bean definitions could have been built"
        captor.capturedEnvironment != null
        captor.capturedEnvironment.getProperty("grails.serverURL") == "https://myhost.example.com:8443"

        cleanup:
        if (previousLocation != null) {
            System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION, previousLocation)
        } else {
            System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)
        }
        ReloadableRundeckPropertySource.reload()
        // Belt-and-braces: reload() above may leave this test's values cached (see the comment
        // where previousRundeckProps was captured), so restore the exact snapshot regardless.
        Properties liveRundeckProps = ReloadableRundeckPropertySource.getRundeckPropertySourceInstance().source as Properties
        liveRundeckProps.clear()
        liveRundeckProps.putAll(previousRundeckProps)
        Application.rundeckConfig = previousRundeckConfig
    }

    /**
     * Explicit typed class (not a closure coerced to the interface) so Spring's event multicaster
     * can correctly reflect its generic ApplicationListener<ApplicationEnvironmentPreparedEvent>
     * type parameter to filter events -- see the comment where this is instantiated.
     */
    static class EnvironmentCaptor implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
        ConfigurableEnvironment capturedEnvironment

        @Override
        void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
            capturedEnvironment = event.environment
            throw new RuntimeException("test: abort intentionally right after environment preparation")
        }
    }
}
