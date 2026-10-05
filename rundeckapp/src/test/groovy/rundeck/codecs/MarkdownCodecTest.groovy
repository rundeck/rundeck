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

import org.junit.Test
import org.rundeck.util.HtmlTableToMarkdownConverter

import static org.junit.Assert.*

/**
 * MarkdownCodecTest covers PS-1683: raw inline HTML in markdown must be
 * escaped rather than passed through as live markup, while normal
 * markdown syntax keeps rendering.
 */
class MarkdownCodecTest {
    @Test
    void testNormalMarkdownStillRenders(){
        assertEquals(
            '<article class="markdown-body"><p><strong>bold</strong></p>\n</article>',
            MarkdownCodec.decodeStr('**bold**')
        )
    }

    @Test
    void testRawHtmlInMarkdownIsEscaped(){
        assertEquals(
            '<article class="markdown-body"><p>some text</p>\n' +
            '<p>&lt;td style&#61;&#34;position:fixed;inset:0;background:url(&#39;//attacker/leak&#39;)&#34;&gt;x&lt;/td&gt;</p>\n</article>',
            MarkdownCodec.decodeStr(
                'some text\n\n<td style="position:fixed;inset:0;background:url(\'//attacker/leak\')">x</td>'
            )
        )
    }

    /**
     * A CSS-injection payload (a style attribute with position:fixed and a
     * background url()), placed inside an HTML table, must come out of the
     * full HtmlTableToMarkdownConverter -&gt; MarkdownCodec -&gt;
     * SanitizedHTMLCodec pipeline with no live "style" attribute and no
     * trace of the injected CSS/URL anywhere -- proving the table-to-markdown
     * conversion does not reintroduce that vector. This passes with zero
     * changes to MarkdownCodec.groovy or SanitizedHTMLCodec.groovy: the
     * style attribute is discarded by the converter itself, long before
     * either codec sees the text.
     */
    @Test
    void testCssInjectionPayloadInsideTableIsNeutralizedByConverter(){
        String payload = '<table><tr><td style="position:fixed;inset:0;width:100vw;height:100vh;' +
            'background:url(\'//attacker/leak\')">x</td></tr></table>'

        String converted = HtmlTableToMarkdownConverter.convert(payload)
        String rendered = MarkdownCodec.decodeStr(converted)

        // Pattern-based, case-insensitive, whitespace-tolerant checks --
        // substring checks here could miss variants like "STYLE=" or
        // "style =", or false-positive on the word "style" appearing
        // incidentally elsewhere.
        assertFalse('output must not contain a style attribute', (rendered =~ /(?i)\bstyle\s*=/).find())
        assertFalse('output must not contain the injected CSS', (rendered =~ /(?i)position\s*:\s*fixed/).find())
        assertFalse('output must not contain the injected url()', (rendered =~ /(?i)\burl\s*\(/).find())
        // A single-row table has no body rows left once that row becomes the
        // GFM header row (required by pipe-table syntax), so the safe cell
        // text is rendered inside a <th>, not a <td> -- either is a real,
        // live table cell, as opposed to escaped literal tag text.
        assertTrue(
            'the safe cell text must still render in a real table cell',
            rendered ==~ /(?s).*<t[hd]>x<\/t[hd]>.*/
        )
        assertTrue('converter must produce a real table, not raw HTML text', rendered.contains('<table>'))
    }
}
