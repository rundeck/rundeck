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
