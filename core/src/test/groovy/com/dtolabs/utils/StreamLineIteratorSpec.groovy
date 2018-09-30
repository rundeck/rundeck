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

package com.dtolabs.utils


import spock.lang.Specification

class StreamLineIteratorSpec extends Specification {

    File testfile1

    def setup() throws Exception {

        testfile1 = File.createTempFile("FSFileLineIteratorTest1", ".txt")
        testfile1.deleteOnExit()
        testfile1.withWriter {
            it << "monkey chicken\n"
            it << "balogna lasagna\n"
            it << "cheese asparagus\n"
        }
    }

    def cleanup() {
        testfile1.delete()
    }

    def "readline"(){
        expect:
            StreamLineIterator.readLine(new StringReader("asdf")) == "asdf"
            StreamLineIterator.readLine(new StringReader("asdf\nmonkey")) == "asdf"
            StreamLineIterator.readLine(new StringReader("asdf\r\nmonkey")) == "asdf"
            StreamLineIterator.readLine(new StringReader("\nmonkey")) == ""
            StreamLineIterator.readLine(new StringReader("\rmonkey")) == ""
            StreamLineIterator.readLine(new StringReader("")) == null

    }

    def "read file"() {

        given:
            def iterator = new StreamLineIterator(new FileInputStream(testfile1), "UTF-8")
        expect:
            iterator.hasNext()
            iterator.offset == 0
            iterator.next() == "monkey chicken"
            iterator.offset == 15
            iterator.next() == "balogna lasagna"
            iterator.offset == 31
            iterator.next() == "cheese asparagus"
            iterator.offset == testfile1.length()
            !iterator.hasNext()
    }

    def "read file with channel offset"() {

        given:
            def stream = new FileInputStream(testfile1)
            stream.getChannel().position(15)
            def iterator = new StreamLineIterator(stream, "UTF-8")
        expect:
            iterator.hasNext()
            iterator.offset == 15
            iterator.next() == "balogna lasagna"
            iterator.offset == 31
            iterator.next() == "cheese asparagus"
            iterator.offset == testfile1.length()
            !iterator.hasNext()
    }

    def "read stream"() {

        given:
            def iterator = new StreamLineIterator(new ByteArrayInputStream(testfile1.bytes), "UTF-8")
        expect:
            iterator.hasNext()
            iterator.offset == 0
            iterator.next() == "monkey chicken"
            iterator.offset == 15
            iterator.next() == "balogna lasagna"
            iterator.offset == 31
            iterator.next() == "cheese asparagus"
            iterator.offset == testfile1.length()
            !iterator.hasNext()
    }
}
