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

import java.lang.reflect.Field


class RundeckExtendedMessageBundleTest extends Specification {

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
}
