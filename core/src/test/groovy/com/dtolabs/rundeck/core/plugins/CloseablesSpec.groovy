/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.plugins

import spock.lang.Specification

/**
 * @author greg
 * @since 3/10/17
 */
class CloseablesSpec extends Specification {
    def "maybe closeable with closeable"() {
        given:
        def wasClosed = false
        def m1 = Closeables.maybeCloseable({ -> wasClosed = true } as Closeable)
        when:
        m1.close()
        then:
        wasClosed
    }

    def "maybe closeable without object"() {
        given:

        def m2 = Closeables.maybeCloseable(obj)
        when:
        m2.close()
        then:
        true

        where:

        obj          | _
        new Object() | _
        null         | _
        [:]          | _
        1            | _
    }
}
