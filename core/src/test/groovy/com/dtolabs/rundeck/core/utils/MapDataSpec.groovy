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

package com.dtolabs.rundeck.core.utils

import spock.lang.Specification

class MapDataSpec extends Specification {
    def "meta path string"() {
        given:
            def data = [a: 'b', c: [d: 'e']]
        expect:
            MapData.metaPathString(data, path, defval) == expect

        where:
            path              | defval | expect
            'a'               | null   | 'b'
            'a'               | 'z'    | 'b'
            'b'               | 'z'    | 'z'
            'c'               | null   | null
            'c.d'             | null   | 'e'
            'c.d'             | 'x'    | 'e'
            'c.d.z'           | 'x'    | 'x'
            'c.d.z'           | null   | null
            'c.f'             | 'x'    | 'x'
            'p.q'             | 'x'    | 'x'
            'p.q'             | null   | null
            'a.d.f.e.d.f.e.f' | null   | null
    }

    def "meta string prop"() {
        given:
            def data = [a: 'b', c: [d: 'e']]
        expect:
            MapData.metaStringProp(data, path, defval) == expect

        where:
            path              | defval | expect
            'a'               | null   | 'b'
            'a'               | 'z'    | 'b'
            'b'               | 'z'    | 'z'
            'b'               | null   | null
            'c'               | null   | null
            'c.d'             | null   | 'e'
            'c.d'             | 'x'    | 'e'
            'c.d.z'           | 'x'    | 'x'
            'c.d.z'           | null   | null
            'c.f'             | 'x'    | 'x'
            'p.q'             | 'x'    | 'x'
            'p.q'             | null   | null
            'a.d.f.e.d.f.e.f' | null   | null
    }

    def "meta path boolean"() {
        given:
            def data = [a: 'true', b: true, c: false, d: [x: true, y: false]]
        expect:
            MapData.metaPathBoolean(data, path, defval) == expect

        where:
            path | defval | expect
            'a'  | false  | true
            'a'  | true   | true
            'b'  | false  | true
            'b'  | true   | true
            'c'  | false  | false
            'c'  | true   | false
            'd'  | true   | false
            'd'  | false  | false
    }
}
