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

    def "a th row that is not the first row does not get promoted to the top, reordering the author's rows"() {
        given:
        // <th> appears on the THIRD row here, not the first -- the first
        // row ("Jan"/"10") must stay the header/first line, in its original
        // position, rather than being pushed down because some later row
        // happens to use <th> cells.
        String html = '<table>' +
            '<tr><td>Jan</td><td>10</td></tr>' +
            '<tr><td>Feb</td><td>20</td></tr>' +
            '<tr><th>Month</th><th>Value</th></tr>' +
            '</table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        List<String> lines = result.trim().readLines()

        then:
        lines[0] == '| Jan | 10 |'
        lines[1] == '| --- | --- |'
        lines[2] == '| Feb | 20 |'
        lines[3] == '| Month | Value |'
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

    def "firstLineContainsTable recognizes a table anywhere on the first physical line"() {
        expect:
        HtmlTableToMarkdownConverter.firstLineContainsTable(text) == expected

        where:
        text                                                    | expected
        '<table><tr><td>a</td></tr></table>'                     | true
        '<TABLE><tr><td>a</td></tr></TABLE>'                     | true
        'Intro <table><tr><td>a</td></tr></table>'               | true
        '<b>bold</b> then <table><tr><td>a</td></tr></table>'    | true
        // The table is on the second physical line here, not the first --
        // the ordinary split already hands it the table intact, so no
        // special-casing is needed (or applied).
        '   \n  <table><tr><td>a</td></tr></table>'              | false
        'Some text\n\n<table><tr><td>a</td></tr></table>'        | false
        '<tablecloth>not a table</tablecloth>'                   | false
        // A documented example, not a live table -- convert() would leave
        // it untouched too, so there is no reason to blank the summary line.
        'Use `<table>` syntax.\nDetails'                         | false
        'See <!-- <table> --> below.\nDetails'                   | false
        '<img alt="<table></table>">\nDetails'                   | false
        // Unbalanced -- convert() will fail safe and return the text
        // unchanged, so there is nothing to protect the summary line from.
        '<table><tr><td>a</td></tr>'                             | false
        // CR-only line ending (matches UtilityTagLib's own \r/\r\n/\n
        // splitting) -- the table is on the second line, not the first.
        'Intro\r<table><tr><td>a</td></tr></table>'              | false
        // Nested or unextractable: convert() returns the text unchanged for
        // these, so the summary line must not be blanked for them.
        '<table><tr><td><table><tr><td>x</td></tr></table></td></tr></table>' | false
        '<table></table>'                                        | false
        '<table><tr><td>a</td></tr></table>\n\n<table></table>'  | false
        null                                                     | false
        ''                                                       | false
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

    def "a bare bold tag is converted to Markdown bold"() {
        given:
        String text = 'some text <b>bold</b> more text'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == 'some text **bold** more text'
    }

    def "strong, italic and em tags are converted to their Markdown equivalents"() {
        expect:
        HtmlTableToMarkdownConverter.convert(html) == markdown

        where:
        html                     | markdown
        '<strong>bold</strong>'  | '**bold**'
        '<i>italic</i>'          | '*italic*'
        '<em>italic</em>'        | '*italic*'
    }

    def "attributes on an inline style tag are dropped, not copied"() {
        given:
        String text = '<b title="x" onclick="evil()">bold</b>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == '**bold**'
    }

    def "an inline style tag containing a nested tag is left untouched"() {
        given:
        // Fail-safe: nested emphasis combinations are not supported, rather
        // than guess at how to render them.
        String text = '<b>bold and <i>nested</i></b>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == text
    }

    def "a bold tag inside a fenced code example is left untouched"() {
        given:
        String text = 'Example:\n```\n<b>bold</b>\n```\n'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == text
    }

    def "bold text before and after a table is converted alongside it"() {
        given:
        String text = '<b>before</b>\n\n<table><tr><td>a</td></tr></table>\n\n<b>after</b>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result.contains('**before**')
        result.contains('| a |')
        result.contains('**after**')
    }

    def "a closing tag hidden inside an html comment does not truncate the match early"() {
        given:
        // If the comment-hidden "</b>" were mistaken for the real close, the
        // match would end there, leaving the image syntax after it outside
        // the replaced (and therefore inert) span.
        String text = '<b>safe<!-- </b>![probe](https://example.invalid/pixel) -->after</b>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == '**safeafter**'
        !result.contains('![probe]')
    }

    def "many unmatched opening tags do not cause quadratic scanning"() {
        given:
        // None of these ever close, so nothing should convert -- this
        // exercises the per-tag-name "no closing tag remaining" cache rather
        // than re-scanning the rest of the text for every occurrence.
        String text = ('<b>x' * 2000) + 'end'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == text
    }

    def "an empty or whitespace-only inline style tag does not produce a bare run of emphasis markers"() {
        expect:
        HtmlTableToMarkdownConverter.convert(html) == expected

        where:
        html                | expected
        '<b></b>'           | ''
        '<strong></strong>' | ''
        '<b>   </b>'        | '   '
        '<i></i>'           | ''
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

    def "a table documented inside an inline code span that spans multiple lines is left untouched"() {
        given:
        // A code span's content can legally contain line breaks.
        String text = 'Use ```<table>\n<tr><td>x</td></tr>\n</table>``` for a table.'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == text
    }

    def "a stray backtick inside a fenced block does not pair with one after a real table and swallow it"() {
        given:
        String text = '~~~\n' +
            'echo `unmatched\n' +
            '~~~\n\n' +
            '<table><tr><td>a</td></tr></table>\n\n' +
            'See the ` above.'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result.contains('| a |')
        !result.contains('<table>')
    }

    def "a stray backtick does not pair with one in a later paragraph and swallow a table in between"() {
        given:
        // An inline code span can contain line breaks but never a blank
        // line -- it cannot cross a paragraph boundary.
        String text = 'A stray ` here.\n\n' +
            '<table><tr><td>a</td></tr></table>\n\n' +
            'And another ` here.'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result.contains('| a |')
        !result.contains('<table>')
    }

    def "a backtick run inside a code span that does not match the opening run's length does not close it"() {
        given:
        // A single backtick inside a two-backtick-delimited span is just
        // content -- only another run of exactly two backticks closes it.
        String text = 'Use ``<table>`<tr><td>x</td></tr>`</table>`` for a table.'

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

    def "a complete table followed by an unclosed table aborts all conversion, not just the broken one"() {
        given:
        // Per the fail-safe contract: an unbalanced table anywhere in the
        // text means nothing converts, not even the earlier, well-formed one.
        String html = '<table><tr><td>a</td></tr></table>\n<table><tr><td>b</td></tr>'

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

    def "fenced and indented code scanning recognizes CR and CRLF line endings"() {
        expect:
        // The closed fence protects its example, and the real table after
        // it still converts -- with only "\n" recognized, CR-only text was
        // one giant unclosed fence line that swallowed the table.
        String result = HtmlTableToMarkdownConverter.convert(text)
        result.contains('<table><tr><td>example</td></tr></table>')
        result.contains('| real |')

        where:
        text << [
            '~~~\r<table><tr><td>example</td></tr></table>\r~~~\r\r<table><tr><td>real</td></tr></table>\r',
            '~~~\r\n<table><tr><td>example</td></tr></table>\r\n~~~\r\n\r\n<table><tr><td>real</td></tr></table>\r\n',
            '    <table><tr><td>example</td></tr></table>\r\r<table><tr><td>real</td></tr></table>\r',
        ]
    }

    def "a fenced code block indented inside a list item protects its documented table example"() {
        given:
        // 2 columns of list-content indent + 2 of fence indent: valid
        // CommonMark, but more than the 3 a top-level fence may have.
        String text = '- item\n' +
            '    ~~~\n' +
            '    <table><tr><td>a</td></tr></table>\n' +
            '    ~~~\n'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == text
    }

    def "an unclosed fence inside a list item ends with the list item instead of swallowing what follows"() {
        given:
        String text = '- item\n' +
            '    ~~~\n' +
            '    <table><tr><td>example</td></tr></table>\n' +
            '\n' +
            'Back at top level:\n\n' +
            '<table><tr><td>real</td></tr></table>\n'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result.contains('    <table><tr><td>example</td></tr></table>')
        result.contains('| real |')
    }

    def "a blockquote fence tolerates a bare marker line and ends where the blockquote does"() {
        given:
        // ">" with no trailing space on the blank line is how most editors
        // write an empty quoted line; the fence must survive it. The
        // unquoted table after the quote is top-level and must convert.
        String text = '> ~~~\n' +
            '>\n' +
            '> <table><tr><td>example</td></tr></table>\n' +
            '> ~~~\n\n' +
            '<table><tr><td>real</td></tr></table>\n'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result.contains('> <table><tr><td>example</td></tr></table>')
        result.contains('| real |')
    }

    def "a fenced code block inside a blockquote protects its documented table example"() {
        given:
        String text = '> ~~~\n' +
            '> <table><tr><td>a</td></tr></table>\n' +
            '> ~~~\n'

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

    def "a table as the only content of a blockquote stays inside the blockquote when rendered"() {
        given:
        String text = '> <table><tr><td>a</td><td>b</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)
        String rendered = rundeck.codecs.MarkdownCodec.decodeStr(result)

        then:
        result == '> | a | b |\n> | --- | --- |'
        int blockquoteStart = rendered.indexOf('<blockquote>')
        int tableStart = rendered.indexOf('<table>')
        int blockquoteEnd = rendered.indexOf('</blockquote>')
        blockquoteStart >= 0
        tableStart > blockquoteStart
        tableStart < blockquoteEnd
    }

    def "a table continuing a blockquote paragraph (no blank line before it) falls back safely instead of merging into garbled text"() {
        given:
        // Verified empirically: commonmark does not let a table interrupt a
        // paragraph lazily continued inside the same blockquote this way --
        // attempting the prefix here would merge the pipe-table syntax into
        // the preceding paragraph as plain text, rendering no table at all,
        // which is worse than the existing (pre-this-feature) behavior.
        String text = '> Quote text\n> <table><tr><td>a</td><td>b</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)
        String rendered = rundeck.codecs.MarkdownCodec.decodeStr(result)

        then:
        rendered.contains('<table>')
        rendered.contains('<blockquote>')
    }

    def "a table immediately after a list item marker stays inside the list item when rendered"() {
        given:
        String text = '- <table><tr><td>a</td><td>b</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)
        String rendered = rundeck.codecs.MarkdownCodec.decodeStr(result)

        then:
        result == '- | a | b |\n  | --- | --- |'
        int listStart = rendered.indexOf('<li>')
        int tableStart = rendered.indexOf('<table>')
        int listEnd = rendered.indexOf('</li>')
        listStart >= 0
        tableStart > listStart
        tableStart < listEnd
    }

    def "a table continuing a list item paragraph (no blank line before it) falls back safely instead of merging into garbled text"() {
        given:
        String text = '- Item text\n  <table><tr><td>a</td><td>b</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)
        String rendered = rundeck.codecs.MarkdownCodec.decodeStr(result)

        then:
        rendered.contains('<table>')
    }

    def "a table NOT directly after a recognizable marker falls back to ordinary top-level formatting"() {
        given:
        // Plain indentation alone (no marker) is deliberately not treated as
        // a continuation context -- too ambiguous to guess at safely. Two
        // spaces stay below the 4-column indented-code-block threshold, so
        // this exercises the "no marker" fallback, not code-block protection.
        String text = '  <table><tr><td>a</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result.contains('| a |')
        !result.contains('  | a |')
    }

    def "a table indented 4+ spaces at the start of a block is a CommonMark indented code block, not a live table"() {
        given:
        // Per CommonMark, 4+ columns of indentation at the start of a block
        // is an indented code block -- the exact documented-example case
        // this protection exists for, so it must render as code, unchanged.
        String text = '    <table><tr><td>a</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(text)

        then:
        result == text
        !result.contains('| a |')
    }

    def "a wide row followed by many narrow rows does not cause quadratic padding blow-up"() {
        given:
        // One 101-cell row plus 100 single-cell rows pads out to
        // 101 * 101 = 10201 cells, just over the budget -- exercises the
        // guard without needing a slow, huge input.
        StringBuilder html = new StringBuilder('<table><tr>')
        101.times { html.append('<td>w</td>') }
        html.append('</tr>')
        100.times { html.append('<tr><td>n</td></tr>') }
        html.append('</table>')

        when:
        String result = HtmlTableToMarkdownConverter.convert(html.toString())

        then:
        // Falls back to the safe, unchanged-text path rather than padding
        // every narrow row out to 101 columns.
        result == html.toString()
    }

    def "a line break tag inserts a space instead of merging the surrounding text"() {
        given:
        String html = '<table><tr><td>first<br>second</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)

        then:
        result.contains('| first second |')
    }

    def "a td with an omitted end tag before the next td does not lose its text"() {
        given:
        // Valid HTML: a td's end tag may be omitted immediately before the
        // next td -- it is implicitly closed, not overwritten.
        String html = '<table><tr><td>a<td>b</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)

        then:
        result.contains('| a | b |')
    }

    def "a tr with an omitted end tag before the next tr does not lose its row"() {
        given:
        // Valid HTML: a tr's end tag may be omitted immediately before the
        // next tr -- the row (and its still-open cell) must still be
        // finalized, not discarded.
        String html = '<table><tr><td>a<tr><td>b</td></tr></table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        List<String> lines = result.trim().readLines()

        then:
        lines[0] == '| a |'
        lines[1] == '| --- |'
        lines[2] == '| b |'
    }

    def "a table whose final cell and row omit their end tags entirely does not lose that content"() {
        given:
        // Valid HTML: the table's own close implies the still-open tr/td's
        // close -- nothing explicitly closes them before </table>.
        String html = '<table><tr><td>a</td></tr><tr><td>last</table>'

        when:
        String result = HtmlTableToMarkdownConverter.convert(html)
        List<String> lines = result.trim().readLines()

        then:
        lines[0] == '| a |'
        lines[1] == '| --- |'
        lines[2] == '| last |'
    }
}
