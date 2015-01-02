import grails.test.mixin.support.GrailsUnitTestMixin;

import com.dtolabs.rundeck.util.XmlParserUtil

/*
* Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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
*
*/

/*
* XmlParserUtilTests.groovy
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 27, 2011 6:59:47 PM
*
*/


@TestMixin(GrailsUnitTestMixin)
public class XmlParserUtilTests  {

    void testShouldProduceEmptyString() {
        def map = [test: '']
        def xml = "<test></test>"
        def doc = new XmlParser().parse(new StringReader(xml))
        final obj = new XmlParserUtil(doc).toMap()
        assertEquals(map, obj)
    }
    void testShouldProduceEmptyString2() {
        def map = [test: '']
        def xml = "<test />"
        def doc = new XmlParser().parse(new StringReader(xml))
        final obj = new XmlParserUtil(doc).toMap()
        assertEquals(map, obj)
    }
    void testShouldProduceMap() {
        def map = [test: 'value']
        def xml = "<test>value</test>"
        def doc = new XmlParser().parse(new StringReader(xml))
        final obj = new XmlParserUtil(doc).toMap()
        assertEquals(map, obj)
    }

    void testShouldProduceSubMap() {
        def map = [test: [a: 'b', c: 'd']]
        def xml = "<test><a>b</a><c>d</c></test>"
        def doc = new XmlParser().parse(new StringReader(xml))
        final obj = new XmlParserUtil(doc).toMap()
        assertEquals(map, obj)
    }

    void testShouldProduceAttributes() {
        def map = [test: ['name': 'something', '<text>': 'value']]
        def xml = "<test name='something'>value</test>"
        def doc = new XmlParser().parse(new StringReader(xml))
        final obj = new XmlParserUtil(doc).toMap()
        assertEquals(map, obj)
    }

    void testShouldProduceAListFromMultipleChildElementsWithTheSameName() {
        def map = [test: [multi: ['a', 'b']]]
        def xml = "<test><multi>a</multi><multi>b</multi></test>"
        def doc = new XmlParser().parse(new StringReader(xml))
        final obj = new XmlParserUtil(doc).toMap()
        assertEquals(map, obj)
    }

    void testShouldProduceAListFromElementAndAttributeWithTheSameName() {
        def map = [test: [multi: ['a', 'b']]]
        def xml = "<test multi='a'><multi>b</multi></test>"
        def doc = new XmlParser().parse(new StringReader(xml))
        final obj = new XmlParserUtil(doc).toMap()
        assertEquals(map, obj)
    }
    void testShouldProduceInteger() {
        def map = [test: 2]
        def xml = "<test>2</test>"
        def doc = new XmlParser().parse(new StringReader(xml))
        final obj = new XmlParserUtil(doc).toMap()
        assertEquals(map, obj)
    }
    void testShouldProduceBooleanTrue() {
        def map = [test: Boolean.TRUE]
        def xml = "<test>true</test>"
        def doc = new XmlParser().parse(new StringReader(xml))
        final obj = new XmlParserUtil(doc).toMap()
        assertEquals(map, obj)
    }
    void testShouldProduceBooleanFalse() {
        def map = [test: Boolean.FALSE]
        def xml = "<test>false</test>"
        def doc = new XmlParser().parse(new StringReader(xml))
        final obj = new XmlParserUtil(doc).toMap()
        assertEquals(map, obj)
    }
}