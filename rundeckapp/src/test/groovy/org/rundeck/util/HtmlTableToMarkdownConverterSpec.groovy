/*
 * Copyright 2026 SimplifyOps Inc, <http://simplifyops.com>
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

/**
 * Covers converting hand-written HTML {@code <table>} blocks in job
 * description text into GFM Markdown pipe tables, without ever copying a
 * user-authored tag or attribute into the output.
 */
class HtmlTableToMarkdownConverterSpec extends Specification {

    def "simple table with plain td cells is converted to a markdown pipe table"() {
        given:
        String html = '<table><tr><td>a</td><td>b</td></tr><tr><td>c</td><td>d</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)

        then:
        result.contains('| a | b |')
        result.contains('| --- | --- |')
        result.contains('| c | d |')
        !result.toLowerCase().contains('<table')
        !result.toLowerCase().contains('<td')
    }

    def "table with th header row is marked as the markdown header"() {
        given:
        String html = '<table><tr><th>Name</th><th>Value</th></tr><tr><td>x</td><td>1</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        // convert() wraps the generated table in blank lines for block
        // separation from surrounding text -- trim them off before
        // asserting on the table's own line structure.
        List<String> lines = result.trim().readLines()

        then:
        lines[0] == '| Name | Value |'
        lines[1] == '| --- | --- |'
        lines[2] == '| x | 1 |'
    }

    def "extra whitespace and newlines inside tags do not break parsing"() {
        given:
        String html = '''<table>
            <tr>
                <th> Name </th>
                <th> Value </th>
            </tr>
            <tr>
                <td>
                    x
                </td>
                <td>1</td>
            </tr>
        </table>'''

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        List<String> lines = result.trim().readLines()

        then:
        lines[0] == '| Name | Value |'
        lines[2] == '| x | 1 |'
    }

    def "malformed unclosed table falls back to the original text unchanged"() {
        given:
        String html = 'before <table><tr><td>a</td></tr> after, no closing tag'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)

        then:
        result == html
    }

    def "style and class attributes on cells are stripped entirely, not escaped"() {
        given:
        String html = '<table><tr><td style="color:red" class="foo">text</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)

        then:
        result.contains('| text |')
        !result.contains('style')
        !result.contains('class')
        !result.contains('color:red')
    }

    def "description with no table at all is a no-op"() {
        given:
        String text = 'just **bold** markdown text, no tables here'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result.is(text)
    }

    def "a null or empty description is returned unchanged"() {
        expect:
        HtmlTableToMarkdownConverter.convert(null) == null
        HtmlTableToMarkdownConverter.convert('') == ''
    }

    def "table with surrounding markdown text converts only the table, leaving the rest untouched"() {
        given:
        String text = 'Some **bold** intro.\n\n' +
            '<table><tr><td>a</td><td>b</td></tr></table>\n\n' +
            'Trailing text with a [link](http://example.com).'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result.contains('Some **bold** intro.')
        result.contains('| a | b |')
        result.contains('Trailing text with a [link](http://example.com).')
        !result.toLowerCase().contains('<table')
    }

    def "non-table raw html such as a bare bold tag is left untouched"() {
        given:
        String text = 'some text <b>bold</b> more text'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == text
    }

    def "a row with no th cells still produces a valid header via the first row"() {
        given:
        String html = '<table><tr><td>a</td><td>b</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        List<String> lines = result.trim().readLines()

        then:
        lines[0] == '| a | b |'
        lines[1] == '| --- | --- |'
        lines.size() == 2
    }

    def "pipe characters in cell text are escaped so they cannot break the table structure"() {
        given:
        String html = '<table><tr><td>a | b</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)

        then:
        result.contains('a \\| b')
    }

    def "a table documented inside a fenced code block is left untouched"() {
        given:
        String text = 'Example:\n\n' +
            '```\n' +
            '<table><tr><td>a</td><td>b</td></tr></table>\n' +
            '```\n'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == text
    }

    def "a table documented inside an inline code span is left untouched"() {
        given:
        String text = 'Use `<table><tr><td>a</td></tr></table>` for a simple table.'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == text
    }

    def "a real table outside a fenced code block still converts even when a documented example is also present"() {
        given:
        String text = 'Example syntax:\n\n' +
            '```\n' +
            '<table><tr><td>example</td></tr></table>\n' +
            '```\n\n' +
            '<table><tr><td>real</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result.contains('<table><tr><td>example</td></tr></table>')
        result.contains('| real |')
    }

    def "a table nested inside a cell of another table falls back to the original text unchanged"() {
        given:
        String html = '<table><tr><td>before<table><tr><td>inner</td></tr></table>after</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)

        then:
        result == html
    }

    def "a quoted attribute containing a greater-than sign does not leak its value as cell text"() {
        given:
        String html = '<table><tr><td title="x>![evil](https://attacker.example/leak)">safe</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)

        then:
        result.contains('| safe |')
        !result.contains('attacker.example')
        !result.contains('evil')
    }

    def "a quoted attribute on a nested tag inside a cell does not leak its value as cell text"() {
        given:
        String html = '<table><tr><td><span title="x>![evil](https://attacker.example/leak)">safe</span></td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)

        then:
        result.contains('| safe |')
        !result.contains('attacker.example')
        !result.contains('evil')
    }

    def "markdown-active characters in plain cell text are escaped so they cannot become a live link, image, or formatting"() {
        given:
        String html = '<table><tr><td>![evil](https://attacker.example/leak) and *bold* and `code`</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        String rendered = rundeck.codecs.MarkdownCodec.decodeStr(result)

        then:
        // Confirm conversion actually ran -- a real table, not the original
        // HTML escaped wholesale (which would trivially satisfy the safety
        // assertions below without ever exercising escapeCell at all).
        rendered.contains('<table>')
        !rendered.contains('<img')
        !rendered.contains('<a ')
        !rendered.contains('<strong>')
        !rendered.contains('<code>')
        rendered.contains('attacker.example')
    }

    def "a header with fewer cells than a data row (e.g. from colspan) does not drop the data row's extra values"() {
        given:
        String html = '<table><tr><th colspan="2">Title</th></tr><tr><td>A</td><td>B</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        List<String> lines = result.trim().readLines()

        then:
        lines[0] == '| Title |  |'
        lines[1] == '| --- | --- |'
        lines[2] == '| A | B |'
    }

    def "a fake closing table tag hidden inside a quoted attribute is skipped, and the real closing tag is still found"() {
        given:
        // The fake "</table>" -- and an exploit payload alongside it -- live
        // entirely inside a quoted title attribute; the real closing tag is
        // the one at the very end. A vulnerable scanner would stop at the
        // fake one, truncate the table there, and leak the rest of the
        // attribute plus the trailing text verbatim as live Markdown.
        String html = '<table><tr><td>a</td></tr><tr>' +
            '<td title="</table>![evil](https://attacker.example/leak)">b</td>' +
            '</tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        String rendered = rundeck.codecs.MarkdownCodec.decodeStr(result)

        then:
        // Attribute content must never surface anywhere -- the cell's real,
        // visible text is just "b".
        result.contains('| b |')
        !rendered.contains('<img')
        !rendered.contains('attacker.example')
        !rendered.contains('evil')
    }

    def "a fake closing row or cell tag hidden inside a nested tag's quoted attribute does not leak its value as cell text"() {
        given:
        String html = '<table><tr><td>' +
            '<span title="</td></tr></table>![evil](https://attacker.example/leak)">safe</span>' +
            '</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        String rendered = rundeck.codecs.MarkdownCodec.decodeStr(result)

        then:
        result.contains('| safe |')
        !rendered.contains('<img')
        !rendered.contains('attacker.example')
        !rendered.contains('evil')
    }

    def "a fake closing table tag hidden inside an HTML comment does not truncate the match and leak the comment's contents"() {
        given:
        // Mirrors the quoted-attribute exploit, but the fake closing tag and
        // payload live inside an HTML comment instead -- comments can
        // contain a bare ">" with no escaping, so a scanner that only
        // tracks quotes (and not comments) would stop here too.
        String html = '<table><tr><td>a</td></tr><tr><td>' +
            '<!-- </table>![evil](https://attacker.example/leak) -->' +
            'b</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        String rendered = rundeck.codecs.MarkdownCodec.decodeStr(result)

        then:
        result.contains('| b |')
        !rendered.contains('<img')
        !rendered.contains('attacker.example')
        !rendered.contains('evil')
    }

    def "a fenced code block with a longer closing fence than opening fence is still recognized, per CommonMark"() {
        given:
        String text = 'Example:\n\n' +
            '~~~\n' +
            '<table><tr><td>a</td></tr></table>\n' +
            '~~~~\n'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == text
    }

    def "an unclosed fenced code block extends to the end of the text and its table example is not converted"() {
        given:
        String text = 'Example:\n\n' +
            '```\n' +
            '<table><tr><td>a</td></tr></table>\n'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == text
    }

    def "a stray unmatched table tag inside a documented example does not prevent a separate real table from converting"() {
        given:
        String text = 'Use `<table>` to start a table.\n\n' +
            '<table><tr><td>real</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result.contains('Use `<table>` to start a table.')
        result.contains('| real |')
    }

    def "backticks inside a real table cell do not cause the whole table to be skipped as a documented example"() {
        given:
        String html = '<table><tr><td>Use `value` here</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)

        then:
        result.contains('Use \\`value\\` here')
    }

    def "a fake closing table tag hidden inside a script element's raw text does not truncate the match and leak it as live markup"() {
        given:
        // <script> content is raw text per HTML -- everything up to the real
        // </script> is literal, never markup. A scanner that doesn't know
        // this would treat the "</table>" typed here as a real closing tag,
        // truncating the table before its second row and leaking the rest
        // of the script body as live trailing Markdown.
        String html = '<table><tr><td>safe</td></tr>' +
            '<script>var x = "</table>![evil](https://attacker.example/leak)";</script>' +
            '<tr><td>b</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        String rendered = rundeck.codecs.MarkdownCodec.decodeStr(result)

        then:
        // Both rows must convert -- proving the real closing </table> was
        // found, not the fake one inside the script body.
        result.contains('| safe |')
        result.contains('| b |')
        !rendered.contains('<img')
        !rendered.contains('attacker.example')
        !rendered.contains('evil')
    }

    def "a table tag hidden inside a code span within a real table's cell is treated as genuine nesting, not a documented example"() {
        given:
        // The fake opening "<table>" is hidden inside backticks (normally a
        // documented-example signal), but the closing "</table>" right after
        // it is live markup. Code-range skipping must not apply once we are
        // already inside a real table, or the real close is found too
        // early and the trailing image syntax leaks out unescaped.
        String html = '<table><tr><td>`<table>`<tr><td>inner</td></tr></table>' +
            '![evil](https://attacker.example/leak)</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        String rendered = rundeck.codecs.MarkdownCodec.decodeStr(result)

        then:
        // Falls back to the safe, unchanged-text path (nested table) rather
        // than emitting a truncated, partially-converted result -- the
        // whole input is then escaped as one inert block by the existing,
        // unmodified MarkdownCodec, same as any other unconverted raw HTML.
        result == html
        !rendered.contains('<img')
        !rendered.contains('<a ')
    }

    def "a literal less-than sign in prose before a real table does not prevent the table from converting"() {
        given:
        String text = 'Threshold < 5.\n\n<table><tr><td>a</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result.contains('Threshold < 5.')
        result.contains('| a |')
    }

    def "a literal less-than sign before a genuinely nested table does not absorb the nested tag and truncate the real close early"() {
        given:
        // The literal "1 < 2" must not be treated as a tag that consumes
        // everything up to the next unquoted ">" -- which would otherwise
        // absorb the nested "<table>" immediately after it, so it's never
        // recognized as nesting and the outer close ends up matched to the
        // INNER table's close instead, truncating early.
        String html = '<table><tr><td>safe</td></tr><tr><td>1 < 2 ' +
            '<table><tr><td>inner</td></tr></table>' +
            '![evil](https://attacker.example/leak)</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        String rendered = rundeck.codecs.MarkdownCodec.decodeStr(result)

        then:
        // Falls back to the safe, unchanged-text path (nested table).
        result == html
        !rendered.contains('<img')
        !rendered.contains('<a ')
    }

    def "converting a table already separated by a single newline does not add extra blank lines"() {
        given:
        String text = 'Intro.\n<table><tr><td>a</td></tr></table>\nOutro.'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == 'Intro.\n\n| a |\n| --- |\n\nOutro.'
    }

    def "converting a table already separated by a full blank line does not add any extra blank lines"() {
        given:
        String text = 'Intro.\n\n<table><tr><td>a</td></tr></table>\n\nOutro.'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == 'Intro.\n\n| a |\n| --- |\n\nOutro.'
    }

    def "a line break tag inserts a space instead of merging the surrounding text"() {
        given:
        String html = '<table><tr><td>first<br>second</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)

        then:
        result.contains('| first second |')
    }
}
