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

import groovy.transform.CompileStatic
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import rundeckapp.Application

/**
 * Registers Rundeck's own config-file-derived property sources (rundeck-config.properties or
 * rundeck-config.groovy, plus a couple of hardcoded/derived properties -- see
 * {@link Application#loadRundeckPropertySources(org.springframework.core.env.Environment)}) into the
 * Spring {@link ConfigurableEnvironment} as early as possible in the Spring Boot startup sequence:
 * during environment preparation ({@code SpringApplication.run()}'s {@code prepareEnvironment()}
 * step), which completes entirely before any {@code ApplicationContext} is created and therefore
 * before any bean definitions -- Grails plugin or otherwise -- exist.
 * <br>
 * <b>Why this matters</b> (RUN-4996 / https://github.com/rundeck/rundeck/issues/10352): Grails
 * plugin Spring DSL code (e.g. {@code grails-app/conf/spring/resources.groovy}, and Grails' own
 * framework plugins such as the one that constructs the default {@code grailsLinkGenerator} bean)
 * reads config values like {@code grails.serverURL} <i>eagerly</i>, as plain Groovy/Java code
 * executed while building bean <i>definitions</i> -- which happens during
 * {@code ApplicationContext} refresh, specifically during bean-factory-post-processing, which runs
 * before ordinary singleton beans (including {@link Application} itself) are instantiated.
 * {@link Application} previously only registered these property sources from its
 * {@code EnvironmentAware#setEnvironment(Environment)} callback, which -- being an ordinary
 * {@code Aware} callback -- fires only once {@link Application} is itself instantiated as a bean,
 * i.e. well <i>after</i> Grails has already built (and evaluated the config reads inside) its plugin
 * bean definitions. A {@code grails.serverURL} set only in {@code rundeck-config.properties} or
 * {@code rundeck-config.groovy} was therefore invisible to any bean that reads config this way,
 * regardless of file format -- manifesting as e.g. notification email links losing their host and
 * scheme, since link generation for an async, request-less notification send has no live HTTP
 * request to fall back on for that information.
 * <br>
 * {@link Application#setEnvironment(org.springframework.core.env.Environment)} still separately
 * performs the same registration (by calling this same
 * {@link Application#loadRundeckPropertySources(org.springframework.core.env.Environment)} method)
 * for backward compatibility -- re-adding an identically-named {@code PropertySource} is a safe,
 * idempotent no-op ({@code MutablePropertySources#addFirst} replaces any existing entry with the same
 * name) -- so this class is purely additive: it does not change what gets registered, only ensures it
 * also happens early enough for early-reading beans to see it.
 */
@CompileStatic
class RundeckConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (Application.rundeckConfig == null) {
            Application.runPrebootstrap()
        }
        Application.loadRundeckPropertySources(environment)
    }

    @Override
    int getOrder() {
        // Run after Spring Boot's own config-file processors (application.yml/.properties, etc.,
        // handled by ConfigDataEnvironmentPostProcessor and similar) have added their property
        // sources, so Rundeck's config-location file still takes precedence via addFirst() --
        // matching the precedence setEnvironment() already established for the later registration.
        return Ordered.LOWEST_PRECEDENCE
    }
}
