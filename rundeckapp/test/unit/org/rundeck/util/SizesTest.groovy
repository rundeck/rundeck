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

package org.rundeck.util

import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

import static org.rundeck.util.Sizes.parseFileSize

/**
 * @author greg
 * @since 2/23/17
 */
class SizesTest extends Specification {
    @Unroll("parse #string expect #val")
    def "file size"() {
        expect:
        parseFileSize(string) == val
        where:
        string   | val
        "1"      | 1L
        "1k"     | 1024L
        "1K"     | 1024L
        "1Kb"    | 1024L
        "1KB"    | 1024L
        "1kb"    | 1024L
        "500KB"  | 1024L * 500
        "1m"     | 1024 * 1024L
        "100M"   | 1024 * 1024L * 100
        "1024mb" | 1024 * 1024L * 1024L
        "1g"     | 1024 * 1024L * 1024L
        "1t"     | 1024 * 1024L * 1024L * 1024L
    }

    @Unroll("parse #string expect #val")
    def "file size invalid"() {
        expect:
        parseFileSize(string) == val
        where:
        string  | val
        "1z"    | null
        "1H"    | null
        "1abcd" | null
        "asdf2" | null
    }

    def "evaluate timeout duration"() {
        expect:
        result == Sizes.parseTimeDuration(value)

        where:
        value | result
        "1s"  | 1L
        "1m"  | 60
        "1h"  | 3600
        "1d"  | 24 * 3600
        "1w"  | 7 * 24 * 3600
        "1y"  | 365 * 24 * 3600
    }

    def "evaluate timeout duration units"() {
        expect:
        Sizes.parseTimeDuration(value, unit) == result

        where:
        value   | unit             | result
        "1s"    | TimeUnit.SECONDS | 1L
        "2s"    | TimeUnit.SECONDS | 2L
        "2000s" | TimeUnit.SECONDS | 2000L
        "1m"    | TimeUnit.MINUTES | 1
        "2m"    | TimeUnit.MINUTES | 2
        "2000m" | TimeUnit.MINUTES | 2000
        "1h"    | TimeUnit.HOURS   | 1
        "2h"    | TimeUnit.HOURS   | 2
        "2000h" | TimeUnit.HOURS   | 2000
        "1d"    | TimeUnit.DAYS    | 1
        "2d"    | TimeUnit.DAYS    | 2
        "2000d" | TimeUnit.DAYS    | 2000
        "1w"    | TimeUnit.DAYS    | 7
        "2w"    | TimeUnit.DAYS    | 14
        "2000w" | TimeUnit.DAYS    | 14000
        "1y"    | TimeUnit.SECONDS | 365 * 24 * 3600
        "1y"    | TimeUnit.DAYS    | 365
        "2y"    | TimeUnit.DAYS    | 2 * 365
        "2000y" | TimeUnit.DAYS    | 2000 * 365
    }

    def "evaluate timeout duration multiple unit"() {
        expect:
        result == Sizes.parseTimeDuration(value)

        where:
        value       | result
        "1d1h1m1s"  | 24 * 3600 + 3600 + 60 + 1L
        "1d1h1m2s"  | 24 * 3600 + 3600 + 60 + 2
        "1d1h17m1s" | 24 * 3600 + 3600 + (17 * 60) + 1
        "1d9h1m1s"  | 24 * 3600 + (9 * 3600) + (1 * 60) + 1
        "3d1h1m1s"  | (3 * 24 * 3600) + (1 * 3600) + (1 * 60) + 1
    }

    def "evaluate timeout duration ignored text"() {
        expect:
        result == Sizes.parseTimeDuration(value)

        where:
        value                     | result
        "1d 1h 1m 1s"             | 24 * 3600 + 3600 + 60 + 1L
        "1d 1h 1m 2s"             | 24 * 3600 + 3600 + 60 + 2
        "1d 1h 17m 1s"            | 24 * 3600 + 3600 + (17 * 60) + 1
        "1d 9h 1m 1s"             | 24 * 3600 + (9 * 3600) + (1 * 60) + 1
        "3d 1h 1m 1s"             | (3 * 24 * 3600) + (1 * 3600) + (1 * 60) + 1
        "3d 12h #usual wait time" | (3 * 24 * 3600) + (12 * 3600) + (0 * 60) + 0
    }

    def "evaluate timeout duration default seconds"() {
        expect:
        result == Sizes.parseTimeDuration(value)

        where:
        value     | result
        "0"       | 0L
        "123"     | 123
        "10000"   | 10000
        "5858929" | 5858929
    }

    def "evaluate timeout duration invalid"() {
        expect:
        result == Sizes.parseTimeDuration(value)

        where:
        value  | result
        "asdf" | 0L
        "123z" | 0
    }
}
