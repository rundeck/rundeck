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

package com.dtolabs.rundeck.core.logging.internal

import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.internal.LineLogFormat
import com.dtolabs.rundeck.core.logging.internal.RundeckLogFormat
import spock.lang.Shared
import spock.lang.Specification

import java.text.SimpleDateFormat

/**
 * @author greg
 * @since 5/26/17
 */
class RundeckLogFormatSpec extends Specification {
    @Shared
    SimpleDateFormat dateFormat

    @Shared
    String expectPrefix1
    @Shared
    String expectLineEnd

    def setupSpec() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        expectPrefix1 = "^2013-05-24T01:31:02Z||DEBUG|{something=else|test=1}|"
        expectLineEnd = "^"
    }

    def "test backslash escape"(){
        expect:
        "monkey \\^\\^\\^ blah \\\\ elf \\\\\\\\" == RundeckLogFormat.backslashEscape("monkey ^^^ blah \\ elf \\\\",'^')
    }
    def "test backslash escape null"(){
        expect:
        '' == RundeckLogFormat.backslashEscape(null,'^')
    }

    def "test start and end markers"() {
        when:
        RundeckLogFormat format = new RundeckLogFormat()
        then:
        "^text/x-rundeck-log-v2.0^" == format.outputBegin()
        "^END^" == format.outputFinish()
    }

    def "parseLine: first multiline entry should have newline"() {
        given:
        def format = new RundeckLogFormat()

        def expectPrefix1 = "^2013-05-24T01:31:02Z||DEBUG|{something=else|test=1}|"

        def line = expectPrefix1 + partial
        when:
        LineLogFormat.FormatItem item = format.parseLine(line)

        then:
        !item.lineComplete
        item.entry.message == partial + '\n'


        where:
        partial     | _
        'a message' | _
    }

    def "parseLine: no prefix should be partial"() {
        given:
        def format = new RundeckLogFormat()


        def line = partial
        when:
        LineLogFormat.FormatItem item = format.parseLine(line)

        then:
        !item.lineComplete
        item.entry == null
        item.partial == partial + '\n'


        where:
        partial     | _
        'a message' | _
    }

    def "parseLine: delimiter only should finish a partial"() {
        given:
        def format = new RundeckLogFormat()


        def line = '^'
        when:
        LineLogFormat.FormatItem item = format.parseLine(line)

        then:
        item.lineComplete
        item.entry == null
        item.partial == ''


    }

    def "testOutputEventDefaultEventLevel"() {
        given:
        RundeckLogFormat format = new RundeckLogFormat()
        def event = new DefaultLogEvent(LogLevel.NORMAL,dateFormat.parse('2013-05-24T01:31:02Z'),'message abc',"log",[test: "1", something: "else"])

        //loglevel and event type elided for defaults
        def prefix = "^2013-05-24T01:31:02Z|||{something=else|test=1}|"

        expect:
        prefix+'message abc'+expectLineEnd == format.outputEvent(event)
    }

    private void expectMessage(String expect) {
        RundeckLogFormat format = new RundeckLogFormat()
        expectPrefix1 + expect + expectLineEnd == format.outputEvent(defaultTestEvent())
    }

    private DefaultLogEvent defaultTestEvent() {
        new DefaultLogEvent(LogLevel.DEBUG,dateFormat.parse('2013-05-24T01:31:02Z'),null,"log",[test: "1", something: "else"])
    }

    def "testOutputEventSimple"() {
        given:
        def event = defaultTestEvent()
        event.message = "test message"
        def expect = "test message"
        expect:
        expectMessage(expect)
    }

    def "testOutputEventMultiline"() {
        given:
        def event = defaultTestEvent()
        event.message = "test message\nanother line"
        def expect = "test message\nanother line"
        expect:
        expectMessage(expect)
    }

    def "testOutputEventEscapeMessage"() {
        given:
        def event = defaultTestEvent()
        event.message = "This is a | weird \\asdf ^ message"
        def expect = "This is a | weird \\\\asdf \\^ message"
        expect:
        expectMessage(expect)
    }

    def "testOutputEventEscapeMeta"() {
        given:
        RundeckLogFormat format = new RundeckLogFormat()
        def event = defaultTestEvent()
        event.message = "test"
        event.metadata = ["aomething=": "=else", "best|as}df": "flif}|="]
        expect:
        "^2013-05-24T01:31:02Z||DEBUG|{aomething\\==\\=else|best\\|as\\}df=flif\\}\\|\\=}|test^" == format.outputEvent(event)
    }
    def "testOutputEventBlankMeta"() {
        given:
        RundeckLogFormat format = new RundeckLogFormat()
        def event = defaultTestEvent()
        event.message = "test"
        event.metadata = ["a":"b",c:'']
        expect:
        "^2013-05-24T01:31:02Z||DEBUG|{a=b|c=}|test^" == format.outputEvent(event)
    }
    def "testOutputEventNullMeta"() {
        given:
        RundeckLogFormat format = new RundeckLogFormat()
        def event = defaultTestEvent()
        event.message = "test"
        event.metadata = ["a":"b",c:null]
        expect:
        "^2013-05-24T01:31:02Z||DEBUG|{a=b}|test^" == format.outputEvent(event)
    }

    def "testOutputEventEscapeEventType"() {
        given:
        RundeckLogFormat format = new RundeckLogFormat()
        def event = defaultTestEvent()
        event.message = "test"
        event.eventType = "test2|test3"
        expect:
        "^2013-05-24T01:31:02Z|test2test3|DEBUG|{something=else|test=1}|test^" == format.outputEvent(event)
    }

    def "testInputFileStart"() {
        given:
        RundeckLogFormat format = new RundeckLogFormat()
        LineLogFormat.FormatItem item = format.parseLine(RundeckLogFormat.FILE_START)
        expect:
        item.fileStart
        !item.invalid
    }

    def "testInputFileEnd"() {
        given:
        RundeckLogFormat format = new RundeckLogFormat()
        LineLogFormat.FormatItem item = format.parseLine(RundeckLogFormat.FILE_END)
        expect:
        item.fileEnd
        !item.invalid
    }
    def "testInputBlank"() {
        given:
        RundeckLogFormat format = new RundeckLogFormat()
        LineLogFormat.FormatItem item = format.parseLine("")
        expect:
        !item.fileEnd
        !item.invalid
        "\n" == item.partial
    }

    def "testInputBasic"() {
        when:
        RundeckLogFormat format = new RundeckLogFormat()
        def event = defaultTestEvent()
        def expectMessage = "test message"
        def line = expectPrefix1 + expectMessage + expectLineEnd
        LineLogFormat.FormatItem item = format.parseLine(line)
        def assertMsg = item.toString() + ";" + line
        then:
        assert item.lineComplete : assertMsg
        assert !item.invalid : assertMsg
        assert item.entry : assertMsg
        assert expectMessage == item.entry.message : assertMsg
        assert event.metadata == item.entry.metadata : assertMsg
        assert event.loglevel == item.entry.loglevel : assertMsg
        assert event.datetime == item.entry.datetime : assertMsg
        assert event.eventType == item.entry.eventType : assertMsg
    }

    private void assertDecode(String expect, String expectDone, String input) {
        RundeckLogFormat.UnescapedData unescapedData = RundeckLogFormat.decodeLog(input)
        assert expect == unescapedData.unescaped : "input: " + input
        assert expectDone == unescapedData.delimiterReached : "input: " + input
    }

    def "testDecodeLog"() {
        expect:
        assertDecode("abc", null, "abc")
        assertDecode("abc", '^', "abc^^^")
        assertDecode("abc\\", '^', "abc\\\\^^^")
        assertDecode("abc\\", null, "abc\\\\")
        assertDecode("abc\\^", '^', "abc\\\\\\^^^^")
        assertDecode("abc\\^", null, "abc\\\\\\^")
        assertDecode("abc^^^asdf", null, "abc\\^\\^\\^asdf")
        assertDecode("abc^^^asdf", '^', "abc\\^\\^\\^asdf^^^")
    }

    private void assertUnescape(String expect, String expectDone, String expectRest,
                                char escape, String validEscaped, String delim, String input) {
        RundeckLogFormat.UnescapedData unescapedData = RundeckLogFormat.unescape(input,escape,validEscaped,delim)
        assert expect == unescapedData.unescaped : "input: " + input
        assert expectDone == unescapedData.delimiterReached : "input: " + input
        assert expectRest == unescapedData.remaining : "input: " + input
    }
    def "testUnescapeBasic"() {
        expect:
        assertUnescape("abc", null, null, '\\' as char, "\\^", "^^^", "abc")
    }
    def "testUnescapeEndline"(){
        expect:
        assertUnescape("abc", '^^^', null, '\\' as char, "\\^", "^^^", "abc^^^")
    }
    def "testUnescapeValidEscapechar"(){
        expect:
        assertUnescape("abc\\", '^^^', null, '\\' as char, "\\^", "^^^", "abc\\\\^^^")
        assertUnescape("abc^", '^^^', null, '\\' as char, "\\^", "^^^", "abc\\^^^^")
    }
    def "testUnescapeLiteralEscapechar"(){
        expect:
        assertUnescape("abc^\\e", '^^^', null, '\\' as char, "\\^", "^^^", "abc\\^\\e^^^")
    }
    def "testUnescapeMessageRest"() {
        expect:
        assertUnescape("abc", '^^^', 'alpha', '\\' as char, "\\^", "^^^", "abc^^^alpha")
        assertUnescape("abc", '^^^', 'alpha\\^\\\\^^^asdf', '\\' as char, "\\^", "^^^", "abc^^^alpha\\^\\\\^^^asdf")
    }
    def "testUnescapeMetaKey"() {
        expect:
        assertUnescape("monkey", '=', 'test', '\\' as char, "=|}", "=", "monkey=test")
        assertUnescape("monkey=", '=', 'test', '\\' as char, "=|}", "=", "monkey\\==test")
        assertUnescape("monkey=|", '=', 'test', '\\' as char, "=|}", "=", "monkey\\=\\|=test")
        assertUnescape("monkey=|}", '=', 'test', '\\' as char, "=|}", "=", "monkey\\=\\|\\}=test")
        assertUnescape("mon", '=', 'key\\=\\|\\}=test', '\\' as char, "=|}", "=", "mon=key\\=\\|\\}=test")
    }

    private void assertUnescape2(String expect, String expectDone, String expectRest,
                                 char escape, String validEscaped, List delim, String input) {
        RundeckLogFormat.UnescapedData unescapedData = RundeckLogFormat.unescape(input, escape, validEscaped, delim as String[])
        assert expect == unescapedData.unescaped : "input: " + input
        assert expectDone == unescapedData.delimiterReached : "input: " + input
        assert expectRest == unescapedData.remaining : "input: " + input
    }
    def "testUnescapeMetaValue"() {
        expect:
        assertUnescape2("test", '|', 'abc=def', '\\' as char, "|}", ["|","}"], "test|abc=def")
        assertUnescape2("t|est", '|', 'abc=def', '\\' as char, "|}", ["|", "}"], "t\\|est|abc=def")
        assertUnescape2("t|e}st", '|', 'abc=def', '\\' as char, "|}", ["|", "}"], "t\\|e\\}st|abc=def")

        assertUnescape2("test", '}', 'abc=def', '\\' as char, "|}", ["|", "}"], "test}abc=def")
        assertUnescape2("t|est", '}', 'abc=def', '\\' as char, "|}", ["|", "}"], "t\\|est}abc=def")
        assertUnescape2("t|e}st", '}', 'abc=def', '\\' as char, "|}", ["|", "}"], "t\\|e\\}st}abc=def")
    }
    def "testUnescapeMultidelimiter"() {
        expect:
        assertUnescape2("abc", null, null, '\\' as char, "1234", ["123","234"], "abc")
        assertUnescape2("abc12", null, null, '\\' as char, "1234", ["123","234"], "abc12")
        assertUnescape2("abc", '123', null, '\\' as char, "1234", ["123","234"], "abc123")
        assertUnescape2("abc", '123', 'def', '\\' as char, "1234", ["123","234"], "abc123def")
        assertUnescape2("abc23", null, null, '\\' as char, "1234", ["123","234"], "abc23")
        assertUnescape2("abc", '234', null, '\\' as char, "1234", ["123","234"], "abc234")
        assertUnescape2("abc", '234', 'def', '\\' as char, "1234", ["123","234"], "abc234def")

        assertUnescape2("abc234def", null, null, '\\' as char, "1234", ["123", "234"], "abc\\234def")
        assertUnescape2("abc123def", null, null, '\\' as char, "1234", ["123", "234"], "abc\\123def")
        assertUnescape2("abc123def12", null, null, '\\' as char, "1234", ["123", "234"], "abc\\123def12")
    }

    /**
     * Test what happens when there are no actual characters in the log line (occurs on multi-line logs lines
     * where the first line is a new line)
     *
     * E.g.
     * <pre>
     * ^2013-09-24T18:15:49Z|log|ERROR|{command=3-NodeDispatch-salt-api-exec|node=somehost|step=3|user=vagrant}|
     * Traceback (most recent call last):
     * with open (source_file, "r") as file:
     * ^
     * </pre>
     */
    def "testUnescapeEmptyLogLine"() {
        when:
        RundeckLogFormat.UnescapedData unescapedData = RundeckLogFormat.unescape("foo}|", '\\' as char, '=|}\\', ['|', '}'] as String[]);
        then:
        "foo" == unescapedData.unescaped
        "}" == unescapedData.delimiterReached
        "|" == unescapedData.remaining
    }

    /**
     * basic seek back with only log message lines
     */
    def "testSeekBackSimple"(){
        when:
        RundeckLogFormat format = new RundeckLogFormat()
        def f = File.createTempFile("log-format-test", ".rdlog")
        f.deleteOnExit()

        def line1 = '^2015-05-15T16:50:57Z|||{node=madmartigan.local|step=1|stepctx=1|user=greg}|testing execution output api-plain line 1^\n'
        def line2 = '^2015-05-15T16:50:57Z|||{node=madmartigan.local|step=1|stepctx=1|user=greg}|line 2^\n'
        def line3 = '^2015-05-15T16:50:57Z|||{node=madmartigan.local|step=1|stepctx=1|user=greg}|line 3^\n'
        def line4 = '^2015-05-15T16:50:57Z|||{node=madmartigan.local|step=1|stepctx=1|user=greg}|line 4 final^\n'
        f<<     line1 +
                line2 +
                line3 +
                line4
        then:
        0L == format.seekBackwards(f, 5)
        0L == format.seekBackwards(f, 4)
        line1.length() == format.seekBackwards(f, 3)
        (line1.length()+line2.length()) == format.seekBackwards(f, 2)
        (line1.length()+line2.length()+line3.length()) == format.seekBackwards(f, 1)

    }

    /**
     * seek back with interstitial log entries
     */
    def "testSeekBackMeta"(){
        when:
        RundeckLogFormat format = new RundeckLogFormat()
        def f = File.createTempFile("log-format-test", ".rdlog")
        f.deleteOnExit()

        def line1 = '^2015-05-15T16:50:57Z|||{node=madmartigan.local|step=1|stepctx=1|user=greg}|testing execution output api-plain line 1^\n'
        def line2 = '^2015-05-15T16:50:57Z|nodebegin||{node=madmartigan.local|step=1|stepctx=1|user=greg}|^\n'
        def line3 = '^2015-05-15T16:50:57Z|||{node=madmartigan.local|step=1|stepctx=1|user=greg}|testing execution output api-plain line 1^\n'
        def line4 = '^2015-05-15T16:50:58Z|nodeend||{node=madmartigan.local|step=1|stepctx=1|user=greg}|^\n'
        f<<     line1 + line2 + line3 + line4
        then:
        0L == format.seekBackwards(f, 2)
        (line1.length() + line2.length()) == format.seekBackwards(f, 1)
    }
    def "testSeekBackFull"(){
        when:
        RundeckLogFormat format = new RundeckLogFormat()
        def f = File.createTempFile("log-format-test", ".rdlog")
        f.deleteOnExit()

        def part1 = '^text/x-rundeck-log-v2.0^\n' +
                '^2015-05-15T16:50:57Z|stepbegin||{node=madmartigan.local|step=1|stepctx=1|user=admin}|^\n' +
                '^2015-05-15T16:50:57Z|nodebegin||{node=madmartigan.local|step=1|stepctx=1|user=greg}|^\n'

        def line1 = '^2015-05-15T16:50:57Z|||{node=madmartigan.local|step=1|stepctx=1|user=greg}|testing execution output api-plain line 1^\n'

        def line2 = '^2015-05-15T16:50:57Z|||{node=madmartigan.local|step=1|stepctx=1|user=greg}|line 2^\n'

        def line3 = '^2015-05-15T16:50:57Z|||{node=madmartigan.local|step=1|stepctx=1|user=greg}|line 3^\n'

        def line4 = '^2015-05-15T16:50:57Z|||{node=madmartigan.local|step=1|stepctx=1|user=greg}|line 4 final^\n'
        def endpart='^2015-05-15T16:50:58Z|nodeend||{node=madmartigan.local|step=1|stepctx=1|user=greg}|^\n' +
                '^2015-05-15T16:50:58Z|stepend||{node=madmartigan.local|step=1|stepctx=1|user=admin}|^\n' +
                '^END^\n'
        f<<     part1 +
                line1 +
                line2 +
                line3 +
                line4 +
                endpart
        then:
        0L == format.seekBackwards(f, 5)
        part1.length() == format.seekBackwards(f, 4)
        (part1.length() + line1.length()) == format.seekBackwards(f, 3)
        (part1.length() + line1.length() + line2.length()) == format.seekBackwards(f, 2)
        (part1.length() + line1.length() + line2.length() + line3.length()) == format.seekBackwards(f, 1)

    }
}
