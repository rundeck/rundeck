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
package com.rundeck.repository.client.util

import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta
import spock.lang.Specification


class PluginMetaToPluginMetadataAdaptorTest extends Specification {

    def "plugin date format yyyy-MM-dd'T'HH:mm:sssZ test"() {
        when:
        PluginMeta pmeta = new PluginMeta()
        pmeta.date = "2018-12-17T17:57:05.188840900Z"
        PluginMetaToPluginMetadataAdaptor adaptor = new PluginMetaToPluginMetadataAdaptor(pmeta)

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(2018,Calendar.DECEMBER,17,17,57,05)
        cal.set(Calendar.MILLISECOND,188)

        then:
        adaptor.pluginDate == cal.getTime()

    }

    def "plugin date format yyyy-MM-dd'T'HH:mm:ssX test"() {
        when:
        PluginMeta pmeta = new PluginMeta()
        pmeta.date = "2018-12-14T17:24:57-03"
        PluginMetaToPluginMetadataAdaptor adaptor = new PluginMetaToPluginMetadataAdaptor(pmeta)

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("-03"))
        cal.clear()
        cal.set(2018,Calendar.DECEMBER,14,20,24,57)

        then:
        adaptor.pluginDate == cal.getTime()

    }

    def "plugin date format invalid test"() {
        when:
        PluginMeta pmeta = new PluginMeta()
        pmeta.date = "2018-12-1417:24:57-03"
        PluginMetaToPluginMetadataAdaptor adaptor = new PluginMetaToPluginMetadataAdaptor(pmeta)

        then:
        !adaptor.pluginDate

    }
}
