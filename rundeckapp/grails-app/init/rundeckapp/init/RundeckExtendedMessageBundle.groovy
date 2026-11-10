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

import groovy.transform.CompileStatic
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.context.MessageSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Field


@CompileStatic
class RundeckExtendedMessageBundle {
    private static final transient Logger LOG = LoggerFactory.getLogger(RundeckExtendedMessageBundle.class)

    RundeckExtendedMessageBundle(PluginAwareResourceBundleMessageSource messageSource, String externalBase) {
        if(externalBase) {
            PluginAwareResourceBundleMessageSource msgSource = (PluginAwareResourceBundleMessageSource) messageSource
            Field oldBaseNameField = msgSource.getClass().getSuperclass().getDeclaredField("basenames")
            oldBaseNameField.setAccessible(true)
            List<String> oldBaseNames = ((String[]) oldBaseNameField.get(msgSource)).toList()
            oldBaseNames.add(0, externalBase)
            msgSource.setBasenames(oldBaseNames.toArray(new String[oldBaseNames.size()]))
            LOG.debug("adding external i18n message source: ${externalBase}")
        }
    }

    /**
     * Configures the Core default family with locale-aware per-key fallback.
     *
     * @param messageSource application message source
     * @param administratorBase administrator override basename
     * @param canonicalBase canonical public basename
     * @param legacyBase retained legacy basename
     */
    RundeckExtendedMessageBundle(
            PluginAwareResourceBundleMessageSource messageSource,
            String administratorBase,
            String canonicalBase,
            String legacyBase
    ) {
        Field basenamesField = messageSource.getClass().getSuperclass().getDeclaredField('basenames')
        basenamesField.setAccessible(true)
        List<String> unrelatedBasenames =
                ((String[]) basenamesField.get(messageSource)).toList().findAll { String basename ->
            basename != canonicalBase && basename != legacyBase && basename != administratorBase
        }
        MessageSource previousParent = messageSource.parentMessageSource
        LocaleAwareTranslationMessageSource translationSource =
                new LocaleAwareTranslationMessageSource(
                        administratorBase,
                        canonicalBase,
                        legacyBase
                )
        translationSource.parentMessageSource = previousParent
        messageSource.setBasenames(unrelatedBasenames as String[])
        messageSource.parentMessageSource = translationSource
        LOG.debug('configured locale-aware Core i18n message source')
    }
}
