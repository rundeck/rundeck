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

import grails.plugins.GrailsPluginManager
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import java.lang.reflect.Field


class RundeckExtendedMessageBundleTest extends Specification {

    @TempDir
    File temporaryDirectory

    def "Test Rundeck message bundle extender"() {
        given:
        PluginAwareResourceBundleMessageSource messageSource = new PluginAwareResourceBundleMessageSource(null,null)
        Field baseNameField = messageSource.getClass().getSuperclass().getDeclaredField("basenames")
        baseNameField.setAccessible(true)

        when:
        RundeckExtendedMessageBundle extender = new RundeckExtendedMessageBundle(messageSource,"file:/tmp/i18n/messages")
        def baseNames = baseNameField.get(messageSource).toList()

        then:
        baseNames[0] == "file:/tmp/i18n/messages"

    }

    def "Test Rundeck message bundle extender does not extend when external bundle ref is null"() {
        given:
        PluginAwareResourceBundleMessageSource messageSource = new PluginAwareResourceBundleMessageSource(null,null)
        Field baseNameField = messageSource.getClass().getSuperclass().getDeclaredField("basenames")
        baseNameField.setAccessible(true)

        when:
        RundeckExtendedMessageBundle extender = new RundeckExtendedMessageBundle(messageSource,null)
        def baseNames = baseNameField.get(messageSource).toList()

        then:
        baseNames.size() == 0

    }

    /**
     * The genuine production key existed before the POC with {@code Create}
     * in messages.properties and {@code Crear} in messages_es_419.properties.
     */
    @Unroll
    def "Core default resolves #scenario"() {
        given:
        File originalDirectory = new File(temporaryDirectory, "original")
        File publicDirectory = new File(temporaryDirectory, "public")
        File administratorDirectory = new File(temporaryDirectory, "administrator")
        [originalDirectory, publicDirectory, administratorDirectory]*.mkdirs()
        writeBundle(originalDirectory, "messages.properties", legacyDefault)
        writeBundle(originalDirectory, "messages_es_419.properties", legacyLocalized)
        writeBundle(publicDirectory, "public-messages.properties", canonicalDefault)
        writeBundle(publicDirectory, "public-messages_es_419.properties", canonicalLocalized)
        writeBundle(administratorDirectory, "messages.properties", administrator)

        PluginAwareResourceBundleMessageSource messageSource =
                new PluginAwareResourceBundleMessageSource(
                        null,
                        Mock(GrailsPluginManager) {
                            getAllPlugins() >> []
                        }
                )
        messageSource.setDefaultEncoding("UTF-8")
        messageSource.setBasenames("file:${originalDirectory.absolutePath}/messages")

        when:
        new RundeckExtendedMessageBundle(
                messageSource,
                "file:${administratorDirectory.absolutePath}/messages",
                "file:${publicDirectory.absolutePath}/public-messages",
                "file:${originalDirectory.absolutePath}/messages"
        )

        then:
        messageSource.getMessage(
                "default.button.create.label",
                null,
                Locale.forLanguageTag("es-419")
        ) == expected

        where:
        scenario                                     | administrator | canonicalLocalized | legacyLocalized | canonicalDefault | legacyDefault || expected
        "administrator override above every bundle" | "Admin Create" | "Canonical Crear"   | "Crear"         | "Create"         | "Create"      || "Admin Create"
        "canonical localized before legacy localized"| null           | "Canonical Crear"   | "Crear"         | "Create"         | "Create"      || "Canonical Crear"
        "legacy localized before canonical default" | null           | null                | "Crear"         | "Create"         | "Create"      || "Crear"
        "canonical default before legacy default"   | null           | null                | null            | "Create"         | "Legacy Create" || "Create"
        "legacy default as final fallback"           | null           | null                | null            | null             | "Create"      || "Create"
    }

    private static void writeBundle(File directory, String name, String value) {
        if (value != null) {
            new File(directory, name).text = "default.button.create.label=${value}\n"
        }
    }
}
