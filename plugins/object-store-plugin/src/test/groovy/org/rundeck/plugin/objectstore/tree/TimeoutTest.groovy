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
package org.rundeck.plugin.objectstore.tree

import okhttp3.mockwebserver.MockWebServer
import org.rundeck.plugin.objectstore.ObjectStorePlugin
import spock.lang.Specification


class TimeoutTest extends Specification {

    def "Test timeout setting"() {
        setup:
        String configBucket = "test-config-bucket"
        String storeUrl = "http://localhost:9000"
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()

        when:
        ObjectStorePlugin plugin = new ObjectStorePlugin()
        plugin.objectStoreUrl = httpServer.url("/")
        plugin.bucket = configBucket
        plugin.connectionTimeout = 5
        long start = System.currentTimeMillis()
        def tree = plugin.initTree()

        then:
        def ex = thrown(RuntimeException)
        ex.message == "Unable to connect to the server. Please check your firewall, or make sure your server is accepting connections."
        (System.currentTimeMillis()-start) >= 5000

    }

    def "Test non-timeout exception"() {
        setup:
        String configBucket = "test-config-bucket"
        String storeUrl = "http://localhost"

        when:
        ObjectStorePlugin plugin = new ObjectStorePlugin()
        plugin.objectStoreUrl = storeUrl
        plugin.bucket = configBucket
        plugin.connectionTimeout = 5
        def tree = plugin.initTree()

        then:
        thrown(Exception)


    }
}
