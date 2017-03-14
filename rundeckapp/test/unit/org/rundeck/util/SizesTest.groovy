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
}
