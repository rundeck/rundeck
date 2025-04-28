/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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

import grails.events.annotation.Subscriber
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.PluginApiService
import rundeck.services.UiPluginService


class PluginCachePreloader {

    @Autowired UiPluginService uiPluginService
    @Autowired PluginApiService pluginApiService

    @Subscriber("rundeck.bootstrap")
    void boostrap() {
        try {
            def plugins = pluginApiService.listPluginsDetailed()
            plugins.descriptions.each { svc, v ->
                if(svc != "UI") {
                    v.each {pdesc ->
                        uiPluginService.getMessagesFor(svc,pdesc.name)

                    }
                }
            }
        } catch(Throwable t) {
            println "Error heating the plugin cache"
            t.printStackTrace()
        }
    }
}
