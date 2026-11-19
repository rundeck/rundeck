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

package rundeck.codecs

import org.rundeck.util.HtmlTableToMarkdownConverter
import spock.lang.Specification
import spock.lang.Unroll

/**
 * MarkdownCodecSpec covers PS-1683: raw inline HTML in markdown must be
 * escaped rather than passed through as live markup, while normal
 * markdown syntax keeps rendering. It also covers RUN-4945: HTML comments
 * (e.g. MOTD directive comments) must not leak into the output as visible
 * escaped text, while any non-comment content is still escaped per PS-1683.
 */
class MarkdownCodecSpec extends Specification {

    def "normal markdown still renders"() {
        expect:
        MarkdownCodec.decodeStr('**bold**') ==
            '<article class="markdown-body"><p><strong>bold</strong></p>\n</article>'
    }

    def "raw HTML in markdown is escaped"() {
        expect:
        MarkdownCodec.decodeStr(
            'some text\n\n<td style="position:fixed;inset:0;background:url(\'//attacker/leak\')">x</td>'
        ) ==
            '<article class="markdown-body"><p>some text</p>\n' +
            '<p>&lt;td style&#61;&#34;position:fixed;inset:0;background:url(&#39;//attacker/leak&#39;)&#34;&gt;x&lt;/td&gt;</p>\n</article>'
    }

    /**
     * A CSS-injection payload (a style attribute with position:fixed and a
     * background url()), placed inside an HTML table, must come out of the
     * full HtmlTableToMarkdownConverter -> MarkdownCodec -> SanitizedHTMLCodec
     * pipeline with no live "style" attribute and no trace of the injected
     * CSS/URL anywhere -- proving the table-to-markdown conversion does not
     * reintroduce that vector. This passes with zero changes to
     * MarkdownCodec.groovy or SanitizedHTMLCodec.groovy: the style attribute
     * is discarded by the converter itself, long before either codec sees
     * the text.
     */
    def "CSS-injection payload inside a table is neutralized by the converter"() {
        given:
        String payload = '<table><tr><td style="position:fixed;inset:0;width:100vw;height:100vh;' +
            'background:url(\'//attacker/leak\')">x</td></tr></table>'

        when:
        String converted = HtmlTableToMarkdownConverter.convert(payload)
        String rendered = MarkdownCodec.decodeStr(converted)

        then:
        // Pattern-based, case-insensitive, whitespace-tolerant checks --
        // substring checks here could miss variants like "STYLE=" or
        // "style =", or false-positive on the word "style" appearing
        // incidentally elsewhere.
        !(rendered =~ /(?i)\bstyle\s*=/).find()
        !(rendered =~ /(?i)position\s*:\s*fixed/).find()
        !(rendered =~ /(?i)\burl\s*\(/).find()
        // A single-row table has no body rows left once that row becomes the
        // GFM header row (required by pipe-table syntax), so the safe cell
        // text is rendered inside a <th>, not a <td> -- either is a real,
        // live table cell, as opposed to escaped literal tag text.
        rendered ==~ /(?s).*<t[hd]>x<\/t[hd]>.*/
        // Converter must produce a real table, not raw HTML text.
        rendered.contains('<table>')
    }

    def "HTML comment block directives are not rendered as visible text"() {
        given:
        String motd = '<!-- fgcolor: #ffffff -->\n<!-- bgcolor: #000000 -->\n<!-- style:danger -->\n\n# Welcome\n\nhello'

        when:
        String rendered = MarkdownCodec.decodeStr(motd)

        then:
        rendered.contains('<h1>Welcome</h1>')
        rendered.contains('hello')
        !rendered.contains('fgcolor')
        !rendered.contains('bgcolor')
        !rendered.contains('style:danger')
        !rendered.contains('&lt;!--')
        !rendered.contains('<!--')
    }

    def "inline HTML comment is dropped but surrounding text is kept"() {
        when:
        String rendered = MarkdownCodec.decodeStr('before <!-- fgcolor: #ffffff --> after')

        then:
        rendered.contains('before')
        rendered.contains('after')
        !rendered.contains('fgcolor')
        !rendered.contains('&lt;!--')
    }

    def "multiple comments on one line are all dropped"() {
        when:
        String rendered = MarkdownCodec.decodeStr('<!-- a --> <!-- b -->')

        then:
        !rendered.contains('&lt;!--')
        !rendered.contains('a --')
        !rendered.contains('b --')
    }

    def "comment followed by markup on the same line is escaped, not passed through"() {
        when:
        String rendered = MarkdownCodec.decodeStr('<!-- fgcolor: #ffffff --> <img src=x onerror=alert(1)>')

        then:
        !rendered.contains('<img')
        !(rendered =~ /(?i)<img[^>]*onerror\s*=/).find()
        rendered.contains('&lt;img')
    }

    def "comment wrapping a script tag followed by live markup is escaped"() {
        when:
        String rendered = MarkdownCodec.decodeStr('<!-- x --><script>alert(1)</script>')

        then:
        rendered.contains('&lt;script')
        !rendered.contains('<script')
    }

    def "unterminated comment opener is escaped"() {
        when:
        String rendered = MarkdownCodec.decodeStr('<!-- not closed\n\ntext')

        then:
        rendered.contains('&lt;!--')
        rendered.contains('text')
        !rendered.contains('<!--')
    }

    def "comment inside a fenced code block is still shown as code"() {
        when:
        String rendered = MarkdownCodec.decodeStr('```\n<!-- fgcolor: #ffffff -->\n```')

        then:
        rendered.contains('<pre>')
        rendered.contains('<code>')
        rendered.contains('&lt;!-- fgcolor: #ffffff --&gt;')
    }

    def "raw HTML escaping from RUN-4543 is still active for non-comment HTML"() {
        when:
        String rendered = MarkdownCodec.decodeStr('<td style="position:fixed">x</td>')

        then:
        rendered.contains('&lt;td')
        !(rendered =~ /(?i)\bstyle\s*=/).find()
    }

    /**
     * Exercises the renderer directly, without SanitizedHTMLCodec, because the
     * OWASP sanitizer strips real HTML comments on its own and would otherwise
     * hide a renderer that emitted the comment raw instead of dropping it.
     */
    def "renderer emits nothing for comment-only input before sanitization"() {
        when:
        String rendered = MarkdownCodec.renderer.render(
            MarkdownCodec.parser.parse('<!-- fgcolor: #ffffff -->\n<!-- style:danger -->')
        )

        then:
        rendered.trim() == ''
    }

    def "renderer escapes a non-comment inline HTML node following a comment"() {
        when:
        String rendered = MarkdownCodec.renderer.render(
            MarkdownCodec.parser.parse('text <!-- x --><img src=x onerror=alert(1)> more')
        )

        then:
        rendered.contains('&lt;img')
        !rendered.contains('<img')
        !rendered.contains('<!--')
    }

    def "inline HTML comment followed by markup is escaped through the full pipeline"() {
        when:
        String rendered = MarkdownCodec.decodeStr('text <!-- x --><img src=x onerror=alert(1)> more')

        then:
        rendered.contains('&lt;img')
        !rendered.contains('<img')
        rendered.contains('text')
        rendered.contains('more')
    }

    @Unroll
    def "isCommentOnly returns #expected for literal #literal"() {
        expect:
        MarkdownCodec.HtmlCommentStrippingRenderer.isCommentOnly(literal) == expected

        where:
        literal                                           | expected
        '<!-- a -->'                                       | true
        '  <!-- a -->  '                                   | true
        '<!-- a -->\n<!-- b -->'                            | true
        '<!-- a -- b -->'                                   | true
        ''                                                  | false
        '   '                                               | false
        '<!-->'                                             | false
        '<!-- a'                                            | false
        'x <!-- a -->'                                      | false
        '<!-- a --> x'                                      | false
        '<!-- a --> <img src=x> <!-- b -->'                 | false
        '<!-- <!-- --> -->'                                 | false
        '<!--->'                                            | false
        '<!-- a --!> <img> -->'                             | true
        '<!-- a --> <img>'                             | false
        '<div>'                                             | false
    }
}
