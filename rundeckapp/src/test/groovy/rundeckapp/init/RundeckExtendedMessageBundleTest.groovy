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

import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import spock.lang.Specification
import spock.lang.TempDir

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

    def "configured source resolves administrator then public then original per key and locale"() {
        given:
        File originalDirectory = new File(temporaryDirectory, "original")
        File publicDirectory = new File(temporaryDirectory, "public")
        File administratorDirectory = new File(temporaryDirectory, "administrator")
        [originalDirectory, publicDirectory, administratorDirectory]*.mkdirs()
        new File(originalDirectory, "messages.properties").text = '''
translation.poc.conflict=legacy-value
translation.poc.legacy-only=legacy-only-value
translation.poc.locale-fallback=legacy-default-value
'''
        new File(originalDirectory, "messages_fr.properties").text =
                'translation.poc.locale-original-only=legacy-french-value\n'
        new File(publicDirectory, "public-messages.properties").text = '''
translation.poc.conflict=canonical-value
translation.poc.canonical-only=canonical-only-value
'''
        new File(publicDirectory, "public-messages_fr.properties").text =
                'translation.poc.locale-conflict=canonical-french-value\n'
        new File(originalDirectory, "messages_fr.properties") <<
                'translation.poc.locale-conflict=legacy-french-value\n'
        new File(administratorDirectory, "messages.properties").text =
                'translation.poc.conflict=administrator-value\n'

        PluginAwareResourceBundleMessageSource messageSource =
                new PluginAwareResourceBundleMessageSource(null, null)
        messageSource.setDefaultEncoding("UTF-8")
        messageSource.setBasenames("file:${originalDirectory.absolutePath}/messages")

        when:
        new RundeckExtendedMessageBundle(
                messageSource,
                "file:${publicDirectory.absolutePath}/public-messages"
        )
        new RundeckExtendedMessageBundle(
                messageSource,
                "file:${administratorDirectory.absolutePath}/messages"
        )

        then: "the identical key proves administrator precedence through getMessage"
        messageSource.getMessage("translation.poc.conflict", null, Locale.US) ==
                "administrator-value"

        and: "removing the administrator fixture proves canonical precedence over legacy"
        new File(administratorDirectory, "messages.properties").delete()
        messageSource.clearCache()
        messageSource.getMessage("translation.poc.conflict", null, Locale.US) ==
                "canonical-value"

        and: "sparse canonical and legacy-only keys both resolve"
        messageSource.getMessage("translation.poc.canonical-only", null, Locale.US) ==
                "canonical-only-value"
        messageSource.getMessage("translation.poc.legacy-only", null, Locale.US) ==
                "legacy-only-value"

        and: "locale conflicts prefer canonical while absent locale keys fall back"
        messageSource.getMessage("translation.poc.locale-conflict", null, Locale.FRENCH) ==
                "canonical-french-value"
        messageSource.getMessage("translation.poc.locale-original-only", null, Locale.FRENCH) ==
                "legacy-french-value"
        messageSource.getMessage("translation.poc.locale-fallback", null, Locale.FRENCH) ==
                "legacy-default-value"
    }
}
