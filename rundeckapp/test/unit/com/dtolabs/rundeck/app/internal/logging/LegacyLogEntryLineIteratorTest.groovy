package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel

import grails.test.mixin.TestMixin;
import grails.test.mixin.support.GrailsUnitTestMixin;

import java.text.SimpleDateFormat

/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
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
 * LegacyLogEntryLineIteratorTest.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/25/13 5:11 PM
 * 
 */
@TestMixin(GrailsUnitTestMixin)
class LegacyLogEntryLineIteratorTest  {
    File testfile1
    Date startDate
    List<Date> dates
    List<Long> lengths
    SimpleDateFormat w3cDateFormat
    public void setUp() throws Exception {
        testfile1 = File.createTempFile("LogEntryLineIteratorTest1", ".log")
        testfile1.deleteOnExit()

        long nowtime = 1404864029000L
        startDate = new Date(nowtime - 120000 /*120 sec ago*/)
        dates = [
                new Date(nowtime - (90000) /*90 sec ago*/),
                new Date(nowtime - (60000) /*60 sec ago*/),
                new Date(nowtime - (30000) /*30 sec ago*/),
                new Date(nowtime),
        ]
        w3cDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        w3cDateFormat.timeZone=TimeZone.getTimeZone("GMT")
        def lines=[
                "^^^${w3cDateFormat.format(dates[0])}|ERROR|user1|||nodea|ctx1|This is a test^^^\n",
                "^^^${w3cDateFormat.format(dates[1])}|WARN|user2|||nodeb|ctx2|This is a test2^^^\n",
                "^^^${w3cDateFormat.format(dates[2])}|NORMAL|user3|||nodec|ctx3|This is a test3^^^\n",
                "^^^${w3cDateFormat.format(dates[3])}|NORMAL|user3|||nodec|ctx3|This is a test4\n" +
                        "and some more text\n" +
                        "and even more^^^\n",
                "^^^END^^^\n"
        ]
        def linelens= (lines*.toString().bytes.length)
        //running sum of line lengths == list of offsets from start
        lengths=linelens.inject([0]){List s,v-> s<< s[-1]+v }
        testfile1.withWriter {w-> lines.each { w << it } }
        //running sum of line lengths == list of offsets from start
    }

    public void testFromStart() {
        def iterator = new LegacyLogEventLineIterator(new FSFileLineIterator(new FileInputStream(testfile1), "UTF-8"))
        assertTrue(iterator.hasNext())
        assertEquals(lengths[0], iterator.offset)
        LogEvent entry = iterator.next()
        assertEntry(entry, dates[0], LogLevel.ERROR, [user: 'user1', node: 'nodea', context: 'ctx1'], "This is a test")
        assertEquals(lengths[1], iterator.offset)

        LogEvent entry2 = iterator.next()
        assertEntry(entry2, dates[1], LogLevel.WARN, [user: 'user2', node: 'nodeb', context: 'ctx2'], "This is a test2")
        assertEquals(lengths[2], iterator.offset)

        LogEvent entry3 = iterator.next()
        assertEntry(entry3, dates[2], LogLevel.NORMAL, [user: 'user3', node: 'nodec', context: 'ctx3'], "This is a test3")
        assertEquals(lengths[3], iterator.offset)

        LogEvent entry4 = iterator.next()
        assertEntry(entry4, dates[3], LogLevel.NORMAL, [user: 'user3', node: 'nodec', context: 'ctx3'], "This is a test4\nand some more text\nand even more")
        assertEquals(lengths[4], iterator.offset)

        assertFalse(iterator.hasNext())
    }

    public void testFromMiddle() {
        def fis = new FileInputStream(testfile1)
        fis.channel.position(lengths[1])
        def iterator = new LegacyLogEventLineIterator(new FSFileLineIterator(fis, "UTF-8"))
        assertTrue(iterator.hasNext())

        assertEquals(lengths[1], iterator.offset)

        LogEvent entry2 = iterator.next()
        assertEntry(entry2, dates[1], LogLevel.WARN, [user: 'user2', node: 'nodeb', context: 'ctx2'], "This is a test2")
        assertEquals(lengths[2], iterator.offset)

        LogEvent entry3 = iterator.next()
        assertEntry(entry3, dates[2], LogLevel.NORMAL, [user: 'user3', node: 'nodec', context: 'ctx3'], "This is a test3")
        assertEquals(lengths[3], iterator.offset)

        LogEvent entry4 = iterator.next()
        assertEntry(entry4, dates[3], LogLevel.NORMAL, [user: 'user3', node: 'nodec', context: 'ctx3'], "This is a test4\n" +
                "and some more text\n" +
                "and even more")
        assertEquals(lengths[4], iterator.offset)

        assertFalse(iterator.hasNext())
    }
    public void testFromMiddle2() {
        def fis = new FileInputStream(testfile1)
        fis.channel.position(lengths[2])
        def iterator = new LegacyLogEventLineIterator(new FSFileLineIterator(fis, "UTF-8"))
        assertTrue(iterator.hasNext())

        assertEquals(lengths[2], iterator.offset)

        LogEvent entry3 = iterator.next()
        assertEntry(entry3, dates[2], LogLevel.NORMAL, [user: 'user3', node: 'nodec', context: 'ctx3'], "This is a test3")
        assertEquals(lengths[3], iterator.offset)

        LogEvent entry4 = iterator.next()
        assertEntry(entry4, dates[3], LogLevel.NORMAL, [user: 'user3', node: 'nodec', context: 'ctx3'], "This is a test4\n" +
                "and some more text\n" +
                "and even more")
        assertEquals(lengths[4], iterator.offset)


        assertFalse(iterator.hasNext())
    }
    public void testFromEnd() {
        def fis = new FileInputStream(testfile1)
        fis.channel.position(lengths[4])
        def iterator = new LegacyLogEventLineIterator(new FSFileLineIterator(fis, "UTF-8"))

        assertEquals(lengths[4], iterator.offset)

        assertFalse(iterator.hasNext())
    }

    public void testSeekBackwards(){
        assertEquals(lengths[3],LegacyLogEventLineIterator.seekBackwards(testfile1,1))
        assertEquals(lengths[2],LegacyLogEventLineIterator.seekBackwards(testfile1,2))
        assertEquals(lengths[1],LegacyLogEventLineIterator.seekBackwards(testfile1,3))
        assertEquals(lengths[0],LegacyLogEventLineIterator.seekBackwards(testfile1,4))
    }
    private static void assertEntry(LogEvent entry, final Date date, final LogLevel level, final LinkedHashMap<String, String> meta, final String message) {
        assertNotNull(entry)
        assertEquals(message, entry.message)
        assertNotNull(entry.datetime)
        assertEquals(date.time, entry.datetime.time)
        assertEquals(level, entry.loglevel)
        assertNotNull(entry.metadata)
        meta.each { k, v ->
            assertEquals("key ${k}", v, entry.metadata[k])
        }
    }
}
