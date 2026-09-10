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

/**
 * Puts an operator-supplied message bundle ahead of the ones shipped with the product, so
 * translations under $RDECK_BASE/i18n override rather than supplement them.
 *
 * Grails 8.0.0-M6 removed PluginAwareResourceBundleMessageSource: plugin bundles are now declared
 * through generated META-INF/grails/i18n.properties descriptors and served by Spring's own message
 * source. Targeting Spring's AbstractResourceBasedMessageSource follows that move, and lets the base
 * names be read and rewritten through the public API instead of a reflective field grab.
 */
class RundeckExtendedMessageBundle {
    private static final transient Logger LOG = LoggerFactory.getLogger(RundeckExtendedMessageBundle.class)

    RundeckExtendedMessageBundle(AbstractResourceBasedMessageSource messageSource, String externalBase) {
        if (externalBase) {
            List<String> basenames = [externalBase] + messageSource.basenameSet.toList()
            messageSource.setBasenames(basenames as String[])
            LOG.debug("adding external i18n message source: ${externalBase}")
        }
    }
}
