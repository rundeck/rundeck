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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Field


class RundeckExtendedMessageBundle {
    private static final transient Logger LOG = LoggerFactory.getLogger(RundeckExtendedMessageBundle.class)

    RundeckExtendedMessageBundle(PluginAwareResourceBundleMessageSource messageSource, String externalBase) {
        if(externalBase) {
            PluginAwareResourceBundleMessageSource msgSource = (PluginAwareResourceBundleMessageSource) messageSource
            Field oldBaseNameField = msgSource.getClass().getSuperclass().getDeclaredField("basenames")
            oldBaseNameField.setAccessible(true)
            def oldBaseNames = oldBaseNameField.get(msgSource).toList()
            oldBaseNames.add(0, externalBase)
            msgSource.setBasenames(oldBaseNames.toArray(new String[oldBaseNames.size()]))
            LOG.debug("adding external i18n message source: ${externalBase}")
        }
    }
}
