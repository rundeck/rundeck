/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

/**
 * Created by greg on 4/25/16.
 */
class TextUtilsSpec extends Specification {
    def "escape with backslash"() {
        expect:
        TextUtils.escape(input, (char) echar, (char[]) chars) == result


        where:
        echar | input                 | chars                 | result
        '\\'  | "abc\\,=5def"         | ['\\']                | 'abc\\\\,=5def'
        '\\'  | "abc\\,=5def"         | ['\\', ',']           | 'abc\\\\\\,=5def'
        '\\'  | "abc\\,=5def"         | ['\\', ',', '=']      | 'abc\\\\\\,\\=5def'
        '\\'  | "abc\\,=5def"         | ['\\', ',', '=', '5'] | 'abc\\\\\\,\\=\\5def'
        '\\'  | "abc\\,=5def\\,=5ghi" | ['\\']                | 'abc\\\\,=5def\\\\,=5ghi'
        '\\'  | "abc\\,=5def\\,=5ghi" | ['\\', ',']           | 'abc\\\\\\,=5def\\\\\\,=5ghi'
        '\\'  | "abc\\,=5def\\,=5ghi" | ['\\', ',', '=']      | 'abc\\\\\\,\\=5def\\\\\\,\\=5ghi'
        '\\'  | "abc\\,=5def\\,=5ghi" | ['\\', ',', '=', '5'] | 'abc\\\\\\,\\=\\5def\\\\\\,\\=\\5ghi'

    }

    def "unescape with backslash"() {
        expect:
        TextUtils.unescape(input, (char) echar, (char[]) chars) == result

        where:
        echar | input                                 | chars           | result
        '\\'  | 'abc\\\\,=5def'                       | []              | 'abc\\,=5def'
        '\\'  | 'abc\\\\,\\=5def'                     | []              | 'abc\\,\\=5def'
        '\\'  | 'abc\\\\\\,=5def'                     | [',']           | "abc\\,=5def"
        '\\'  | 'abc\\\\\\,\\=5def'                   | [',', '=']      | "abc\\,=5def"
        '\\'  | 'abc\\\\\\,\\=\\5def'                 | [',', '=', '5'] | "abc\\,=5def"
        '\\'  | 'abc\\\\,=5def\\\\,=5ghi'             | []              | 'abc\\,=5def\\,=5ghi'
        '\\'  | 'abc\\\\\\,=5def\\\\\\,=5ghi'         | [',']           | 'abc\\,=5def\\,=5ghi'
        '\\'  | 'abc\\\\\\,\\=5def\\\\\\,\\=5ghi'     | [',', '=']      | 'abc\\,=5def\\,=5ghi'
        '\\'  | 'abc\\\\\\,\\=\\5def\\\\\\,\\=\\5ghi' | [',', '=', '5'] | 'abc\\,=5def\\,=5ghi'


    }

    def "join without escape"() {
        expect:
        TextUtils.join(input as String[], (char) separator) == result

        where:
        input                           | separator | result
        ["abc", "def"]                  | '@'       | 'abc@def'
        ["abc@monkey", "def"]           | '@'       | 'abc@monkey@def'
        ["abc\\monkey", "def"]          | '@'       | 'abc\\monkey@def'
        ["abc\\monkey", "def@champ"]    | '@'       | 'abc\\monkey@def@champ'
        ["abc\\mon\\key", "def@cha@mp"] | '@'       | 'abc\\mon\\key@def@cha@mp'
        ["abc\\mon@key", "def\\cha@mp"] | '@'       | 'abc\\mon@key@def\\cha@mp'
        ["abc\\mon@key", "def\\cha@mp"] | '@'       | 'abc\\mon@key@def\\cha@mp'
    }
    static def BS = (char) '\\'

    def "join and split escaped backslash"() {
        expect:
        TextUtils.joinEscaped(input as String[], (char) separator, BS, special as char[]) == result
        TextUtils.splitUnescape(result, (char) separator, BS, special as char[]) == input

        where:
        input                           | separator | special | result
        ["abc", "def"]                  | '@'       | []      | 'abc@def'
        ["abc@monkey", "def"]           | '@'       | []      | 'abc\\@monkey@def'
        ["abc\\monkey", "def"]          | '@'       | []      | 'abc\\\\monkey@def'
        ["abc\\monkey", "def@champ"]    | '@'       | []      | 'abc\\\\monkey@def\\@champ'
        ["abc\\mon\\key", "def@cha@mp"] | '@'       | []      | 'abc\\\\mon\\\\key@def\\@cha\\@mp'
        ["abc\\mon@key", "def\\cha@mp"] | '@'       | []      | 'abc\\\\mon\\@key@def\\\\cha\\@mp'
    }

}
