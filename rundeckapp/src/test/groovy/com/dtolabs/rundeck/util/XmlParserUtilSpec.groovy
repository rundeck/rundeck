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

package com.dtolabs.rundeck.util

import spock.lang.Specification
import spock.lang.Unroll

class XmlParserUtilSpec extends Specification {
    @Unroll
    def "decode data value"() {
        given:

            def doc = new XmlParser().parse(new StringReader(xml))
        when:
            final obj = new XmlParserUtil(doc).toData()
        then:
            obj == expected

        where:
            xml                                                                | expected
            //basic
            "<value>asdf</value>"                                              | 'asdf'
            "<list><value>a</value><value>b</value></list>"                    | ['a', 'b']
            "<set><value>b</value><value>c</value></set>"                      | ['b', 'c'].toSet()
            "<map><value key='a'>b</value></map>"                              | ['a': 'b']
            //map embed
            "<map><map key='a'><value key='b'>c</value></map></map>"           | ['a': ['b': 'c']]
            "<map><set key='a'><value>b</value><value>c</value></set></map>"   | ['a': ['b', 'c'].toSet()]
            "<map><list key='a'><value>b</value><value>c</value></list></map>" | ['a': ['b', 'c']]
            //set embed
            "<set><map><value key='b'>c</value></map></set>"                   | [['b': 'c']].toSet()
            "<set><set><value>c</value></set></set>"                           | [['c'].toSet()].toSet()
            "<set><list><value>c</value></list></set>"                         | [['c']].toSet()
            //list embed
            "<list><map><value key='b'>c</value></map></list>"                 | [['b': 'c']]
            "<list><set><value>c</value></set></list>"                         | [['c'].toSet()]
            "<list><list><value>c</value></list></list>"                       | [['c']]
    }

    def "simple toMap"() {
        given:
            def xml = "<test></test>"
            def doc = new XmlParser().parse(new StringReader(xml))
        when:
            final obj = new XmlParserUtil(doc).toMap()
        then:
            obj == [test: '']
    }

    @Unroll
    def "decode renamed element"() {
        given:
            def xml = "<test>$content</test>"
            def doc = new XmlParser().parse(new StringReader(xml))

        when:
            final result = new XmlParserUtil(doc).toMap()

        then:
            result.test == expect

        where:
            content                                 | expect
            'asdf'                                  | 'asdf'
            '<element name="dingo">bingo</element>' | [dingo: 'bingo']
    }
}
