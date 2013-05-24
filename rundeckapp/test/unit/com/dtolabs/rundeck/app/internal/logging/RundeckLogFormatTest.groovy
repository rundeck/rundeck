package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogLevel

import java.text.SimpleDateFormat

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/23/13
 * Time: 6:26 PM
 */
class RundeckLogFormatTest extends GroovyTestCase {
    SimpleDateFormat dateFormat
    DefaultLogEvent event
    String expectPrefix1
    String expectLineEnd

    @Override
    protected void setUp() throws Exception {
        super.setUp()    //To change body of overridden methods use File | Settings | File Templates.
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        event = new DefaultLogEvent()
        event.logLevel = LogLevel.DEBUG
        event.eventType = "log"
        event.datetime = dateFormat.parse('2013-05-24T01:31:02Z')
        event.metadata = [test: "1", something: "else"]
        expectPrefix1 = "^2013-05-24T01:31:02Z|DEBUG|{something=else|test=1}|"
        expectLineEnd = "^"
    }

    void testBackslashEscape(){
        assertEquals("monkey \\^\\^\\^ blah \\\\ elf \\\\\\\\", RundeckLogFormat.backslashEscape("monkey ^^^ blah \\ elf \\\\",'^'))
    }
    void testBasic() {
        RundeckLogFormat format = new RundeckLogFormat()
        assertEquals("^text/x-rundeck-log-v2.0^", format.outputBegin())
        assertEquals("^END^", format.outputFinish())
    }

    private void expectMessage(String expect) {
        RundeckLogFormat format = new RundeckLogFormat()
        assertEquals(expectPrefix1 + expect + expectLineEnd, format.outputEvent(event))
    }

    void testOutputEventSimple() {
        event.message = "test message"
        def expect = "test message"
        expectMessage(expect)
    }

    void testOutputEventMultiline() {
        event.message = "test message\nanother line"
        def expect = "test message\nanother line"
        expectMessage(expect)
    }

    void testOutputEventEscapeMessage() {
        event.message = "This is a | weird \\asdf ^ message"
        def expect = "This is a | weird \\\\asdf \\^ message"
        expectMessage(expect)
    }

    void testOutputEventEscapeMeta() {
        //nb: current meta does not escape, but eliminates unexpected chars
        RundeckLogFormat format = new RundeckLogFormat()
        event.message = "test"
        event.metadata = ["aomething=": "=else", "best|as}df": "flif}|="]
        assertEquals("^2013-05-24T01:31:02Z|DEBUG|{aomething\\==\\=else|best\\|as\\}df=flif\\}\\|\\=}|test^",
                format.outputEvent(event))
    }

    void testInputFileStart() {
        RundeckLogFormat format = new RundeckLogFormat()
        LineLogFormat.FormatItem item = format.parseLine(RundeckLogFormat.FILE_START)
        assertTrue(item.fileStart)
        assertFalse(item.invalid)
    }

    void testInputFileEnd() {
        RundeckLogFormat format = new RundeckLogFormat()
        LineLogFormat.FormatItem item = format.parseLine(RundeckLogFormat.FILE_END)
        assertTrue(item.fileEnd)
        assertFalse(item.invalid)
    }
    void testInputBlank() {
        RundeckLogFormat format = new RundeckLogFormat()
        LineLogFormat.FormatItem item = format.parseLine("")
        assertFalse(item.toString(),item.fileEnd)
        assertFalse(item.invalid)
        assertEquals("\n",item.partial)
    }

    void testInputBasic() {
        RundeckLogFormat format = new RundeckLogFormat()
        def expectMessage = "test message"
        def line = expectPrefix1 + expectMessage + expectLineEnd
        LineLogFormat.FormatItem item = format.parseLine(line)
        def assertMsg = item.toString() + ";" + line
        assertTrue(assertMsg, item.lineComplete)
        assertFalse(assertMsg, item.invalid)
        assertNotNull(assertMsg, item.entry)
        assertEquals(assertMsg, expectMessage, item.entry.message)
        assertEquals(assertMsg, event.metadata, item.entry.metadata)
        assertEquals(assertMsg, event.logLevel, item.entry.logLevel)
        assertEquals(assertMsg, event.datetime, item.entry.datetime)
        assertEquals(assertMsg, /*event.eventType*/ null, item.entry.eventType)
    }

    void assertDecode(String expect, String expectDone, String input) {
        def (testString, testDone) = RundeckLogFormat.decodeLog(input)
        assertEquals("input: " + input, expect, testString)
        assertEquals("input: " + input, expectDone, testDone)
    }

    void testDecodeLog() {
        assertDecode("abc", null, "abc")
        assertDecode("abc", '^', "abc^^^")
        assertDecode("abc\\", '^', "abc\\\\^^^")
        assertDecode("abc\\", null, "abc\\\\")
        assertDecode("abc\\^", '^', "abc\\\\\\^^^^")
        assertDecode("abc\\^", null, "abc\\\\\\^")
        assertDecode("abc^^^asdf", null, "abc\\^\\^\\^asdf")
        assertDecode("abc^^^asdf", '^', "abc\\^\\^\\^asdf^^^")
    }

    void assertUnescape(String expect, String expectDone, String expectRest,
                        char escape, String validEscaped, String delim, String input) {
        def (testString, testDone, rest) = RundeckLogFormat.unescape(input,escape,validEscaped,delim)
        assertEquals("input: " + input, expect, testString)
        assertEquals("input: " + input, expectDone, testDone)
        assertEquals("input: " + input, expectRest, rest)
    }
    void testUnescapeBasic() {
        assertUnescape("abc", null, null, '\\' as char, "\\^", "^^^", "abc")
    }
    void testUnescapeEndline(){
        assertUnescape("abc", '^^^', null, '\\' as char, "\\^", "^^^", "abc^^^")
    }
    void testUnescapeValidEscapechar(){
        assertUnescape("abc\\", '^^^', null, '\\' as char, "\\^", "^^^", "abc\\\\^^^")
        assertUnescape("abc^", '^^^', null, '\\' as char, "\\^", "^^^", "abc\\^^^^")
    }
    void testUnescapeLiteralEscapechar(){
        assertUnescape("abc^\\e", '^^^', null, '\\' as char, "\\^", "^^^", "abc\\^\\e^^^")
    }
    void testUnescapeMessageRest() {
        assertUnescape("abc", '^^^', 'alpha', '\\' as char, "\\^", "^^^", "abc^^^alpha")
        assertUnescape("abc", '^^^', 'alpha\\^\\\\^^^asdf', '\\' as char, "\\^", "^^^", "abc^^^alpha\\^\\\\^^^asdf")
    }
    void testUnescapeMetaKey() {
        assertUnescape("monkey", '=', 'test', '\\' as char, "=|}", "=", "monkey=test")
        assertUnescape("monkey=", '=', 'test', '\\' as char, "=|}", "=", "monkey\\==test")
        assertUnescape("monkey=|", '=', 'test', '\\' as char, "=|}", "=", "monkey\\=\\|=test")
        assertUnescape("monkey=|}", '=', 'test', '\\' as char, "=|}", "=", "monkey\\=\\|\\}=test")
        assertUnescape("mon", '=', 'key\\=\\|\\}=test', '\\' as char, "=|}", "=", "mon=key\\=\\|\\}=test")
    }

    void assertUnescape2(String expect, String expectDone, String expectRest,
                        char escape, String validEscaped, List delim, String input) {
        def (testString, testDone, rest) = RundeckLogFormat.unescape(input, escape, validEscaped, delim as String[])
        assertEquals("input: " + input, expect, testString)
        assertEquals("input: " + input, expectDone, testDone)
        assertEquals("input: " + input, expectRest, rest)
    }
    void testUnescapeMetaValue() {
        assertUnescape2("test", '|', 'abc=def', '\\' as char, "|}", ["|","}"], "test|abc=def")
        assertUnescape2("t|est", '|', 'abc=def', '\\' as char, "|}", ["|", "}"], "t\\|est|abc=def")
        assertUnescape2("t|e}st", '|', 'abc=def', '\\' as char, "|}", ["|", "}"], "t\\|e\\}st|abc=def")

        assertUnescape2("test", '}', 'abc=def', '\\' as char, "|}", ["|", "}"], "test}abc=def")
        assertUnescape2("t|est", '}', 'abc=def', '\\' as char, "|}", ["|", "}"], "t\\|est}abc=def")
        assertUnescape2("t|e}st", '}', 'abc=def', '\\' as char, "|}", ["|", "}"], "t\\|e\\}st}abc=def")
    }
    void testUnescapeMultidelimiter() {
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
}
