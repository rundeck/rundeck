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

package com.dtolabs.rundeck.app.internal.logging

import static org.junit.Assert.*

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin;

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/23/13
 * Time: 5:36 PM
 */

@TestMixin(GrailsUnitTestMixin)
class LogEntryLineIteratorTest  {

    static class testItem implements LineLogFormat.FormatItem{
        boolean fileEnd

        boolean lineComplete

        boolean fileStart

        boolean invalid

        LogEvent entry

        String partial

        @Override
        public java.lang.String toString() {
            return "testItem{" +
                    "fileEnd=" + fileEnd +
                    ", lineComplete=" + lineComplete +
                    ", fileStart=" + fileStart +
                    ", invalid=" + invalid +
                    ", entry=" + entry +
                    ", partial='" + partial + '\'' +
                    '}';
        }
    }
    static class testFormat implements LineLogFormat{
        LineLogFormat.FormatItem parseLine(String line) {
            def item=new testItem()
            item.invalid=true
            item.partial=line
            def event = new DefaultLogEvent()
            switch (line){
                case "start":
                    item.fileStart=true
                    item.invalid = false
                    return item
                case "end":
                    item.fileEnd = true
                    item.invalid = false
                    return item
            }

            if(line==~/^START:.*$/){
                event.datetime = new Date(0)
                event.eventType = "log"
                event.loglevel = LogLevel.DEBUG
                event.metadata = [node: "test1", something: "else"]
                item.partial = item.partial.substring(6)
                event.message = item.partial
                item.entry = event
                item.invalid = false
            }
            if(line=~/^.*:END$/){
                item.partial = item.partial.substring(0,item.partial.length()-4)
                if (item.entry) {
                    event.message = item.partial
                }
                item.lineComplete = true
                item.invalid = false
            }
            println("parseLine(${line}): ${item}")
            return item as LineLogFormat.FormatItem
        }

        @Override
        long seekBackwards(File file, int count) {
            return 0  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
    void test1(){
        File testfile1 = File.createTempFile("LogEntryLineIteratorTest1", ".log")
        testfile1.deleteOnExit()
        def lines2 = [
                "start\n",
                "START:test message:END\n",
                "START:test \n",
                "message2:END\n",
                "invalid\n",
                "end\n"
        ]
        testfile1.withWriter { w -> lines2.each { w << it } }
        LogEventLineIterator test = new LogEventLineIterator(
                new FSFileLineIterator(new FileInputStream(testfile1), "UTF-8"),
                new testFormat())
        assertTrue(test.hasNext())
        def entries = test.collect{it}
        assertEquals(2,entries.size())
        assertEquals("test message",entries[0].message)
        assertEquals("test message2",entries[1].message)
    }
}
