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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.support.AbstractResourceBasedMessageSource
import org.springframework.context.support.ReloadableResourceBundleMessageSource

/**
 * Puts an operator-supplied message bundle ahead of the ones shipped with the product, so
 * translations under $RDECK_BASE/i18n override rather than supplement them.
 *
 * Grails 8.0.0-M6 removed PluginAwareResourceBundleMessageSource, which extended Spring's
 * ReloadableResourceBundleMessageSource. What M6 configures instead is a plain
 * ResourceBundleMessageSource, and the difference matters here: it resolves base names through
 * java.util.ResourceBundle, which reads from the classpath, cannot interpret a 'file:' base name,
 * and throws MissingResourceException rather than moving on when a bundle is absent. Handing it an
 * external path therefore failed startup for every installation -- including the overwhelmingly
 * common one that has no custom translations at all.
 *
 * So the base name is only added to a message source that can actually load it. Where it cannot,
 * the condition is logged instead of failing the boot, because an optional override is not worth a
 * server that will not start.
 */
class RundeckExtendedMessageBundle {
    private static final transient Logger LOG = LoggerFactory.getLogger(RundeckExtendedMessageBundle.class)

    RundeckExtendedMessageBundle(AbstractResourceBasedMessageSource messageSource, String externalBase) {
        if (!externalBase) {
            return
        }
        if (!(messageSource instanceof ReloadableResourceBundleMessageSource)) {
            LOG.warn("External i18n messages at ${externalBase} were not applied: the configured " +
                     "message source (${messageSource.getClass().name}) resolves base names from " +
                     "the classpath only and cannot read a file location.")
            return
        }
        List<String> basenames = [externalBase] + messageSource.basenameSet.toList()
        messageSource.setBasenames(basenames as String[])
        LOG.debug("adding external i18n message source: ${externalBase}")
    }
}
