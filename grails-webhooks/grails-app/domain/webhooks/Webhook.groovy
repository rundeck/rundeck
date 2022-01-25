/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
package webhooks

class Webhook {

    String uuid
    String name
    String project
    String authToken
    String authConfigJson
    String eventPlugin
    String pluginConfigurationJson = '{}'
    boolean enabled = true

    static constraints = {
        uuid(nullable: true)
        name(nullable: false)
        authConfigJson(nullable: true)
        project(nullable: false)
        authToken(nullable: false)
        eventPlugin(nullable: false)
    }

    static mapping = {
        authConfigJson type: 'text'
        pluginConfigurationJson type: 'text'
    }

    static String cleanAuthToken(String authtoken) {
        if(authtoken.contains("#")) return authtoken.substring(0,authtoken.indexOf("#"))
        return authtoken
    }
}
