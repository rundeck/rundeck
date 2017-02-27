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

package org.rundeck.util

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 2/23/17
 */
class ThresholdInputStreamSpec extends Specification {
    @Unroll("read #total with max #max")
    def "threshold test"() {
        given:
        def data = 1..total as byte[]
        def arr1 = new ByteArrayInputStream(data)
        def test = new ThresholdInputStream(arr1, max)
        def buff = new byte[data.length]
        when:
        test.read(buff, 0, data.length)

        then:
        ThresholdInputStream.Threshold e = thrown()
        e.breach == total
        data.length == total

        where:
        total | max
        10    | 9
        10    | 1
        200   | 1
        200   | 199
        200   | 50

    }

    @Unroll("read #total with max #max")
    def "threshold ok"() {
        given:
        def data = 1..total as byte[]
        def arr1 = new ByteArrayInputStream(data)
        def test = new ThresholdInputStream(arr1, max)
        def buff = new byte[data.length]
        when:
        def len = test.read(buff, 0, data.length)

        then:
        len == total
        data.length == total
        buff == data

        where:
        total | max
        10    | 10
        10    | 11
        200   | 200
        200   | 2000
        200   | 201

    }
}
