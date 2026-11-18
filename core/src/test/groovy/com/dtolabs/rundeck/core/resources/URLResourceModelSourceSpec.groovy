/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.resources

import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import spock.lang.Specification

/**
 * Verifies the description used in logs does not expose credentials.
 */
class URLResourceModelSourceSpec extends Specification {

    def "toString omits user info and query string"() {
        given:
        def source = new URLResourceModelSource(AbstractBaseTest.createTestFramework())
        source.configure([project: 'URLResourceModelSourceSpec', url: url] as Properties)

        expect:
        source.toString() == "URLResourceModelSource{URL='${expected}'}".toString()

        where:
        url                                                           | expected
        'https://user:secret@example.com:8443/nodes.yaml?token=abc123' | 'https://example.com:8443/nodes.yaml'
        'http://example.com/nodes.json?key=abc'                       | 'http://example.com/nodes.json'
    }
}
