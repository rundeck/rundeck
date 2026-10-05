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
}
